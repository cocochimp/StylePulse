package com.reflow.adaptor.capacity.input;

import com.reflow.client.capacity.CapacityAppService;
import com.reflow.client.capacity.req.ConfirmCapacityRequestDTO;
import com.reflow.client.capacity.req.ReserveCapacityRequestDTO;
import com.reflow.client.capacity.res.ConfirmCapacityResponseDTO;
import com.reflow.client.capacity.res.ReserveCapacityResponseDTO;
import com.reflow.common.result.ResultDO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产能入口：预占 + 确认。
 *
 * <p>对外暴露产能接口的价值：运营可以脱离返单流程单独锁产能
 * （比如大促前提前给某条产线占位），也可以让其他系统复用同一套产能账本。</p>
 */
@RestController
@RequestMapping("/api/capacity")
public class CapacityController {

    private final CapacityAppService capacityAppService;

    public CapacityController(CapacityAppService capacityAppService) {
        this.capacityAppService = capacityAppService;
    }

    @PostMapping("/reserve")
    public ResultDO<ReserveCapacityResponseDTO> reserve(@RequestBody ReserveCapacityRequestDTO requestDTO) {
        return capacityAppService.reserveCapacity(requestDTO);
    }

    @PostMapping("/confirm")
    public ResultDO<ConfirmCapacityResponseDTO> confirm(@RequestBody ConfirmCapacityRequestDTO requestDTO) {
        return capacityAppService.confirmCapacity(requestDTO);
    }
}