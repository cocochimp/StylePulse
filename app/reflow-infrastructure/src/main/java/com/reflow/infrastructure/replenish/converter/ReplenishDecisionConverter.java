package com.reflow.infrastructure.replenish.converter;

import com.reflow.domain.replenish.model.aggregate.ReplenishDecisionAggregate;
import com.reflow.domain.replenish.model.entity.ReplenishDecisionItemEntity;
import com.reflow.infrastructure.replenish.mysql.po.ReplenishDecisionItemPO;
import com.reflow.infrastructure.replenish.mysql.po.ReplenishDecisionPO;
import com.reflow.model.replenish.DecisionStatusEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * 决策单 PO ↔ 聚合转换。
 */
public final class ReplenishDecisionConverter {

    private ReplenishDecisionConverter() {
    }

    public static ReplenishDecisionAggregate toAggregate(ReplenishDecisionPO po, List<ReplenishDecisionItemPO> itemPOs) {
        if (po == null) {
            return null;
        }
        ReplenishDecisionAggregate aggregate = new ReplenishDecisionAggregate();
        aggregate.setId(po.getId());
        aggregate.setStyleId(po.getStyleId());
        aggregate.setRound(po.getRound());
        aggregate.setRuleId(po.getRuleId());
        aggregate.setRuleCode(po.getRuleCode());
        aggregate.setRuleVersion(po.getRuleVersion());
        aggregate.setTotalQty(po.getTotalQty());
        aggregate.setShadow(po.getShadow() != null && po.getShadow() == 1);
        aggregate.setTriggerSource(po.getTriggerSource());
        aggregate.setStatus(po.getStatus() == null ? null : DecisionStatusEnum.valueOf(po.getStatus()));
        aggregate.setVersion(po.getVersion());

        List<ReplenishDecisionItemEntity> items = new ArrayList<>();
        if (itemPOs != null) {
            for (ReplenishDecisionItemPO itemPO : itemPOs) {
                items.add(new ReplenishDecisionItemEntity(
                        itemPO.getSkuId(), itemPO.getColorCode(), itemPO.getSizeCode(), itemPO.getQty()));
                // 实体主键在业务上不关心，但保留下来便于更新
                items.get(items.size() - 1).setId(itemPO.getId());
            }
        }
        aggregate.restoreItems(items);
        return aggregate;
    }

    public static ReplenishDecisionPO toPO(ReplenishDecisionAggregate aggregate) {
        ReplenishDecisionPO po = new ReplenishDecisionPO();
        po.setId(aggregate.getId());
        po.setStyleId(aggregate.getStyleId());
        po.setRound(aggregate.getRound());
        po.setRuleId(aggregate.getRuleId());
        po.setRuleCode(aggregate.getRuleCode());
        po.setRuleVersion(aggregate.getRuleVersion());
        po.setTotalQty(aggregate.getTotalQty());
        po.setShadow(aggregate.isShadow() ? 1 : 0);
        po.setTriggerSource(aggregate.getTriggerSource());
        po.setStatus(aggregate.getStatus() == null ? null : aggregate.getStatus().name());
        po.setVersion(aggregate.getVersion());
        return po;
    }

    public static List<ReplenishDecisionItemPO> toItemPOs(ReplenishDecisionAggregate aggregate) {
        List<ReplenishDecisionItemPO> result = new ArrayList<>();
        for (ReplenishDecisionItemEntity item : aggregate.getItems()) {
            ReplenishDecisionItemPO po = new ReplenishDecisionItemPO();
            po.setId(item.getId());
            po.setDecisionId(aggregate.getId());
            po.setSkuId(item.getSkuId());
            po.setColorCode(item.getColorCode());
            po.setSizeCode(item.getSizeCode());
            po.setQty(item.qtyValue());
            result.add(po);
        }
        return result;
    }
}