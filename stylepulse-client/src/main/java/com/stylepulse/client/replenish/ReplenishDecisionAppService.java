package com.stylepulse.client.replenish;

import com.stylepulse.client.common.ApplicationCmdService;
import com.stylepulse.client.replenish.req.ApproveDecisionRequestDTO;
import com.stylepulse.client.replenish.req.OpenDecisionRequestDTO;
import com.stylepulse.client.replenish.res.ApproveDecisionResponseDTO;
import com.stylepulse.client.replenish.res.OpenDecisionResponseDTO;
import com.stylepulse.common.result.ResultDO;

/**
 * 返单决策应用服务（写模式）。
 *
 * <p>两个动作对应决策单生命周期的前两跳：
 * openDecision（生成）→ approveDecision（审批 + 占产能 + 下发）。</p>
 */
public interface ReplenishDecisionAppService extends ApplicationCmdService {

    /**
     * 为某个款开启返单决策。
     *
     * <p>并发安全由领域层保证（分布式锁 → 状态机 CAS → 唯一键），
     * 本接口承诺：同一款同一时刻只会存在一张"进行中"的决策单。
     */
    ResultDO<OpenDecisionResponseDTO> openDecision(OpenDecisionRequestDTO requestDTO);

    /**
     * 审批决策单。批准后会自动尝试预占产能，成功则下发生产指令。
     */
    ResultDO<ApproveDecisionResponseDTO> approveDecision(ApproveDecisionRequestDTO requestDTO);
}