package com.reflow.infrastructure.replenish.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.result.DispatchMessageResult;
import com.reflow.domain.replenish.repository.DispatchMessageRepository;
import com.reflow.infrastructure.replenish.mysql.mapper.LocalMessageMapper;
import com.reflow.infrastructure.replenish.mysql.po.LocalMessagePO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地消息表实现。
 *
 * <p>投递逻辑不在这里，这里只负责"消息的存取状态机"。
 * 这样做的原因：投递是技术动作（HTTP / MQ），存状态是数据动作。
 * 把两者混在一起，重试策略就会和渠道绑死，换渠道要重写整个仓储。</p>
 */
@Component
public class DispatchMessageRepositoryImpl implements DispatchMessageRepository {

    private static final Logger log = LoggerFactory.getLogger(DispatchMessageRepositoryImpl.class);

    private static final String BIZ_TYPE_REPLENISH = "REPLENISH_DISPATCH";

    private final LocalMessageMapper localMessageMapper;

    public DispatchMessageRepositoryImpl(LocalMessageMapper localMessageMapper) {
        this.localMessageMapper = localMessageMapper;
    }

    @Override
    public ResultDO<Void> savePending(String msgId, Long decisionId, String payload) {
        try {
            LocalMessagePO po = new LocalMessagePO();
            po.setMsgId(msgId);
            po.setBizType(BIZ_TYPE_REPLENISH);
            po.setBizId(decisionId);
            po.setPayload(payload);
            po.setStatus(LocalMessagePO.STATUS_INIT);
            po.setRetryCount(0);
            po.setNextRetryAt(LocalDateTime.now());
            localMessageMapper.insert(po);
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("写入本地消息失败, decisionsId={}", decisionId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "写入本地消息失败");
        }
    }

    @Override
    public ResultDO<List<DispatchMessageResult>> queryPending(int limit) {
        try {
            List<LocalMessagePO> pos = localMessageMapper.selectList(
                    new LambdaQueryWrapper<LocalMessagePO>()
                            .in(LocalMessagePO::getStatus, LocalMessagePO.STATUS_INIT, LocalMessagePO.STATUS_SENT)
                            .le(LocalMessagePO::getNextRetryAt, LocalDateTime.now())
                            .orderByAsc(LocalMessagePO::getId)
                            .last("LIMIT " + Math.max(1, limit)));

            List<DispatchMessageResult> result = new ArrayList<>(pos.size());
            for (LocalMessagePO po : pos) {
                result.add(new DispatchMessageResult(
                        po.getMsgId(), po.getBizId(), po.getPayload(),
                        po.getRetryCount() == null ? 0 : po.getRetryCount()));
            }
            return ResultDO.buildSuccessResult(result);
        } catch (Throwable e) {
            log.error("拉取待投递消息失败", e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "拉取待投递消息失败");
        }
    }

    @Override
    public ResultDO<Void> markSent(String msgId) {
        return updateStatus(msgId, LocalMessagePO.STATUS_SENT);
    }

    @Override
    public ResultDO<Void> markAck(String msgId) {
        return updateStatus(msgId, LocalMessagePO.STATUS_ACK);
    }

    @Override
    public ResultDO<Void> markFailed(String msgId, int retryDelaySeconds) {
        try {
            LocalMessagePO po = localMessageMapper.selectOne(
                    new LambdaQueryWrapper<LocalMessagePO>().eq(LocalMessagePO::getMsgId, msgId));
            if (po == null) {
                return ResultDO.buildFailResult("MSG_NOT_FOUND", "消息不存在");
            }
            int retryCount = (po.getRetryCount() == null ? 0 : po.getRetryCount()) + 1;
            localMessageMapper.update(null, new LambdaUpdateWrapper<LocalMessagePO>()
                    .eq(LocalMessagePO::getMsgId, msgId)
                    .set(LocalMessagePO::getRetryCount, retryCount)
                    .set(LocalMessagePO::getNextRetryAt, LocalDateTime.now().plusSeconds(retryDelaySeconds)));
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("标记消息失败态异常, msgId={}", msgId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "标记消息失败");
        }
    }

    private ResultDO<Void> updateStatus(String msgId, String status) {
        try {
            localMessageMapper.update(null, new LambdaUpdateWrapper<LocalMessagePO>()
                    .eq(LocalMessagePO::getMsgId, msgId)
                    .set(LocalMessagePO::getStatus, status));
            return ResultDO.buildSuccessResult();
        } catch (Throwable e) {
            log.error("更新消息状态异常, msgId={}, status={}", msgId, status, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "更新消息状态失败");
        }
    }
}