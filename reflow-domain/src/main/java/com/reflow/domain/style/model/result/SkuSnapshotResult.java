package com.reflow.domain.style.model.result;

import com.reflow.common.base.BaseResult;

/**
 * SKU 快照（读输出对象）。
 *
 * <p>为什么要单独定义快照，而不是把 {@code SkuEntity} 直接交给外部：
 * 实体是可变的、属于聚合内部；一旦流出去，外部就可能拿着它改库存绕过聚合根。
 * 快照是不可变的只读拷贝，跨出聚合边界一律用快照。</p>
 *
 * <p>拆量算法（纯计算模式）的入参就是这组快照——它不需要知道款的状态机，
 * 只需要"哪些 SKU、各自库存和历史销量多少"。</p>
 */
public class SkuSnapshotResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    private final Long skuId;
    private final String colorCode;
    private final String sizeCode;
    private final int availableStock;
    private final int recentSales;

    public SkuSnapshotResult(Long skuId, String colorCode, String sizeCode, int availableStock, int recentSales) {
        this.skuId = skuId;
        this.colorCode = colorCode;
        this.sizeCode = sizeCode;
        this.availableStock = availableStock;
        this.recentSales = recentSales;
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

    public int getAvailableStock() {
        return availableStock;
    }

    public int getRecentSales() {
        return recentSales;
    }

    /** 断码且有需求：拆量时应当放大权重 */
    public boolean isBroken() {
        return availableStock <= 0 && recentSales > 0;
    }
}