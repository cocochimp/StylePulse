package com.stylepulse.domain.metric.repository;

import com.stylepulse.common.base.AggregateRepository;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.metric.model.aggregate.SalesWatermarkAggregate;
import com.stylepulse.domain.metric.model.param.SalesWindowQuery;

/**
 * 销量水位仓储接口。
 *
 * <p>接口里出现了"桶"这个概念（incrBucket / sumWindow），但没有出现 Redis、ZSet、HASH。
 * 这是刻意的：领域层需要表达"按时间桶累加"，这是业务语义；
 * 至于用 Redis HASH 还是 ClickHouse，是基础设施的选择。</p>
 */
public interface SalesWatermarkRepository extends AggregateRepository<SalesWatermarkAggregate, Long> {

    /**
     * 事件去重标记（原子 SETNX）。
     *
     * <p>返回 true 表示首次见到该事件。必须由存储保证原子性——
     * 应用层先查后写一定会漏，两个线程同时查到"不存在"是常态。</p>
     */
    ResultDO<Boolean> tryMarkEvent(String eventId);

    /** 在指定桶上累加销量 */
    ResultDO<Void> incrBucket(Long styleId, long bucketTs, int qty);

    /** 汇总窗口内销量 */
    ResultDO<Integer> sumWindow(SalesWindowQuery query);

    /** 加载水位聚合根；不存在时返回一个 lastEventTime=null 的新对象 */
    ResultDO<SalesWatermarkAggregate> query(Long styleId);

    /** 保存水位（乐观锁） */
    ResultDO<Void> save(SalesWatermarkAggregate aggregate);

    /** 记录迟到事件，供日批对账时补偿 */
    ResultDO<Void> markLateEvent(String eventId, Long styleId);
}