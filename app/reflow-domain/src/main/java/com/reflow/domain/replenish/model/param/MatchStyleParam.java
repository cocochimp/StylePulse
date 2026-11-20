package com.reflow.domain.replenish.model.param;

import com.reflow.common.base.BaseParam;
import com.reflow.model.style.PriceBandEnum;

/**
 * 规则匹配入参。
 *
 * <p>字段就是规则条件的"另一面"：规则声明条件，这里给出款的实际值。
 * 两边字段一一对应，所以匹配逻辑可以写得很直白——
 * 一旦发现需要在这里加"临时字段"，就该考虑是不是规则建模漏了一维。</p>
 */
public class MatchStyleParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long styleId;
    private Long categoryId;
    private PriceBandEnum priceBand;

    /** 上架天数 */
    private Integer ageDays;

    /** 可售天数 */
    private Double dos;

    /** 动销率，0.83 表示 83% */
    private Double sellThroughRate;

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public PriceBandEnum getPriceBand() {
        return priceBand;
    }

    public void setPriceBand(PriceBandEnum priceBand) {
        this.priceBand = priceBand;
    }

    public Integer getAgeDays() {
        return ageDays;
    }

    public void setAgeDays(Integer ageDays) {
        this.ageDays = ageDays;
    }

    public Double getDos() {
        return dos;
    }

    public void setDos(Double dos) {
        this.dos = dos;
    }

    public Double getSellThroughRate() {
        return sellThroughRate;
    }

    public void setSellThroughRate(Double sellThroughRate) {
        this.sellThroughRate = sellThroughRate;
    }
}