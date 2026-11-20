package com.reflow.domain.metric.model.aggregate;

import com.reflow.common.base.BaseAggregate;
import com.reflow.domain.metric.model.param.AcceptSalesEventParam;

/**
 * 销量水位聚合根（写模式）。
 *
 * <p>它只管一件事：<b>这条事件的时间是否晚于已见水位</b>。
 * 桶里的具体销量数字不在这里——那是"大量时序数据"，属于存储侧，
 * 塞进聚合根会让聚合变成一个数据容器，失去边界意义。</p>
 *
 * <p>那聚合根的存在价值是什么？把"什么算有效事件"这条规则收拢：
 * 乱序判定、同时间戳判定、水位推进。如果这些散在消费者里，
 * 三处消费者迟早会出现三套判定。</p>
 */
public class SalesWatermarkAggregate extends BaseAggregate<Long> {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    /** 已见的最大事件时间；null 表示还没有任何事件 */
    private Long lastEventTime;

    /** 乐观锁版本：仓储保存时用它做 CAS */
    private Integer version;

    public SalesWatermarkAggregate() {
    }

    public SalesWatermarkAggregate(Long styleId) {
        this.styleId = styleId;
        this.version = 0;
    }

    /**
     * 推进水位。
     *
     * @return true 表示事件时序正确、水位已推进；false 表示迟到（或时间戳相同）
     *
     * <p>为什么相同时间戳也判为迟到：两个时间戳相同的事件无法确定先后，
     * 如果都接受，同一个桶会被加两次而水位不变，重复投递就检测不出来了。
     * 宁可少算一笔（可由日批对账补），也不能多算——多算会直接导致多追单。</p>
     */
    public boolean advance(AcceptSalesEventParam param) {
        if (param == null || param.getEventTimeMillis() == null) {
            return false;
        }
        if (this.lastEventTime != null && param.getEventTimeMillis() <= this.lastEventTime) {
            return false;
        }
        this.lastEventTime = param.getEventTimeMillis();
        this.version = this.version == null ? 1 : this.version + 1;
        return true;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Long getLastEventTime() {
        return lastEventTime;
    }

    public void setLastEventTime(Long lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}