package com.reflow.domain.replenish.model.param;

import com.reflow.common.base.BaseParam;

/**
 * 追单量计算入参。
 *
 * <p>注意它<b>不包含</b>规则参数：周期系数、增长系数、MOQ 都属于规则，
 * 由 {@link com.reflow.domain.replenish.model.entity.ReplenishRuleEntity} 自己持有。
 * 入参只放"款侧的事实"，规则侧的事实由规则自己管——这样同一套事实可以喂给多条规则做对比。</p>
 */
public class CalculateQtyParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    /** 首单量：公式里的基数 */
    private Integer firstOrderQty;

    /** 动销率 */
    private Double sellThroughRate;

    /** 匹配阶段用的条件，规则命中后会带进计算结果，便于追溯 */
    private MatchStyleParam matchStyleParam;

    public Integer getFirstOrderQty() {
        return firstOrderQty;
    }

    public void setFirstOrderQty(Integer firstOrderQty) {
        this.firstOrderQty = firstOrderQty;
    }

    public Double getSellThroughRate() {
        return sellThroughRate;
    }

    public void setSellThroughRate(Double sellThroughRate) {
        this.sellThroughRate = sellThroughRate;
    }

    public MatchStyleParam getMatchStyleParam() {
        return matchStyleParam;
    }

    public void setMatchStyleParam(MatchStyleParam matchStyleParam) {
        this.matchStyleParam = matchStyleParam;
    }
}