package com.reflow.domain.replenish.model.result;

import com.reflow.common.base.BaseResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 尺码拆量结果。
 */
public class SizeAllocationResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    private final List<Item> items;
    private final int totalAllocated;
    private final boolean belowMoq;

    public SizeAllocationResult(List<Item> items, int totalAllocated, boolean belowMoq) {
        this.items = items;
        this.totalAllocated = totalAllocated;
        this.belowMoq = belowMoq;
    }

    public static SizeAllocationResult empty() {
        return new SizeAllocationResult(new ArrayList<>(), 0, true);
    }

    public List<Item> getItems() {
        return items;
    }

    public int getTotalAllocated() {
        return totalAllocated;
    }

    /** 是否因低于 MOQ 而放弃整款 */
    public boolean isBelowMoq() {
        return belowMoq;
    }

    /** 充血方法：是否可用（有货且守恒） */
    public boolean isUsable() {
        return !belowMoq && totalAllocated > 0;
    }

    /** 单个尺码的分配结果 */
    public static class Item {
        private final Long skuId;
        private final String colorCode;
        private final String sizeCode;
        private final int qty;

        public Item(Long skuId, String colorCode, String sizeCode, int qty) {
            this.skuId = skuId;
            this.colorCode = colorCode;
            this.sizeCode = sizeCode;
            this.qty = qty;
        }

        public Long getSkuId() {
            return skuId;
        }

        public String getColorCode() {
            return colorCode;
        }

        public String getSizeCode() {
            return sizeCode;
        }

        public int getQty() {
            return qty;
        }
    }
}