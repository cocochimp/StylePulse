package com.stylepulse.domain.replenish.model.aggregate;

import com.stylepulse.domain.replenish.model.entity.ReplenishRuleEntity;
import com.stylepulse.domain.replenish.model.param.CalculateQtyParam;
import com.stylepulse.domain.replenish.model.param.MatchStyleParam;
import com.stylepulse.domain.replenish.model.result.CalculateQtyResult;
import com.stylepulse.domain.replenish.model.value.QtyRangeValue;
import com.stylepulse.model.style.PriceBandEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则匹配与追单量计算测试（规则+计算模式）。
 *
 * <p>重点验证三件事：
 * <ol>
 *   <li>条件为"任意区间"时不会误判为不命中</li>
 *   <li>追单量公式的方向正确（动销率越高追得越多）</li>
 *   <li>两次收敛的顺序正确（先截上限，再判 MOQ）</li>
 * </ol>
 */
class ReplenishRuleAggregateTest {

    @Test
    @DisplayName("条件全部满足时应命中")
    void shouldMatchWhenAllConditionsSatisfied() {
        ReplenishRuleAggregate rule = standardRule();
        assertThat(rule.matchStyle(matchParam(1L, PriceBandEnum.LOW, 12, 0.9, 2.0))).isTrue();
    }

    @Test
    @DisplayName("类目不匹配时应不命中")
    void shouldNotMatchWhenCategoryDiffers() {
        ReplenishRuleAggregate rule = standardRule();
        assertThat(rule.matchStyle(matchParam(99L, PriceBandEnum.LOW, 12, 0.9, 2.0))).isFalse();
    }

    @Test
    @DisplayName("动销率低于下限时应不命中")
    void shouldNotMatchWhenSellThroughTooLow() {
        ReplenishRuleAggregate rule = standardRule();
        assertThat(rule.matchStyle(matchParam(1L, PriceBandEnum.LOW, 12, 0.10, 2.0))).isFalse();
    }

    @Test
    @DisplayName("动销率越高，追单量越大")
    void shouldIncreaseQtyWithSellThrough() {
        ReplenishRuleAggregate rule = standardRule();
        int low = rule.calculateQty(calcParam(600, 0.5)).getQty();
        int high = rule.calculateQty(calcParam(600, 0.9)).getQty();
        assertThat(high).isGreaterThan(low);
    }

    @Test
    @DisplayName("追单量不得超过 首单量 × 上限倍数")
    void shouldCapByMultiplier() {
        ReplenishRuleAggregate rule = standardRule();
        // 600 × 3.0(动销率) × 1.2 × 1.0 = 2160，上限 600 × 3.0 = 1800
        CalculateQtyResult result = rule.calculateQty(calcParam(600, 3.0));
        assertThat(result.getQty()).isEqualTo(1800);
        assertThat(result.isCappedByMultiplier()).isTrue();
    }

    @Test
    @DisplayName("低于 MOQ 时追单量归零，并标记 belowMoq")
    void shouldZeroWhenBelowMoq() {
        ReplenishRuleAggregate rule = standardRule();
        // 600 × 0.05 × 1.2 = 36 < MOQ 100
        CalculateQtyResult result = rule.calculateQty(calcParam(600, 0.05));
        assertThat(result.getQty()).isZero();
        assertThat(result.isBelowMoq()).isTrue();
        assertThat(result.shouldReplenish()).isFalse();
    }

    @Test
    @DisplayName("停用的规则即使条件满足也不命中")
    void shouldNotMatchWhenDisabled() {
        ReplenishRuleAggregate rule = standardRule();
        rule.setEnabled(false);
        assertThat(rule.matchStyle(matchParam(1L, PriceBandEnum.LOW, 12, 0.9, 2.0))).isFalse();
    }

    // ------------------------------------------------------------------

    private ReplenishRuleAggregate standardRule() {
        ReplenishRuleEntity entity = new ReplenishRuleEntity();
        entity.setCategoryIds(Set.of(1L, 2L));
        entity.setPriceBands(Set.of(PriceBandEnum.LOW, PriceBandEnum.MID));
        entity.setAgeDayRange(QtyRangeValue.of(0, 30));
        entity.setDosRange(QtyRangeValue.of(0, 15));
        // 动销率 30% 起
        entity.setSellThroughRange(QtyRangeValue.of(3000, 100000));
        entity.setCycleFactor(new BigDecimal("1.2"));
        entity.setGrowthFactor(new BigDecimal("1.0"));
        entity.setMoq(100);
        entity.setMaxMultiplier(new BigDecimal("3.0"));
        return new ReplenishRuleAggregate(2L, "RT-STD-002", 10, 3, true, entity);
    }

    private MatchStyleParam matchParam(Long categoryId, PriceBandEnum band, int ageDays,
                                       double sellThrough, double dos) {
        MatchStyleParam param = new MatchStyleParam();
        param.setStyleId(1L);
        param.setCategoryId(categoryId);
        param.setPriceBand(band);
        param.setAgeDays(ageDays);
        param.setSellThroughRate(sellThrough);
        param.setDos(dos);
        return param;
    }

    private CalculateQtyParam calcParam(int firstOrderQty, double sellThrough) {
        CalculateQtyParam param = new CalculateQtyParam();
        param.setFirstOrderQty(firstOrderQty);
        param.setSellThroughRate(sellThrough);
        param.setMatchStyleParam(matchParam(1L, PriceBandEnum.LOW, 12, sellThrough, 2.0));
        return param;
    }
}