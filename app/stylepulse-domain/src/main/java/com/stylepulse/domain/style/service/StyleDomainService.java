package com.stylepulse.domain.style.service;

import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.style.model.param.ChangeStageParam;

/**
 * 款领域服务（写模式）。
 *
 * <p>接口定义在领域层，实现也在领域层——和仓储相反。
 * 区别在于：领域服务是纯业务逻辑，没有替换需求；仓储是技术实现，必须可替换。</p>
 */
public interface StyleDomainService {

    /**
     * 流转款的生命周期阶段。
     *
     * <p>标准四步：取锁 → 加载聚合根 → 调聚合根方法 → 保存。
     * 这个顺序不是形式主义：先取锁再加载，才能保证"读到的状态"到"写回"之间
     * 不会被另一个线程改掉。</p>
     */
    ResultDO<Void> changeStage(Long styleId, ChangeStageParam param);
}