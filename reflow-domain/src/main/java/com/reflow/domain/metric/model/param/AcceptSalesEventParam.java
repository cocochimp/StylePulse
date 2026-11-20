package com.reflow.domain.metric.model.param;

import com.reflow.common.base.BaseParam;

/**
 * 销量事件入参（垂体内）。
 *
 * <p>注意 {@code bucketTs} 是<b>由外部传入而不是在领域层算</b>的：
 * 对齐规则属于存储细节（用什么粒度分桶会变），领域层只关心"这条事件的业务时间"。
 * 这样将来把桶从 5 分钟改成 1 分钟，领域层一行不用改。</p>
 */
public class AcceptSalesEventParam extends BaseParam {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long styleId;

    private Integer qty;

    /** 事件发生时间（毫秒），用于水位线比较 */
    private Long eventTimeMillis;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Long getEventTimeMillis() {
        return eventTimeMillis;
    }

    public void setEventTimeMillis(Long eventTimeMillis) {
        this.eventTimeMillis = eventTimeMillis;
    }
}