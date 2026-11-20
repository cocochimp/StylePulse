package com.stylepulse.domain.replenish.repository;

import com.stylepulse.common.base.AggregateRepository;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.replenish.model.aggregate.ReplenishRuleAggregate;
import com.stylepulse.domain.replenish.model.param.MatchStyleParam;

import java.util.List;

/**
 * 返单规则仓储接口 —— T1 倒排索引的对外门面。
 *
 * <p>签名只有一个条件对象，没有"查所有规则"的暴露。这是刻意的：
 * 如果提供 {@code queryAll()}，调用方迟早会写成 "for 款 × for 规则" 的双层遍历，
 * 倒排索引就白做了。接口设计本身就是性能约束。</p>
 */
public interface ReplenishRuleRepository extends AggregateRepository<ReplenishRuleAggregate, Long> {

    /**
     * 查询可能命中该款的规则候选集。
     *
     * <p>实现层先走倒排索引粗筛（位图交集），再对候选集做精确区间判断。
     * 返回的是"已精确命中"的规则，调用方不需要再判断一遍。</p>
     */
    ResultDO<List<ReplenishRuleAggregate>> queryMatchedRules(MatchStyleParam param);

    /** 查询全部启用规则，供索引重建使用 */
    ResultDO<List<ReplenishRuleAggregate>> queryAllEnabled();
}