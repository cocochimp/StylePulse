package com.stylepulse.infrastructure.capacity.converter;

import com.stylepulse.domain.capacity.model.aggregate.CapacitySlotAggregate;
import com.stylepulse.infrastructure.capacity.mysql.po.CapacitySlotPO;
import com.stylepulse.model.common.BusinessDate;

/**
 * 产能槽 PO ↔ 聚合转换。
 */
public final class CapacityConverter {

    private CapacityConverter() {
    }

    public static CapacitySlotAggregate toAggregate(CapacitySlotPO po, int usedQty) {
        if (po == null) {
            return null;
        }
        CapacitySlotAggregate aggregate = new CapacitySlotAggregate();
        aggregate.setId(po.getFactoryId() + ":" + po.getLineId() + ":" + po.getBizDate());
        aggregate.setFactoryId(po.getFactoryId());
        aggregate.setLineId(po.getLineId());
        aggregate.setBizDate(BusinessDate.of(java.time.LocalDate.parse(po.getBizDate())));
        aggregate.setTotalQty(po.getTotalQty());
        // 聚合里 reserved 表示"已被占用的量"，由 hold 汇总而来
        aggregate.setReservedQty(usedQty);
        aggregate.setConfirmedQty(0);
        return aggregate;
    }
}