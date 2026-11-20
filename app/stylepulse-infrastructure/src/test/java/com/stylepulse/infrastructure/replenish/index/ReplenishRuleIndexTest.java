package com.stylepulse.infrastructure.replenish.index;

import com.stylepulse.domain.replenish.model.aggregate.ReplenishRuleAggregate;
import com.stylepulse.domain.replenish.model.entity.ReplenishRuleEntity;
import com.stylepulse.domain.replenish.model.param.MatchStyleParam;
import com.stylepulse.domain.replenish.model.value.QtyRangeValue;
import com.stylepulse.model.style.PriceBandEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则倒排索引测试（T1）。
 *
 * <p>要证明两件事：
 * <ol>
 *   <li>索引是<b>不会漏</b>的：所有真正命中的规则都必须出现在候选集里</li>
 *   <li>索引是<b>会多</b>的：桶化会带来误召回，所以必须再做精确判断（这是设计的一部分，不是 bug）</li>
 * </ol>
 * 前者是正确性红线，后者是性能取舍。</p>
 */
class ReplenishRuleIndexTest {

    @Test
    @DisplayName("类目与价格带精确命中：候选集只含该维度适用的规则")
    void shouldFilterByCategoryAndPriceBand() {
        ReplenishRuleIndex index = ReplenishRuleIndex.build(List.of(
                rule(1L, "CAT-1-ONLY", Set.of(1L), 0.8),
                rule(2L, "CAT-2-ONLY", Set.of(2L), 0.8)));

        MatchStyleParam param = param(1L, PriceBandEnum.LOW, 5, 0.9, 2.0);
        List<String> codes = index.candidates(param).stream()
                .map(ReplenishRuleAggregate::getRuleCode).collect(Collectors.toList());

        assertThat(codes).contains("CAT-1-ONLY").doesNotContain("CAT-2-ONLY");
    }

    @Test
    @DisplayName("不限类目的规则对任何类目都应召回（哨兵并集）")
    void shouldIncludeUnrestrictedCategoryRules() {
        ReplenishRuleIndex index = ReplenishRuleIndex.build(List.of(
                rule(1L, "ANY-CATEGORY", Set.of(), 0.8)));

        List<String> codes = index.candidates(param(123L, PriceBandEnum.HIGH, 5, 0.9, 2.0))
                .stream().map(ReplenishRuleAggregate::getRuleCode).collect(Collectors.toList());

        assertThat(codes).contains("ANY-CATEGORY");
    }

    @Test
    @DisplayName("区间条件不该漏：落在区间内的款必须召回")
    void shouldNotMissRulesWithinRange() {
        // 上架天数限定 0~7
        ReplenishRuleIndex index = ReplenishRuleIndex.build(List.of(
                rule(1L, "AGE-0-7", Set.of(), 0.8)));

        List<ReplenishRuleAggregate> candidates = index.candidates(param(1L, PriceBandEnum.LOW, 5, 0.9, 2.0));
        assertThat(candidates).extracting(ReplenishRuleAggregate::getRuleCode).contains("AGE-0-7");
    }

    @Test
    @DisplayName("桶化会多召回：区间外的款也可能进候选集，必须靠精算剔除")
    void shouldOverFetchThenRefine() {
        // 规则上架天数 0~3，落在第 0 个桶 [0-3]；款上架 4 天落在第 1 个桶 [4-7]，
        // 桶不相交，所以连候选集都进不去 —— 这一维上是精确的
        ReplenishRuleAggregate narrowRule = rule(1L, "AGE-0-3", Set.of(), 0.8);
        narrowRule.getRuleEntity().setAgeDayRange(QtyRangeValue.of(0, 3));
        ReplenishRuleIndex index = ReplenishRuleIndex.build(List.of(narrowRule));

        assertThat(index.candidates(param(1L, PriceBandEnum.LOW, 4, 0.9, 2.0))).isEmpty();

        // 换成同桶内的值，会被召回，但是否真正命中由 matchStyle 决定
        List<ReplenishRuleAggregate> candidates = index.candidates(param(1L, PriceBandEnum.LOW, 1, 0.9, 2.0));
        assertThat(candidates).hasSize(1);
    }

    @Test
    @DisplayName("停用规则不进入索引")
    void shouldSkipDisabledRule() {
        ReplenishRuleAggregate disabled = rule(1L, "DISABLED", Set.of(), 0.8);
        disabled.setEnabled(false);

        ReplenishRuleIndex index = ReplenishRuleIndex.build(List.of(disabled));

        assertThat(index.size()).isZero();
        assertThat(index.candidates(param(1L, PriceBandEnum.LOW, 5, 0.9, 2.0))).isEmpty();
    }

    // ------------------------------------------------------------------

    private ReplenishRuleAggregate rule(Long id, String code, Set<Long> categoryIds, double maxMultiplier) {
        ReplenishRuleEntity entity = new ReplenishRuleEntity();
        entity.setCategoryIds(categoryIds);
        entity.setPriceBands(Set.of());   // 不限价格带
        entity.setAgeDayRange(QtyRangeValue.of(0, 7));
        entity.setDosRange(QtyRangeValue.of(0, 5));
        entity.setSellThroughRange(QtyRangeValue.of(6000, 100000));
        entity.setCycleFactor(new BigDecimal("1.5"));
        entity.setGrowthFactor(new BigDecimal("1.1"));
        entity.setMoq(100);
        entity.setMaxMultiplier(BigDecimal.valueOf(maxMultiplier));
        return new ReplenishRuleAggregate(id, code, 10, 1, true, entity);
    }

    private MatchStyleParam param(Long categoryId, PriceBandEnum band, int ageDays,
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
}