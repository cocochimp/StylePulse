package com.reflow.domain.replenish.model.entity;

import com.reflow.common.base.BaseEntity;
import com.reflow.common.exception.AggregateException;
import com.reflow.domain.replenish.model.param.CalculateQtyParam;
import com.reflow.domain.replenish.model.param.MatchStyleParam;
import com.reflow.domain.replenish.model.result.CalculateQtyResult;
import com.reflow.domain.replenish.model.value.QtyRangeValue;
import com.reflow.model.style.PriceBandEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Set;

/**
 * 返单规则实体：规则的<b>条件</b>与<b>参数</b>都在这里。
 *
 * <p>为什么规则要做成"实体 + 聚合根"而不是一行配置：
 * 规则是会被改的（口径按季度调整），改规则等于改钱。
 * 做成聚合根后，规则有了版本号、有了优先级、有了启用开关，
 * 决策单又能引用"当时用的是哪一版"，复盘才成立。</p>
 *
 * <p>两个方法都无副作用（规则+计算模式的核心约束）：
 * {@code matchStyle} 只判断，{@code calculateQty} 只计算，绝不改自身状态。
 * 这样同一条规则可以被上千个款并发匹配，不需要任何锁。</p>
 */
public class ReplenishRuleEntity extends BaseEntity<Long> {

    private static final long serialVersionUID = 1L;

    /** 适用类目；空集合表示不限类目 */
    private Set<Long> categoryIds;

    /** 适用价格带；空集合表示不限 */
    private Set<PriceBandEnum> priceBands;

    /** 上架天数条件 */
    private QtyRangeValue ageDayRange;

    /** 可售天数条件（整数天） */
    private QtyRangeValue dosRange;

    /** 动销率条件，单位"万分比"：8300 表示 83% */
    private QtyRangeValue sellThroughRange;

    /** 周期系数：把"当前窗口的动销"折算成一个补货周期 */
    private BigDecimal cycleFactor;

    /** 增长系数：对趋势做微调，>1 表示预期继续涨 */
    private BigDecimal growthFactor;

    /** 最小起订量：低于它就不值得开单，工厂也不会接 */
    private int moq;

    /** 追单上限倍数：追单量不得超过 首单量 × 该倍数，防止一次追爆 */
    private BigDecimal maxMultiplier;

    // ------------------------------------------------------------------
    // 规则+计算模式：只读，无副作用
    // ------------------------------------------------------------------

    /**
     * 判断款是否命中本规则。
     *
     * <p>顺序按"过滤能力从强到弱"排：类目和价格带能一次性筛掉大部分款，
     * 区间判断放最后。这是很朴素但很有效的优化——精确匹配比区间比较便宜。</p>
     */
    public boolean matchStyle(MatchStyleParam param) {
        if (param == null) {
            return false;
        }
        if (categoryIds != null && !categoryIds.isEmpty()
                && (param.getCategoryId() == null || !categoryIds.contains(param.getCategoryId()))) {
            return false;
        }
        if (priceBands != null && !priceBands.isEmpty()
                && (param.getPriceBand() == null || !priceBands.contains(param.getPriceBand()))) {
            return false;
        }
        int ageDays = param.getAgeDays() == null ? 0 : param.getAgeDays();
        if (ageDayRange != null && !ageDayRange.contains(ageDays)) {
            return false;
        }
        int dos = param.getDos() == null ? 0 : (int) Math.round(param.getDos());
        if (dosRange != null && !dosRange.contains(dos)) {
            return false;
        }
        int sellThroughBp = param.getSellThroughRate() == null
                ? 0
                : (int) Math.round(param.getSellThroughRate() * 10000);
        if (sellThroughRange != null && !sellThroughRange.contains(sellThroughBp)) {
            return false;
        }
        return true;
    }

    /**
     * 计算追单量。
     *
     * <p>公式：{@code 首单量 × 动销率 × 周期系数 × 增长系数}，再依次做两次收敛：
     * <ol>
     *   <li>上限收敛：不超过 {@code 首单量 × 追单上限倍数}</li>
     *   <li>MOQ 收敛：低于最小起订量则归零</li>
     * </ol>
     * 顺序不能反：先截上限再判 MOQ，否则会出现"本来够 MOQ、被截断后反而变小"的怪结果。</p>
     *
     * <p>全程 {@link BigDecimal}：浮点乘法在千级数量上误差不明显，但一旦上了上限收敛，
     * 0.1 的误差就可能让结果在 199 和 200 之间跳，而 200 正好是 MOQ 边界。</p>
     */
    public CalculateQtyResult calculateQty(CalculateQtyParam param) {
        if (param == null || param.getFirstOrderQty() == null || param.getFirstOrderQty() <= 0) {
            throw new AggregateException("PARAM_ERROR", "首单量必须大于 0");
        }
        if (cycleFactor == null || growthFactor == null || maxMultiplier == null) {
            throw new AggregateException("RULE_INCOMPLETE", "规则参数不完整，无法计算追单量");
        }

        int firstOrderQty = param.getFirstOrderQty();
        BigDecimal sellThrough = BigDecimal.valueOf(
                param.getSellThroughRate() == null ? 0D : param.getSellThroughRate());

        BigDecimal raw = BigDecimal.valueOf(firstOrderQty)
                .multiply(sellThrough)
                .multiply(cycleFactor)
                .multiply(growthFactor);
        int qty = raw.setScale(0, RoundingMode.HALF_UP).intValue();

        int cap = BigDecimal.valueOf(firstOrderQty)
                .multiply(maxMultiplier)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        boolean capped = qty > cap;
        if (capped) {
            qty = cap;
        }

        boolean belowMoq = qty < moq;
        if (belowMoq) {
            qty = 0;
        }

        CalculateQtyResult result = new CalculateQtyResult(qty);
        result.setBelowMoq(belowMoq);
        result.setMoq(moq);
        result.setCappedByMultiplier(capped);
        result.setFormulaTrace(String.format(
                "%d×%s×%s×%s=%d, cap=%d, moq=%d",
                firstOrderQty, show(sellThrough), show(cycleFactor), show(growthFactor),
                raw.setScale(0, RoundingMode.HALF_UP).intValue(), cap, moq));
        return result;
    }

    private String show(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    // ------------------------------------------------------------------
    // 访问器
    // ------------------------------------------------------------------

    public Set<Long> getCategoryIds() {
        return categoryIds == null ? Collections.emptySet() : categoryIds;
    }

    public void setCategoryIds(Set<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public Set<PriceBandEnum> getPriceBands() {
        return priceBands == null ? Collections.emptySet() : priceBands;
    }

    public void setPriceBands(Set<PriceBandEnum> priceBands) {
        this.priceBands = priceBands;
    }

    public QtyRangeValue getAgeDayRange() {
        return ageDayRange;
    }

    public void setAgeDayRange(QtyRangeValue ageDayRange) {
        this.ageDayRange = ageDayRange;
    }

    public QtyRangeValue getDosRange() {
        return dosRange;
    }

    public void setDosRange(QtyRangeValue dosRange) {
        this.dosRange = dosRange;
    }

    public QtyRangeValue getSellThroughRange() {
        return sellThroughRange;
    }

    public void setSellThroughRange(QtyRangeValue sellThroughRange) {
        this.sellThroughRange = sellThroughRange;
    }

    public BigDecimal getCycleFactor() {
        return cycleFactor;
    }

    public void setCycleFactor(BigDecimal cycleFactor) {
        this.cycleFactor = cycleFactor;
    }

    public BigDecimal getGrowthFactor() {
        return growthFactor;
    }

    public void setGrowthFactor(BigDecimal growthFactor) {
        this.growthFactor = growthFactor;
    }

    public int getMoq() {
        return moq;
    }

    public void setMoq(int moq) {
        this.moq = moq;
    }

    public BigDecimal getMaxMultiplier() {
        return maxMultiplier;
    }

    public void setMaxMultiplier(BigDecimal maxMultiplier) {
        this.maxMultiplier = maxMultiplier;
    }
}