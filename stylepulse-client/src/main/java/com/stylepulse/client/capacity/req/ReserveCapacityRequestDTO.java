package com.stylepulse.client.capacity.req;

import com.stylepulse.common.base.BaseParam;
import com.stylepulse.common.result.ResultDO;

/**
 * 产能预占请求。
 *
 * <p>bizDate 用字符串而非日期对象：跨语言、跨系统传输时字符串最稳，
 * 且这里的业务日是"工厂排产日"，语义上就该是明确的 yyyy-MM-dd 文本。</p>
 */
public class ReserveCapacityRequestDTO extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long factoryId;
    private Long lineId;

    /** 业务日 yyyy-MM-dd */
    private String bizDate;

    private Integer qty;

    /** 关联的决策单，便于对账时定位 */
    private Long decisionId;

    public ResultDO<Void> check() {
        if (factoryId == null || factoryId <= 0) {
            return ResultDO.buildFailResult("PARAM_ERROR", "factoryId 非法");
        }
        if (lineId == null || lineId <= 0) {
            return ResultDO.buildFailResult("PARAM_ERROR", "lineId 非法");
        }
        if (bizDate == null || !bizDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return ResultDO.buildFailResult("PARAM_ERROR", "bizDate 格式必须为 yyyy-MM-dd");
        }
        if (qty == null || qty <= 0) {
            return ResultDO.buildFailResult("PARAM_ERROR", "qty 必须大于 0");
        }
        return ResultDO.buildSuccessResult();
    }

    public Long getFactoryId() {
        return factoryId;
    }

    public void setFactoryId(Long factoryId) {
        this.factoryId = factoryId;
    }

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public String getBizDate() {
        return bizDate;
    }

    public void setBizDate(String bizDate) {
        this.bizDate = bizDate;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }
}