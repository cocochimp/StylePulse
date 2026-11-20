package com.reflow.domain.style.model.entity;

import com.reflow.common.base.BaseEntity;
import com.reflow.common.field.Field;

/**
 * 款下的颜色×尺码 SKU（实体）。
 *
 * <p>它只活在 {@link com.reflow.domain.style.model.aggregate.StyleAggregate} 内部：
 * 外部不能直接改某个 SKU 的库存，必须经过聚合根——
 * 因为"改一个 SKU 的库存"往往还牵连款级状态（比如全尺码售罄 → 该进清仓）。</p>
 *
 * <p>属性用 {@link Field} 包装，是为了区分"未加载"和"值为空"。
 * 列表页可能只查了 id + 库存，保存时就不会把没查出来的 recentSales 覆盖成 null。</p>
 */
public class SkuEntity extends BaseEntity<Long> {

    private static final long serialVersionUID = 1L;

    private String colorCode;

    private String sizeCode;

    /** 可售库存 */
    private Field<Integer> availableStock;

    /** 近 N 天销量，用于算断码与曲线权重 */
    private Field<Integer> recentSales;

    public SkuEntity() {
    }

    public SkuEntity(Long id, String colorCode, String sizeCode, Integer availableStock, Integer recentSales) {
        setId(id);
        this.colorCode = colorCode;
        this.sizeCode = sizeCode;
        this.availableStock = Field.of(availableStock);
        this.recentSales = Field.of(recentSales);
    }

    /** 判断类方法：这个 SKU 是否已经卖断 */
    public boolean isSoldOut() {
        return availableStock != null && availableStock.orElse(0) <= 0;
    }

    /** 判断类方法：是否属于"断了且有需求"的断码 SKU——这才是要重点补货的对象 */
    public boolean isBroken() {
        return isSoldOut() && recentSales != null && recentSales.orElse(0) > 0;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public void setSizeCode(String sizeCode) {
        this.sizeCode = sizeCode;
    }

    public Field<Integer> getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Field<Integer> availableStock) {
        this.availableStock = availableStock;
    }

    public Field<Integer> getRecentSales() {
        return recentSales;
    }

    public void setRecentSales(Field<Integer> recentSales) {
        this.recentSales = recentSales;
    }
}