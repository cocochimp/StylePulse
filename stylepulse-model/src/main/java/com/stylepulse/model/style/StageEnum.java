package com.stylepulse.model.style;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 款生命周期阶段。
 *
 * <p>放在 model 而不是 domain.style，是因为它被三个上下文用到：
 * style（自己流转）、replenish（判断"这阶段还能不能追单"）、insight（按阶段做复盘分组）。
 * 这类跨上下文枚举属于"共享内核"，放 model 层；只被单个上下文使用的枚举就留在该上下文里。</p>
 *
 * <p>把流转规则写在枚举内部（而不是散在 Service 里）的好处：新增一个状态时，
 * 编译器和这张表会强制你回答"谁能进、谁能出"，不会漏掉某条分支。</p>
 */
public enum StageEnum {

    /** 建档：款号、颜色尺码、面辅料还没齐 */
    DRAFT("草稿"),

    /** 测款：首单已入仓上架，尚无有效销量 */
    TESTING("测款中"),

    /** 观察：首单开始出单，正在积累判断数据 */
    OBSERVING("观察期"),

    /** 返单中：已生成返单决策单，等待生产到仓 */
    REPLENISHING("返单中"),

    /** 放量：连续返单且售罄率稳定达标 */
    SCALING("放量期"),

    /** 清仓：不再返单，只做库存出清 */
    CLEARANCE("清仓"),

    /** 归档：生命周期结束 */
    ARCHIVED("已归档");

    private final String label;

    StageEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 静态流转表：key 是当前阶段，value 是允许迁入的阶段 */
    private static final Map<StageEnum, Set<StageEnum>> TRANSITIONS;

    static {
        Map<StageEnum, Set<StageEnum>> map = new EnumMap<>(StageEnum.class);
        map.put(DRAFT, EnumSet.of(TESTING));
        map.put(TESTING, EnumSet.of(OBSERVING, CLEARANCE));
        // 观察期是返单决策的主战场：追单进入 REPLENISHING，不追直接进清仓
        map.put(OBSERVING, EnumSet.of(REPLENISHING, CLEARANCE));
        map.put(REPLENISHING, EnumSet.of(SCALING, CLEARANCE));
        map.put(SCALING, EnumSet.of(CLEARANCE));
        map.put(CLEARANCE, EnumSet.of(ARCHIVED));
        map.put(ARCHIVED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * 判断能否流转到目标阶段。
     *
     * <p>注意这里是"允许的下一跳"而不是"可达"。跳级（如 DRAFT 直接到 SCALING）
     * 一律拒绝——业务上不存在这种路径，放行只会掩盖数据问题。</p>
     */
    public boolean canTransitTo(StageEnum target) {
        if (target == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    /** 该阶段是否允许开启返单决策 */
    public boolean canOpenDecision() {
        return this == OBSERVING || this == REPLENISHING || this == SCALING;
    }

    /** 该阶段是否属于"还在卖"的活跃期，诊断巡检只扫这些款 */
    public boolean isActive() {
        return this == TESTING || this == OBSERVING || this == REPLENISHING || this == SCALING;
    }
}