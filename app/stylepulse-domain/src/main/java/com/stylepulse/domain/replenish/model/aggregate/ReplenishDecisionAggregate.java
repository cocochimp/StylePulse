package com.stylepulse.domain.replenish.model.aggregate;

import com.stylepulse.common.base.BaseAggregate;
import com.stylepulse.common.exception.AggregateException;
import com.stylepulse.domain.replenish.model.entity.ReplenishDecisionItemEntity;
import com.stylepulse.domain.replenish.model.param.ApproveDecisionParam;
import com.stylepulse.domain.replenish.model.param.DecisionItemParam;
import com.stylepulse.domain.replenish.model.param.OpenDecisionParam;
import com.stylepulse.model.replenish.DecisionStatusEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 返单决策单聚合根（写模式）。
 *
 * <p>这是全系统"最贵"的聚合：它一旦流转到 DISPATCHED，工厂就开始备料生产。
 * 所以它承担三件事：
 * <ol>
 *   <li><b>状态机</b>：每步流转都要校验前置状态，非法流转直接抛异常</li>
 *   <li><b>不变量</b>：总量必须等于尺码行之和，否则工厂收到的数与审批看到的数不一致</li>
 *   <li><b>版本</b>：每次变更 version+1，仓储用它做乐观锁 CAS</li>
 * </ol>
 * </p>
 */
public class ReplenishDecisionAggregate extends BaseAggregate<Long> {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    /** 第几次返单；与 styleId 组成唯一键 uk_style_round，是三层防重的最后一道 */
    private Integer round;

    private Long ruleId;
    private String ruleCode;
    private Integer ruleVersion;

    private Integer totalQty;

    /** 影子决策：只落库、不下发，用于新旧规则对比 */
    private Boolean shadow;

    private String triggerSource;

    private DecisionStatusEnum status;

    /** 乐观锁版本号 */
    private Integer version;

    private List<ReplenishDecisionItemEntity> items = new ArrayList<>();

    /**
     * 由入参生成一张待审批的决策单。
     *
     * <p>用静态工厂而不是构造器：决策单的创建有明确的前置条件（必须有量、必须有尺码行），
     * 放进工厂方法可以在对象出生那一刻就把非法数据挡掉，不留"半个非法对象"。</p>
     */
    public static ReplenishDecisionAggregate open(OpenDecisionParam param) {
        if (param == null || param.getStyleId() == null) {
            throw new AggregateException("PARAM_ERROR", "styleId 不能为空");
        }
        if (param.getRound() == null || param.getRound() <= 0) {
            throw new AggregateException("PARAM_ERROR", "返单轮次必须大于 0");
        }
        if (param.getTotalQty() == null || param.getTotalQty() <= 0) {
            throw new AggregateException("PARAM_ERROR", "追单量必须大于 0，不该为 0 的款开单");
        }

        ReplenishDecisionAggregate decision = new ReplenishDecisionAggregate();
        decision.styleId = param.getStyleId();
        decision.round = param.getRound();
        decision.ruleId = param.getRuleId();
        decision.ruleCode = param.getRuleCode();
        decision.ruleVersion = param.getRuleVersion();
        decision.totalQty = param.getTotalQty();
        decision.triggerSource = param.getTriggerSource();
        decision.shadow = param.getShadow() == null ? Boolean.FALSE : param.getShadow();
        decision.status = DecisionStatusEnum.PENDING_REVIEW;
        decision.version = 0;

        if (param.getItems() != null) {
            for (DecisionItemParam item : param.getItems()) {
                decision.items.add(new ReplenishDecisionItemEntity(
                        item.getSkuId(), item.getColorCode(), item.getSizeCode(), item.getQty()));
            }
        }
        decision.validateBalance();
        return decision;
    }

    // ------------------------------------------------------------------
    // 不变量
    // ------------------------------------------------------------------

    /**
     * 校验总量守恒。
     *
     * <p>如果允许不守恒，下游会同时存在两个"真实数量"：审批看到的 totalQty、
     * 工厂按 items 生产出来的实际数量。两者一旦分叉，库存账就再也对不上了。</p>
     */
    public void validateBalance() {
        int sum = items.stream().mapToInt(ReplenishDecisionItemEntity::qtyValue).sum();
        if (sum != (totalQty == null ? 0 : totalQty)) {
            throw new AggregateException("QTY_NOT_BALANCED",
                    "尺码行合计 " + sum + " 与总量 " + totalQty + " 不一致");
        }
    }

    // ------------------------------------------------------------------
    // 状态流转
    // ------------------------------------------------------------------

    public void approve(ApproveDecisionParam param) {
        if (param == null || param.getApproved() == null) {
            throw new AggregateException("PARAM_ERROR", "审批结论不能为空");
        }
        transitTo(Boolean.TRUE.equals(param.getApproved())
                ? DecisionStatusEnum.APPROVED
                : DecisionStatusEnum.REJECTED);
    }

    public void markPendingCapacity() {
        transitTo(DecisionStatusEnum.PENDING_CAPACITY);
    }

    public void backToApproved() {
        transitTo(DecisionStatusEnum.APPROVED);
    }

    public void markDispatched() {
        if (Boolean.TRUE.equals(shadow)) {
            throw new AggregateException("SHADOW_NOT_DISPATCHABLE", "影子决策不允许下发");
        }
        transitTo(DecisionStatusEnum.DISPATCHED);
    }

    public void markInProduction() {
        transitTo(DecisionStatusEnum.IN_PRODUCTION);
    }

    public void markArrived() {
        transitTo(DecisionStatusEnum.ARRIVED);
    }

    public void close() {
        transitTo(DecisionStatusEnum.CLOSED);
    }

    public void cancel() {
        transitTo(DecisionStatusEnum.CANCELLED);
    }

    /** 唯一的流转入口：集中校验 + 递增版本，避免某条路径漏掉 version+1 */
    private void transitTo(DecisionStatusEnum target) {
        if (this.status == null) {
            throw new AggregateException("STATUS_NULL", "决策单状态未初始化");
        }
        if (!this.status.canTransitTo(target)) {
            throw new AggregateException("STATUS_TRANSIT_DENIED",
                    "不允许从 " + this.status.getLabel() + " 流转到 " + target.getLabel());
        }
        this.status = target;
        this.version = this.version == null ? 1 : this.version + 1;
    }

    // ------------------------------------------------------------------
    // 判断类
    // ------------------------------------------------------------------

    public boolean isShadow() {
        return Boolean.TRUE.equals(shadow);
    }

    public boolean canApprove() {
        return status == DecisionStatusEnum.PENDING_REVIEW;
    }

    // ------------------------------------------------------------------
    // 访问器
    // ------------------------------------------------------------------

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public Boolean getShadow() {
        return shadow;
    }

    public void setShadow(Boolean shadow) {
        this.shadow = shadow;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public DecisionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(DecisionStatusEnum status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<ReplenishDecisionItemEntity> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** 仅供 Converter 重建聚合使用 */
    public void restoreItems(List<ReplenishDecisionItemEntity> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}