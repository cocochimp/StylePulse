package com.reflow.infrastructure.replenish.converter;

import com.reflow.domain.replenish.model.aggregate.ReplenishRuleAggregate;
import com.reflow.domain.replenish.model.entity.ReplenishRuleEntity;
import com.reflow.domain.replenish.model.value.QtyRangeValue;
import com.reflow.infrastructure.replenish.mysql.po.ReplenishRulePO;
import com.reflow.model.style.PriceBandEnum;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 规则 PO ↔ 聚合转换。
 *
 * <p>重点看 CSV 列（categoryIds / priceBands）的解析：数据库里为了可读性存成字符串，
 * 领域模型里是 Set。这种"存储表示 vs 领域表示"的差异，正是 Converter 存在的理由。</p>
 */
public final class ReplenishRuleConverter {

    private ReplenishRuleConverter() {
    }

    public static ReplenishRuleAggregate toAggregate(ReplenishRulePO po) {
        if (po == null) {
            return null;
        }
        ReplenishRuleEntity entity = new ReplenishRuleEntity();
        entity.setId(po.getId());
        entity.setCategoryIds(parseLongSet(po.getCategoryIds()));
        entity.setPriceBands(parsePriceBands(po.getPriceBands()));
        entity.setAgeDayRange(QtyRangeValue.of(
                po.getAgeDayMin() == null ? Integer.MIN_VALUE : po.getAgeDayMin(),
                po.getAgeDayMax() == null ? Integer.MAX_VALUE : po.getAgeDayMax()));
        entity.setDosRange(QtyRangeValue.of(
                po.getDosMin() == null ? Integer.MIN_VALUE : po.getDosMin(),
                po.getDosMax() == null ? Integer.MAX_VALUE : po.getDosMax()));
        entity.setSellThroughRange(QtyRangeValue.of(
                po.getSellThroughMin() == null ? Integer.MIN_VALUE : po.getSellThroughMin(),
                po.getSellThroughMax() == null ? Integer.MAX_VALUE : po.getSellThroughMax()));
        entity.setCycleFactor(po.getCycleFactor());
        entity.setGrowthFactor(po.getGrowthFactor());
        entity.setMoq(po.getMoq() == null ? 0 : po.getMoq());
        entity.setMaxMultiplier(po.getMaxMultiplier());

        return new ReplenishRuleAggregate(
                po.getId(), po.getRuleCode(), po.getPriority(), po.getRuleVersion(),
                po.getEnabled() != null && po.getEnabled() == 1, entity);
    }

    private static Set<Long> parseLongSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<PriceBandEnum> parsePriceBands(String csv) {
        if (csv == null || csv.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(code -> PriceBandEnum.ofCode(Integer.parseInt(code)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}