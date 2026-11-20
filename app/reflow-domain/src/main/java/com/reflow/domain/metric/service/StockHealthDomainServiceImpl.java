package com.reflow.domain.metric.service;

import com.reflow.common.result.ResultDO;
import com.reflow.domain.metric.model.param.StockHealthParam;
import com.reflow.domain.metric.model.result.StyleHealthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 库存健康度计算实现。
 *
 * <p>这里刻意用 if/else 而不是策略模式：只有一套公式、三个分支，
 * 引入策略接口 + 工厂只会让"到底怎么算的"变得难找。
 * 领域层的目标是<b>可读</b>，不是可扩展——行业规则是可穷举的。</p>
 *
 * <p>评分权重（40/35/25）是经验值，真实项目应从配置表读，
 * 并按品类分别标定；这里硬编码是为了让示例自洽。</p>
 */
@Service
public class StockHealthDomainServiceImpl implements StockHealthDomainService {

    private static final Logger log = LoggerFactory.getLogger(StockHealthDomainServiceImpl.class);

    /** 日均销量为 0 时的 DOS 哨兵值：表示"卖不动"，比返回 Infinity 更安全 */
    private static final double DOS_NO_SALES = 999D;

    @Override
    public ResultDO<StyleHealthResult> calculate(StockHealthParam param) {
        try {
            if (param == null || param.getFirstOrderQty() == null || param.getFirstOrderQty() <= 0) {
                return ResultDO.buildFailResult("PARAM_ERROR", "首单量必须大于 0，否则动销率无意义");
            }

            int windowSales = param.getWindowSales() == null ? 0 : param.getWindowSales();
            int windowDays = param.getWindowDays() == null || param.getWindowDays() <= 0 ? 1 : param.getWindowDays();
            int stock = param.getTotalAvailableStock() == null ? 0 : param.getTotalAvailableStock();
            double brokenRate = param.getBrokenSizeRate() == null ? 0D : param.getBrokenSizeRate();

            // 动销率：窗口销量 / 首单量。可以超过 1（卖得比首单还多），不做上限截断——
            // 截断会掩盖"首单严重不足"这个最重要的信号
            double sellThroughRate = (double) windowSales / param.getFirstOrderQty();

            // DOS：按窗口内的日均销量折算还能卖几天
            double dailySales = (double) windowSales / windowDays;
            double dos = dailySales <= 0 ? DOS_NO_SALES : stock / dailySales;

            int score = scoreOf(sellThroughRate, dos, brokenRate);

            return ResultDO.buildSuccessResult(new StyleHealthResult(sellThroughRate, dos, brokenRate, score));

        } catch (Throwable e) {
            log.error("健康度计算异常, param={}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }

    /**
     * 综合评分：动销 40 + 可售天数 35 + 断码 25。
     *
     * <ul>
     *   <li>动销：达到 80% 视为满分，线性折算</li>
     *   <li>DOS：≤7 天满分（要断货），30 天以上 0 分（压货），中间线性</li>
     *   <li>断码：断码率越低越高分</li>
     * </ul>
     */
    private int scoreOf(double sellThroughRate, double dos, double brokenRate) {
        double sellScore = Math.min(1.0, sellThroughRate / 0.8) * 40;

        double dosScore;
        if (dos <= 7) {
            dosScore = 35;
        } else if (dos >= 30) {
            dosScore = 0;
        } else {
            dosScore = (30 - dos) / 23 * 35;
        }

        double brokenScore = Math.max(0, 1 - brokenRate) * 25;

        int total = (int) Math.round(sellScore + dosScore + brokenScore);
        return Math.max(0, Math.min(100, total));
    }
}