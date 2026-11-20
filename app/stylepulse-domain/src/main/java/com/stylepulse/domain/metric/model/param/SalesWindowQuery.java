package com.stylepulse.domain.metric.model.param;

import com.stylepulse.common.base.BaseParam;

/**
 * 窗口销量查询条件。
 */
public class SalesWindowQuery extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    /** 起始桶（含） */
    private Long fromBucket;

    /** 结束桶（含） */
    private Long toBucket;

    public SalesWindowQuery() {
    }

    public SalesWindowQuery(Long styleId, Long fromBucket, Long toBucket) {
        this.styleId = styleId;
        this.fromBucket = fromBucket;
        this.toBucket = toBucket;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Long getFromBucket() {
        return fromBucket;
    }

    public void setFromBucket(Long fromBucket) {
        this.fromBucket = fromBucket;
    }

    public Long getToBucket() {
        return toBucket;
    }

    public void setToBucket(Long toBucket) {
        this.toBucket = toBucket;
    }
}