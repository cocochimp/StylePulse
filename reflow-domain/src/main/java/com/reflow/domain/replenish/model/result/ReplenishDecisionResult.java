package com.reflow.domain.replenish.model.result;

import com.reflow.common.base.BaseResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 决策单快照（跨层传输用）。
 *
 * <p>为什么不直接把聚合根返回给应用层：聚合根带方法，一旦流出领域层，
 * 上层就可能绕过仓储直接改它。返回快照，等于把"只能通过领域服务改状态"这条规则焊死。</p>
 */
public class ReplenishDecisionResult extends BaseResult {

    private static final long serialVersionUID = 1L;

    private Long decisionId;
    private Long styleId;
    private Integer round;
    private String status;
    private Integer totalQty;
    private boolean shadow;

    /** true 表示本次调用命中了幂等，返回的是已存在的单 */
    private boolean duplicated;

    private List<ItemView> items = new ArrayList<>();

    /** 尺码行视图（只读） */
    public static class ItemView {
        private final Long skuId;
        private final String colorCode;
        private final String sizeCode;
        private final int qty;

        public ItemView(Long skuId, String colorCode, String sizeCode, int qty) {
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

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public boolean isShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    public boolean isDuplicated() {
        return duplicated;
    }

    public void setDuplicated(boolean duplicated) {
        this.duplicated = duplicated;
    }

    public List<ItemView> getItems() {
        return items;
    }

    public void setItems(List<ItemView> items) {
        this.items = items;
    }
}