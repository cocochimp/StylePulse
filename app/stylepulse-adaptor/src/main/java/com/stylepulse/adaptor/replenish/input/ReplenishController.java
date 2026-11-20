package com.stylepulse.adaptor.replenish.input;

import com.stylepulse.client.replenish.ReplenishCalculateQueryAppService;
import com.stylepulse.client.replenish.ReplenishDecisionAppService;
import com.stylepulse.client.replenish.req.ApproveDecisionRequestDTO;
import com.stylepulse.client.replenish.req.DiagnoseStyleRequestDTO;
import com.stylepulse.client.replenish.req.OpenDecisionRequestDTO;
import com.stylepulse.client.replenish.res.ApproveDecisionResponseDTO;
import com.stylepulse.client.replenish.res.DiagnoseStyleResponseDTO;
import com.stylepulse.client.replenish.res.OpenDecisionResponseDTO;
import com.stylepulse.common.result.ResultDO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 返单决策入口。
 *
 * <p>三个接口对应业务链上的三个动作：
 * <ul>
 *   <li>{@code /diagnose} 只诊断不开单——运营看板上点"为什么这款不追"就走这里</li>
 *   <li>{@code /decisions} 开单——定时巡检、事件触发、人工催单三路都打这个接口</li>
 *   <li>{@code /decisions/approve} 审批——通过后自动占产能并下发</li>
 * </ul>
 * 注意"三路都打同一个接口"是有意为之：入口收敛，幂等才有意义。</p>
 */
@RestController
@RequestMapping("/api/replenish")
public class ReplenishController {

    private final ReplenishCalculateQueryAppService calculateQueryAppService;
    private final ReplenishDecisionAppService decisionAppService;

    public ReplenishController(ReplenishCalculateQueryAppService calculateQueryAppService,
                               ReplenishDecisionAppService decisionAppService) {
        this.calculateQueryAppService = calculateQueryAppService;
        this.decisionAppService = decisionAppService;
    }

    @PostMapping("/diagnose")
    public ResultDO<DiagnoseStyleResponseDTO> diagnose(@RequestBody DiagnoseStyleRequestDTO requestDTO) {
        return calculateQueryAppService.diagnoseStyle(requestDTO);
    }

    @PostMapping("/decisions")
    public ResultDO<OpenDecisionResponseDTO> openDecision(@RequestBody OpenDecisionRequestDTO requestDTO) {
        return decisionAppService.openDecision(requestDTO);
    }

    @PostMapping("/decisions/approve")
    public ResultDO<ApproveDecisionResponseDTO> approve(@RequestBody ApproveDecisionRequestDTO requestDTO) {
        return decisionAppService.approveDecision(requestDTO);
    }
}