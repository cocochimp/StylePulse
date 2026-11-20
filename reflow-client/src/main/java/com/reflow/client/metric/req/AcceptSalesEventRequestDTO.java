package com.reflow.client.metric.req;

import com.reflow.common.base.BaseParam;
import com.reflow.common.result.ResultDO;

/**
 * 销量事件接入请求。
 *
 * <p>对应 T2 的入口：每个事件都带 eventId（幂等）与 eventTime（治乱序）。
 * 这两个字段不是可选的——缺 eventId 就无法去重，缺 eventTime 就无法判断迟到，
 * 所以 check() 里对它们是硬校验。</p>
 */
public class AcceptSalesEventRequestDTO extends BaseParam {

    private static final long serialVersionUID = 1L;

    /** 事件唯一标识，由上游生成（建议 雪花ID / 业务流水号） */
    private String eventId;

    /** 款 ID */
    private Long styleId;

    /** 本次事件代表的销量件数，必须 > 0 */
    private Integer qty;

    /** 事件实际发生时间（毫秒时间戳），用于水位线与迟到判断 */
    private Long eventTimeMillis;

    public ResultDO<Void> check() {
        if (eventId == null || eventId.isBlank()) {
            return ResultDO.buildFailResult("PARAM_ERROR", "eventId 不能为空");
        }
        if (styleId == null) {
            return ResultDO.buildFailResult("PARAM_ERROR", "styleId 不能为空");
        }
        if (qty == null || qty <= 0) {
            return ResultDO.buildFailResult("PARAM_ERROR", "qty 必须大于 0");
        }
        if (eventTimeMillis == null || eventTimeMillis <= 0) {
            return ResultDO.buildFailResult("PARAM_ERROR", "eventTimeMillis 不能为空");
        }
        return ResultDO.buildSuccessResult();
    }

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