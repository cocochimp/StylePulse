package com.stylepulse.domain.capacity.repository;

import com.stylepulse.common.base.AggregateRepository;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.capacity.model.aggregate.CapacitySlotAggregate;
import com.stylepulse.domain.capacity.model.param.ReserveCapacityParam;

import java.util.List;

/**
 * 产能仓储接口。
 *
 * <p>{@link #tryReserve} 的命名里带 try，是在提示调用方：<b>它会失败，且失败是常态</b>。
 * 命名上的诚实很重要——叫 {@code reserve} 会让调用方以为"调用即成功"，
 * 从而漏掉失败分支的补偿逻辑。</p>
 */
public interface CapacityRepository extends AggregateRepository<CapacitySlotAggregate, String> {

    /**
     * 原子预占：读余量 + 判断 + 扣减必须在同一次操作内完成。
     *
     * @return 成功时带 holdId 与剩余量；失败时带原因与当前剩余量
     */
    ResultDO<com.stylepulse.domain.capacity.model.result.ReserveCapacityResult> tryReserve(ReserveCapacityParam param);

    /** 确认预占（预占转为占用，不再超时回收） */
    ResultDO<Void> confirm(String holdId);

    /** 释放预占（超时回收或主动取消） */
    ResultDO<Void> release(String holdId);

    /** 加载槽位账本 */
    ResultDO<CapacitySlotAggregate> querySlot(String slotKey);

    /**
     * 查询已超时未被确认的预占凭证。
     *
     * <p>超时回收是"预占"这个机制能成立的前提：没有回收，
     * 一次失败的下发会永久占着产能，产能池会越来越少直到归零。</p>
     */
    ResultDO<List<String>> queryExpiredHoldIds(int limit);
}