package com.stylepulse.domain.capacity.model.aggregate;

import com.stylepulse.common.base.BaseAggregate;
import com.stylepulse.common.exception.AggregateException;
import com.stylepulse.domain.capacity.model.param.ReserveCapacityParam;
import com.stylepulse.model.common.BusinessDate;

/**
 * 产能槽聚合根（写模式）。
 *
 * <p>一个槽 = 工厂 × 产线 × 业务日，是一天能生产多少件的承诺。
 *
 * <p>关于原子性的分工要特别说明：聚合根的 {@code reserve} 只做<b>业务合法性</b>判断
 * （数量必须为正、不能超过余量），真正的并发安全由基础设施层的 Redis Lua 保证。
 * 为什么不在聚合根里用 synchronized：那是单机锁，多实例部署下等于没有锁。
 * 为什么不在聚合根里做"查完再扣"：那是最典型的 check-then-act 竞态。</p>
 */
public class CapacitySlotAggregate extends BaseAggregate<String> {

    private static final long serialVersionUID = 1L;

    private Long factoryId;
    private Long lineId;
    private BusinessDate bizDate;

    /** 当日总产能 */
    private Integer totalQty;

    /** 已预占（可能超时被回收） */
    private Integer reservedQty;

    /** 已确认（生产指令已下发，不可回收） */
    private Integer confirmedQty;

    /**
     * 业务校验 + 本地记账。
     *
     * <p>注意这只是"内存里算一下"，不等于占用成功。
     * 真正决定成败的是仓储的原子操作——这里的作用是让非法请求在进入 IO 之前就被挡掉，
     * 减少无意义的 Redis 往返。</p>
     */
    public void reserve(ReserveCapacityParam param) {
        if (param == null || param.getQty() == null || param.getQty() <= 0) {
            throw new AggregateException("PARAM_ERROR", "预占数量必须大于 0");
        }
        if (param.getQty() > remainingQty()) {
            throw new AggregateException("CAPACITY_NOT_ENOUGH",
                    "产能不足，剩余 " + remainingQty() + "，申请 " + param.getQty());
        }
        this.reservedQty = (reservedQty == null ? 0 : reservedQty) + param.getQty();
    }

    /** 剩余可预占量 */
    public int remainingQty() {
        int total = totalQty == null ? 0 : totalQty;
        int reserved = reservedQty == null ? 0 : reservedQty;
        int confirmed = confirmedQty == null ? 0 : confirmedQty;
        return Math.max(0, total - reserved - confirmed);
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

    public BusinessDate getBizDate() {
        return bizDate;
    }

    public void setBizDate(BusinessDate bizDate) {
        this.bizDate = bizDate;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public Integer getReservedQty() {
        return reservedQty;
    }

    public void setReservedQty(Integer reservedQty) {
        this.reservedQty = reservedQty;
    }

    public Integer getConfirmedQty() {
        return confirmedQty;
    }

    public void setConfirmedQty(Integer confirmedQty) {
        this.confirmedQty = confirmedQty;
    }
}