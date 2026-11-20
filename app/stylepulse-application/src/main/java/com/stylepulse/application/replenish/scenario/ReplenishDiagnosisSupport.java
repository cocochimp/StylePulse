package com.stylepulse.application.replenish.scenario;

import com.stylepulse.application.replenish.model.DiagnosisInternalDTO;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.metric.model.param.StockHealthParam;
import com.stylepulse.domain.metric.model.result.StyleHealthResult;
import com.stylepulse.domain.metric.service.SalesWatermarkDomainService;
import com.stylepulse.domain.metric.service.StockHealthDomainService;
import com.stylepulse.domain.replenish.model.param.CalculateQtyParam;
import com.stylepulse.domain.replenish.model.param.MatchStyleParam;
import com.stylepulse.domain.replenish.model.result.CalculateQtyResult;
import com.stylepulse.domain.replenish.service.ReplenishCalculateDomainService;
import com.stylepulse.domain.style.model.aggregate.StyleAggregate;
import com.stylepulse.model.metric.WindowSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 诊断编排器（应用层的复用单元）。
 *
 * <p>它把"跨上下文的取数与计算"编排成一步：
 * <pre>
 * 查销量窗口(metric) → 算健康度(纯计算) → 组装匹配条件 → 命中规则并算追单量(规则+计算)
 * </pre>
 *
 * <p>为什么单独抽出来而不是写在一个 AppService 里：
 * "诊断"这个动作被两个场景共用——只诊断不开单（运营看板）、诊断后开单（决策链）。
 * 复制一份就会出现两套口径，这个坑在真实项目里非常常见。</p>
 *
 * <p>注意这里出现了跨上下文的仓储调用（SalesWatermarkRepository 的领域服务），
 * 这是应用层的合法职责：<b>应用层负责编排</b>。
 * 领域层之间则不允许互相调用，边界由架构约束保证。</p>
 */
@Component
public class ReplenishDiagnosisSupport {

    private static final Logger log = LoggerFactory.getLogger(ReplenishDiagnosisSupport.class);

    /** 动销率取几天窗口。放常量便于演示，真实项目应走配置中心 */
    private static final int WINDOW_DAYS = WindowSpec.DEFAULT_WINDOW_DAYS;

    private final SalesWatermarkDomainService salesWatermarkDomainService;
    private final StockHealthDomainService stockHealthDomainService;
    private final ReplenishCalculateDomainService replenishCalculateDomainService;

    public ReplenishDiagnosisSupport(SalesWatermarkDomainService salesWatermarkDomainService,
                                     StockHealthDomainService stockHealthDomainService,
                                     ReplenishCalculateDomainService replenishCalculateDomainService) {
        this.salesWatermarkDomainService = salesWatermarkDomainService;
        this.stockHealthDomainService = stockHealthDomainService;
        this.replenishCalculateDomainService = replenishCalculateDomainService;
    }

    /**
     * 诊断一个款。
     *
     * @return 诊断结果；规则未命中时仍然返回成功，只是 matched=false、qty=0——
     * "不该追单"是正常结论，不是错误
     */
    public ResultDO<DiagnosisInternalDTO> diagnose(StyleAggregate style) {
        // 1. 取窗口销量（T2 的读取侧）
        ResultDO<Integer> windowSalesResult = salesWatermarkDomainService.queryWindowSales(
                style.getId(), WINDOW_DAYS, System.currentTimeMillis());
        if (!windowSalesResult.isSuccess()) {
            return ResultDO.buildFailResult(windowSalesResult.getCode(), windowSalesResult.getMsg());
        }
        int windowSales = windowSalesResult.getData() == null ? 0 : windowSalesResult.getData();

        // 2. 算健康度（纯计算，无副作用）
        StockHealthParam healthParam = new StockHealthParam();
        healthParam.setFirstOrderQty(style.getFirstOrderQty());
        healthParam.setWindowSales(windowSales);
        healthParam.setWindowDays(WINDOW_DAYS);
        healthParam.setTotalAvailableStock(style.totalAvailableStock());
        healthParam.setBrokenSizeRate(style.brokenSizeRate());

        ResultDO<StyleHealthResult> healthResult = stockHealthDomainService.calculate(healthParam);
        if (!healthResult.isSuccess()) {
            return ResultDO.buildFailResult(healthResult.getCode(), healthResult.getMsg());
        }
        StyleHealthResult health = healthResult.getData();

        DiagnosisInternalDTO dto = new DiagnosisInternalDTO();
        dto.setStage(style.getStage() == null ? null : style.getStage().name());
        dto.setSellThroughRate(health.getSellThroughRate());
        dto.setDos(health.getDos());
        dto.setBrokenSizeRate(health.getBrokenSizeRate());
        dto.setHealthScore(health.getHealthScore());

        // 3. 组装匹配条件 —— 这是"款的事实"与"规则的维度"之间的桥
        MatchStyleParam matchParam = new MatchStyleParam();
        matchParam.setStyleId(style.getId());
        matchParam.setCategoryId(style.getCategoryId());
        matchParam.setPriceBand(style.getPriceBand());
        matchParam.setAgeDays(style.ageDays(LocalDate.now()));
        matchParam.setDos(health.getDos());
        matchParam.setSellThroughRate(health.getSellThroughRate());

        // 4. 命中规则并计算追单量（规则+计算模式）
        CalculateQtyParam calcParam = new CalculateQtyParam();
        calcParam.setFirstOrderQty(style.getFirstOrderQty());
        calcParam.setSellThroughRate(health.getSellThroughRate());
        calcParam.setMatchStyleParam(matchParam);

        ResultDO<CalculateQtyResult> calcResult = replenishCalculateDomainService.calculateQty(calcParam);
        if (!calcResult.isSuccess()) {
            // 未命中规则是可解释的结果，不要让上层当异常处理
            dto.setMatched(false);
            dto.setQty(0);
            dto.setReason(calcResult.getMsg());
            log.debug("款未命中规则, styleId={}, reason={}", style.getId(), calcResult.getMsg());
            return ResultDO.buildSuccessResult(dto);
        }

        CalculateQtyResult calc = calcResult.getData();
        dto.setMatched(true);
        dto.setRuleId(calc.getRuleId());
        dto.setRuleCode(calc.getRuleCode());
        dto.setRuleVersion(calc.getRuleVersion());
        dto.setQty(calc.getQty());
        dto.setBelowMoq(calc.isBelowMoq());
        dto.setMoq(calc.getMoq());
        dto.setFormulaTrace(calc.getFormulaTrace());
        if (!calc.shouldReplenish()) {
            dto.setReason(calc.isBelowMoq() ? "追单量低于 MOQ" : "追单量为 0");
        }
        return ResultDO.buildSuccessResult(dto);
    }
}