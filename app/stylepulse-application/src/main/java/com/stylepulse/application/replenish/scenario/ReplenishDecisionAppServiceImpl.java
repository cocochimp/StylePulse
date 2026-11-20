package com.stylepulse.application.replenish.scenario;

import com.stylepulse.application.replenish.assembler.FactoryOrderCommandAssembler;
import com.stylepulse.application.replenish.assembler.ReplenishAssembler;
import com.stylepulse.application.replenish.model.DiagnosisInternalDTO;
import com.stylepulse.application.replenish.port.FactoryOrderCommand;
import com.stylepulse.application.replenish.port.FactoryOrderPort;
import com.stylepulse.client.replenish.ReplenishDecisionAppService;
import com.stylepulse.client.replenish.req.ApproveDecisionRequestDTO;
import com.stylepulse.client.replenish.req.OpenDecisionRequestDTO;
import com.stylepulse.client.replenish.res.ApproveDecisionResponseDTO;
import com.stylepulse.client.replenish.res.OpenDecisionResponseDTO;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.capacity.model.param.ReserveCapacityParam;
import com.stylepulse.domain.capacity.model.result.ReserveCapacityResult;
import com.stylepulse.domain.capacity.service.CapacityDomainService;
import com.stylepulse.domain.replenish.model.param.ApproveDecisionParam;
import com.stylepulse.domain.replenish.model.param.DecisionItemParam;
import com.stylepulse.domain.replenish.model.param.OpenDecisionParam;
import com.stylepulse.domain.replenish.model.param.SizeAllocationParam;
import com.stylepulse.domain.replenish.model.param.SkuAllocItemParam;
import com.stylepulse.domain.replenish.model.result.ReplenishDecisionResult;
import com.stylepulse.domain.replenish.model.result.SizeAllocationResult;
import com.stylepulse.domain.replenish.repository.DispatchMessageRepository;
import com.stylepulse.domain.replenish.service.ReplenishDomainService;
import com.stylepulse.domain.replenish.service.SizeAllocationDomainService;
import com.stylepulse.domain.style.model.aggregate.StyleAggregate;
import com.stylepulse.domain.style.model.param.ChangeStageParam;
import com.stylepulse.domain.style.model.param.StyleQuery;
import com.stylepulse.domain.style.model.result.SkuSnapshotResult;
import com.stylepulse.domain.style.model.value.SizeCurveValue;
import com.stylepulse.domain.style.repository.StyleRepository;
import com.stylepulse.domain.style.service.StyleDomainService;
import com.stylepulse.model.common.BusinessDate;
import com.stylepulse.model.style.StageEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 返单决策应用服务（写模式）—— 全系统编排最复杂的一个场景。
 *
 * <p>它把四个上下文串成一条链，每一步都只做"调用 + 判断返回值"：</p>
 * <pre>
 * 开单：    查款 → 诊断(metric+rule) → 拆量(纯计算) → 落决策单(replenish) → 推进款阶段(style)
 * 审批：    审批(replenish) → 预占产能(capacity) → 写本地消息 → 下发工厂 → 确认产能 → 标记已下发
 * </pre>
 *
 * <p><b>为什么不把这些写进领域服务</b>：跨上下文编排是不稳定的（今天顺序是这样，
 * 明天可能要在下发前加一道风控），而领域规则是稳定的。把不稳定的部分放在应用层，
 * 领域层就不会因为流程调整而被反复修改。</p>
 */
@Service
public class ReplenishDecisionAppServiceImpl implements ReplenishDecisionAppService {

    private static final Logger log = LoggerFactory.getLogger(ReplenishDecisionAppServiceImpl.class);

    /** 默认排产日：留 7 天生产周期。真实系统应由产能路由策略计算 */
    private static final int DEFAULT_PRODUCTION_LEAD_DAYS = 7;

    /** 默认工厂与产线。真实系统按品类、历史质量、报价择优 */
    private static final Long DEFAULT_FACTORY_ID = 1L;
    private static final Long DEFAULT_LINE_ID = 1L;

    private final StyleRepository styleRepository;
    private final StyleDomainService styleDomainService;
    private final ReplenishDiagnosisSupport diagnosisSupport;
    private final SizeAllocationDomainService sizeAllocationDomainService;
    private final ReplenishDomainService replenishDomainService;
    private final CapacityDomainService capacityDomainService;
    private final DispatchMessageRepository dispatchMessageRepository;
    private final FactoryOrderPort factoryOrderPort;
    private final FactoryOrderCommandAssembler commandAssembler;

    public ReplenishDecisionAppServiceImpl(StyleRepository styleRepository,
                                           StyleDomainService styleDomainService,
                                           ReplenishDiagnosisSupport diagnosisSupport,
                                           SizeAllocationDomainService sizeAllocationDomainService,
                                           ReplenishDomainService replenishDomainService,
                                           CapacityDomainService capacityDomainService,
                                           DispatchMessageRepository dispatchMessageRepository,
                                           FactoryOrderPort factoryOrderPort,
                                           FactoryOrderCommandAssembler commandAssembler) {
        this.styleRepository = styleRepository;
        this.styleDomainService = styleDomainService;
        this.diagnosisSupport = diagnosisSupport;
        this.sizeAllocationDomainService = sizeAllocationDomainService;
        this.replenishDomainService = replenishDomainService;
        this.capacityDomainService = capacityDomainService;
        this.dispatchMessageRepository = dispatchMessageRepository;
        this.factoryOrderPort = factoryOrderPort;
        this.commandAssembler = commandAssembler;
    }

    // ------------------------------------------------------------------
    // 场景一：开单
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultDO<OpenDecisionResponseDTO> openDecision(OpenDecisionRequestDTO requestDTO) {
        ResultDO<Void> checkResult = requestDTO.check();
        if (!checkResult.isSuccess()) {
            return ResultDO.buildFailResult(checkResult.getCode(), checkResult.getMsg());
        }
        try {
            // 1. 加载款并做阶段校验（业务规则在聚合根里，这里只问"行不行"）
            ResultDO<StyleAggregate> styleResult = styleRepository.query(StyleQuery.byId(requestDTO.getStyleId()));
            if (!styleResult.isSuccess()) {
                return ResultDO.buildFailResult(styleResult.getCode(), styleResult.getMsg());
            }
            StyleAggregate style = styleResult.getData();
            if (style == null) {
                return ResultDO.buildFailResult("STYLE_NOT_FOUND", "款不存在");
            }
            if (!style.canOpenDecision()) {
                return ResultDO.buildFailResult("STAGE_NOT_ALLOWED",
                        "当前阶段[" + (style.getStage() == null ? "未知" : style.getStage().getLabel()) + "]不允许返单");
            }

            // 2. 诊断：算出该追多少
            ResultDO<DiagnosisInternalDTO> diagnosisResult = diagnosisSupport.diagnose(style);
            if (!diagnosisResult.isSuccess()) {
                return ResultDO.buildFailResult(diagnosisResult.getCode(), diagnosisResult.getMsg());
            }
            DiagnosisInternalDTO diagnosis = diagnosisResult.getData();
            if (!diagnosis.isMatched() || diagnosis.getQty() <= 0) {
                return ResultDO.buildFailResult("NO_REPLENISH_NEEDED",
                        diagnosis.getReason() == null ? "当前不需要返单" : diagnosis.getReason());
            }

            // 3. 拆量：款级总量 → 颜色×尺码
            SizeAllocationParam allocParam = buildAllocationParam(style, diagnosis);
            ResultDO<SizeAllocationResult> allocResult = sizeAllocationDomainService.allocate(allocParam);
            if (!allocResult.isSuccess()) {
                return ResultDO.buildFailResult(allocResult.getCode(), allocResult.getMsg());
            }
            SizeAllocationResult allocation = allocResult.getData();
            if (!allocation.isUsable()) {
                return ResultDO.buildFailResult("ALLOC_BELOW_MOQ",
                        "拆分后每个尺码都低于起订量，本次不追");
            }

            // 4. 落决策单（三层防重在这一步生效）
            OpenDecisionParam openParam = new OpenDecisionParam();
            openParam.setStyleId(style.getId());
            openParam.setRuleId(diagnosis.getRuleId());
            openParam.setRuleCode(diagnosis.getRuleCode());
            openParam.setRuleVersion(diagnosis.getRuleVersion());
            openParam.setTotalQty(allocation.getTotalAllocated());
            openParam.setTriggerSource(requestDTO.getTriggerSource());
            openParam.setShadow(Boolean.FALSE);
            openParam.setOperator(requestDTO.getOperator());

            List<DecisionItemParam> items = new ArrayList<>();
            for (SizeAllocationResult.Item item : allocation.getItems()) {
                items.add(new DecisionItemParam(item.getSkuId(), item.getColorCode(),
                        item.getSizeCode(), item.getQty()));
            }
            openParam.setItems(items);

            ResultDO<ReplenishDecisionResult> openResult = replenishDomainService.openDecision(openParam);
            if (!openResult.isSuccess()) {
                return ResultDO.buildFailResult(openResult.getCode(), openResult.getMsg());
            }
            ReplenishDecisionResult decision = openResult.getData();

            // 5. 新单才推进阶段。幂等返回和影子单都不该改款的状态——
            //    否则重复触发会把款反复推着走
            if (!decision.isDuplicated() && !decision.isShadow()) {
                ResultDO<Void> stageResult = styleDomainService.changeStage(
                        style.getId(),
                        new ChangeStageParam(StageEnum.REPLENISHING, "返单决策已生成:" + decision.getDecisionId()));
                if (!stageResult.isSuccess()) {
                    // 阶段推进失败不影响决策单本身：单子已经在了，
                    // 下次巡检会重新对齐状态，这里只记录告警
                    log.warn("返单决策已生成但阶段推进失败, styleId={}, decisionId={}, msg={}",
                            style.getId(), decision.getDecisionId(), stageResult.getMsg());
                }
            }

            return ResultDO.buildSuccessResult(ReplenishAssembler.toOpenDecisionDTO(decision));

        } catch (Throwable e) {
            log.error("开返单决策异常, styleId={}", requestDTO.getStyleId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }

    // ------------------------------------------------------------------
    // 场景二：审批 + 占产能 + 下发
    // ------------------------------------------------------------------

    @Override
    public ResultDO<ApproveDecisionResponseDTO> approveDecision(ApproveDecisionRequestDTO requestDTO) {
        ResultDO<Void> checkResult = requestDTO.check();
        if (!checkResult.isSuccess()) {
            return ResultDO.buildFailResult(checkResult.getCode(), checkResult.getMsg());
        }
        try {
            ApproveDecisionParam approveParam = new ApproveDecisionParam();
            approveParam.setDecisionId(requestDTO.getDecisionId());
            approveParam.setApproved(requestDTO.getApproved());
            approveParam.setRemark(requestDTO.getRemark());
            approveParam.setOperator(requestDTO.getOperator());

            ResultDO<ReplenishDecisionResult> approveResult = replenishDomainService.approveDecision(approveParam);
            if (!approveResult.isSuccess()) {
                return ResultDO.buildFailResult(approveResult.getCode(), approveResult.getMsg());
            }
            ReplenishDecisionResult decision = approveResult.getData();

            // 驳回是完整结果，不需要继续往下走
            if ("REJECTED".equals(decision.getStatus())) {
                return ResultDO.buildSuccessResult(ReplenishAssembler.toApproveDTO(decision, null, false));
            }

            // 1. 预占产能（T4）。失败不是异常，而是"卡在产能"，需要保留单子等待重试
            ResultDO<ReserveCapacityResult> reserveResult =
                    capacityDomainService.reserve(buildReserveParam(decision));
            if (!reserveResult.isSuccess()) {
                return ResultDO.buildFailResult(reserveResult.getCode(), reserveResult.getMsg());
            }
            ReserveCapacityResult capacity = reserveResult.getData();
            if (!capacity.isSuccess()) {
                replenishDomainService.markPendingCapacity(decision.getDecisionId());
                ResultDO<ReplenishDecisionResult> latest =
                        replenishDomainService.queryDecision(decision.getDecisionId());
                return ResultDO.buildSuccessResult(ReplenishAssembler.toApproveDTO(
                        latest.isSuccess() ? latest.getData() : decision, null, false));
            }

            // 2. 写本地消息（先落库再下发，保证"决定要发"这件事不会丢）
            FactoryOrderCommand command = commandAssembler.build(decision);
            String msgId = decision.getDecisionId() + "-" + System.currentTimeMillis();
            ResultDO<Void> saveMsgResult = dispatchMessageRepository.savePending(
                    msgId, decision.getDecisionId(), command.toPayload());
            if (!saveMsgResult.isSuccess()) {
                // 消息写不进去就等于发了也可能丢，直接释放产能，让决策单停在已批准状态可重试
                capacityDomainService.release(capacity.getHoldId());
                return ResultDO.buildFailResult(saveMsgResult.getCode(), saveMsgResult.getMsg());
            }

            // 3. 下发工厂
            ResultDO<Void> dispatchResult = factoryOrderPort.dispatch(command);
            if (!dispatchResult.isSuccess()) {
                // 下发失败：释放产能 + 标记消息待重试。绝不保留"占了产能但没下发"的状态
                capacityDomainService.release(capacity.getHoldId());
                dispatchMessageRepository.markFailed(msgId, 30);
                log.warn("生产指令下发失败，已释放产能待重试, decisionId={}, msg={}",
                        decision.getDecisionId(), dispatchResult.getMsg());
                return ResultDO.buildFailResult("DISPATCH_FAILED", "生产指令下发失败，已进入重试队列");
            }

            // 4. 成功：确认产能 + 标记已下发 + 消息置为已确认
            capacityDomainService.confirm(
                    buildConfirmParam(capacity.getHoldId()));
            replenishDomainService.markDispatched(decision.getDecisionId());
            dispatchMessageRepository.markSent(msgId);
            dispatchMessageRepository.markAck(msgId);

            ResultDO<ReplenishDecisionResult> latest =
                    replenishDomainService.queryDecision(decision.getDecisionId());
            return ResultDO.buildSuccessResult(ReplenishAssembler.toApproveDTO(
                    latest.isSuccess() ? latest.getData() : decision, capacity.getHoldId(), true));

        } catch (Throwable e) {
            log.error("审批决策异常, decisionId={}", requestDTO.getDecisionId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }

    // ------------------------------------------------------------------
    // 内部组装
    // ------------------------------------------------------------------

    private SizeAllocationParam buildAllocationParam(StyleAggregate style, DiagnosisInternalDTO diagnosis) {
        SizeAllocationParam param = new SizeAllocationParam();
        param.setStyleId(style.getId());
        param.setTotalQty(diagnosis.getQty());
        param.setMoq(diagnosis.getMoq());

        List<SkuAllocItemParam> skus = new ArrayList<>();
        for (SkuSnapshotResult snapshot : style.skuSnapshots()) {
            skus.add(new SkuAllocItemParam(snapshot.getSkuId(), snapshot.getColorCode(),
                    snapshot.getSizeCode(), snapshot.getAvailableStock(), snapshot.getRecentSales()));
        }
        param.setSkus(skus);

        // 尺码曲线用懒加载注入：拆量算法在循环里按尺码取权重时才真正查库，
        // 领域层因此不需要依赖 style 上下文
        param.setCurveProvider(this::loadCurve);
        return param;
    }

    private SizeCurveValue loadCurve(Long styleId) {
        ResultDO<StyleAggregate> result = styleRepository.query(StyleQuery.byId(styleId));
        return result.isSuccess() && result.getData() != null ? result.getData().getSizeCurve() : null;
    }

    private ReserveCapacityParam buildReserveParam(ReplenishDecisionResult decision) {
        LocalDate bizDate = LocalDate.now().plusDays(DEFAULT_PRODUCTION_LEAD_DAYS);
        ReserveCapacityParam param = new ReserveCapacityParam();
        param.setFactoryId(DEFAULT_FACTORY_ID);
        param.setLineId(DEFAULT_LINE_ID);
        param.setBizDate(BusinessDate.of(bizDate));
        param.setQty(decision.getTotalQty());
        param.setDecisionId(decision.getDecisionId());
        return param;
    }

    private com.stylepulse.domain.capacity.model.param.ConfirmCapacityParam buildConfirmParam(String holdId) {
        com.stylepulse.domain.capacity.model.param.ConfirmCapacityParam param =
                new com.stylepulse.domain.capacity.model.param.ConfirmCapacityParam();
        param.setHoldId(holdId);
        return param;
    }
}