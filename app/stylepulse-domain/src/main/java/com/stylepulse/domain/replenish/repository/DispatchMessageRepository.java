package com.stylepulse.domain.replenish.repository;

import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.replenish.model.result.DispatchMessageResult;

import java.util.List;

/**
 * 生产指令下发仓储（本地消息表）。
 *
 * <p>为什么叫 Repository 而不是 MQ：在领域语义里，它是"待发送指令的存档"，
 * 是数据；具体投递到 RocketMQ 还是直接调 HTTP，是基础设施的选择。
 *
 * <p>这个模式解决的是经典的"双写不一致"：如果先写库再发 MQ，
 * 发 MQ 失败就丢单；如果先发 MQ 再写库，MQ 消费时可能查不到数据。
 * 本地消息表把两件事变成"一个事务写两张表 + 异步投递"，
 * 把分布式一致性降级成"至少投递一次 + 消费端幂等"。</p>
 */
public interface DispatchMessageRepository {

    /** 与业务数据同事务写入（应用层负责事务边界） */
    ResultDO<Void> savePending(String msgId, Long decisionId, String payload);

    /** 拉取待投递消息（含超时未确认的） */
    ResultDO<List<DispatchMessageResult>> queryPending(int limit);

    /** 投递成功，等待对方 ACK */
    ResultDO<Void> markSent(String msgId);

    /** 对方已确认 */
    ResultDO<Void> markAck(String msgId);

    /** 投递失败，安排下次重试 */
    ResultDO<Void> markFailed(String msgId, int retryDelaySeconds);
}