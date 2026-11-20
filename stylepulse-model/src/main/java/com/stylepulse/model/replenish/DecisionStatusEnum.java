package com.stylepulse.model.replenish;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 返单决策单状态。
 *
 * <p>决策单是本系统里"最贵的一条记录"：它一旦进入 DISPATCHED，工厂就会真的备料生产。
 * 所以状态机要严格——非法流转不是"数据不好看"，而是会造成真实的库存损失。</p>
 *
 * <p>注意 PENDING_CAPACITY 这个中间态：产能预占失败不是直接失败，
 * 而是挂起等重试。如果直接置为失败，运营就得手工重建一张决策单，
 * 而重建会重算追单量，和原决策对不上。</p>
 */
public enum DecisionStatusEnum {

    /** 待审批：已算出追单量，等人或自动策略放行 */
    PENDING_REVIEW("待审批"),

    /** 已批准：可进入产能预占 */
    APPROVED("已批准"),

    /** 已驳回：数量不合理或时机不对 */
    REJECTED("已驳回"),

    /** 产能待定：预占失败，等待重试 */
    PENDING_CAPACITY("产能待定"),

    /** 已下发：生产指令已送达工厂 */
    DISPATCHED("已下发"),

    /** 生产中：工厂已接单 */
    IN_PRODUCTION("生产中"),

    /** 已到仓：货到了，可回填兑现率 */
    ARRIVED("已到仓"),

    /** 已关闭：兑现率复盘完成 */
    CLOSED("已关闭"),

    /** 已撤回：下发前主动取消 */
    CANCELLED("已撤回");

    private final String label;

    DecisionStatusEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final Map<DecisionStatusEnum, Set<DecisionStatusEnum>> TRANSITIONS;

    static {
        Map<DecisionStatusEnum, Set<DecisionStatusEnum>> map = new EnumMap<>(DecisionStatusEnum.class);
        map.put(PENDING_REVIEW, EnumSet.of(APPROVED, REJECTED, CANCELLED));
        map.put(APPROVED, EnumSet.of(PENDING_CAPACITY, DISPATCHED, CANCELLED));
        map.put(PENDING_CAPACITY, EnumSet.of(APPROVED, CANCELLED));
        map.put(DISPATCHED, EnumSet.of(IN_PRODUCTION));
        map.put(IN_PRODUCTION, EnumSet.of(ARRIVED));
        map.put(ARRIVED, EnumSet.of(CLOSED));
        // 以下三个是终态：驳回、关闭、撤回之后不再流转
        map.put(REJECTED, Collections.emptySet());
        map.put(CLOSED, Collections.emptySet());
        map.put(CANCELLED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    public boolean canTransitTo(DecisionStatusEnum target) {
        if (target == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    /** 是否终态：终态记录不允许再被任何流程改写 */
    public boolean isFinal() {
        return this == REJECTED || this == CLOSED || this == CANCELLED;
    }

    /** 是否属于"已占用产能"的状态，撤回/关闭时需要释放 */
    public boolean holdsCapacity() {
        return this == DISPATCHED || this == IN_PRODUCTION;
    }
}