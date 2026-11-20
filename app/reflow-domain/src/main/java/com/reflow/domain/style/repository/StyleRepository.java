package com.reflow.domain.style.repository;

import com.reflow.common.base.AggregateRepository;
import com.reflow.common.lock.LevelLock;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.style.model.aggregate.StyleAggregate;
import com.reflow.domain.style.model.param.StyleQuery;
import com.reflow.model.style.StageEnum;

import java.util.List;

/**
 * 款仓储接口（定义在领域层，实现在基础设施层）。
 *
 * <p>注意这里<b>没有任何技术词汇</b>：没有 Mapper、没有 Redis、没有 SQL。
 * 领域层描述的是"我要存取一个款聚合"，怎么存是基础设施的事。
 * 这条界线一破，领域层就会开始依赖表结构，之后换个存储就得改业务代码。</p>
 */
public interface StyleRepository extends AggregateRepository<StyleAggregate, Long> {

    /** 按条件加载聚合根；查不到返回 success(null)，由调用方判断"不存在" */
    ResultDO<StyleAggregate> query(StyleQuery query);

    /** 保存聚合根（新增或更新），内部用 version 做乐观锁 */
    ResultDO<Void> save(StyleAggregate aggregate);

    /**
     * 构建分布式锁。
     *
     * <p>锁由领域层"声明"，由基础设施"实现"——领域层说"我要一把 key 为 x 的锁"，
     * 具体是 Redis 还是本地锁，领域层不需要知道。</p>
     */
    LevelLock buildLock(String lockKey);

    /** 按阶段批量拉取，供巡检任务使用 */
    ResultDO<List<StyleAggregate>> queryByStages(List<StageEnum> stages, int limit);
}