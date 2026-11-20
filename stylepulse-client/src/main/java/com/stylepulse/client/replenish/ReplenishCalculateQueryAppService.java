package com.stylepulse.client.replenish;

import com.stylepulse.client.common.ApplicationQueryService;
import com.stylepulse.client.replenish.req.DiagnoseStyleRequestDTO;
import com.stylepulse.client.replenish.res.DiagnoseStyleResponseDTO;
import com.stylepulse.common.result.ResultDO;

/**
 * 款诊断应用服务（规则+计算模式）。
 *
 * <p>为什么归到"查询"侧：诊断不改任何状态，它只回答"这款现在该追多少"。
 * 命名用动词开头（diagnose）而不是聚合根名，这是 easy-DDD 对纯计算/规则+计算模式的约定——
 * 这类服务没有单一聚合根，硬套聚合根名会让命名撒谎。</p>
 */
public interface ReplenishCalculateQueryAppService extends ApplicationQueryService {

    /**
     * 诊断单个款：算指标 → 命中规则 → 算追单量。
     *
     * <p>不产生决策单。要看诊断结果、再人工决定追不追，走这个接口；
     * 要直接开单，走 {@link ReplenishDecisionAppService#openDecision}。
     */
    ResultDO<DiagnoseStyleResponseDTO> diagnoseStyle(DiagnoseStyleRequestDTO requestDTO);
}