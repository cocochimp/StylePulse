package com.reflow.application.capacity.scenario;

import com.reflow.application.capacity.assembler.CapacityAssembler;
import com.reflow.client.capacity.CapacityAppService;
import com.reflow.client.capacity.req.ConfirmCapacityRequestDTO;
import com.reflow.client.capacity.req.ReserveCapacityRequestDTO;
import com.reflow.client.capacity.res.ConfirmCapacityResponseDTO;
import com.reflow.client.capacity.res.ReserveCapacityResponseDTO;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.capacity.model.result.ReserveCapacityResult;
import com.reflow.domain.capacity.service.CapacityDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 产能应用服务（写模式）。
 *
 * <p>薄薄一层：校验 → 转换 → 调领域服务 → 转换。没有业务判断。
 * "薄"是好事——说明领域层把该管的都管了。</p>
 */
@Service
public class CapacityAppServiceImpl implements CapacityAppService {

    private static final Logger log = LoggerFactory.getLogger(CapacityAppServiceImpl.class);

    private final CapacityDomainService capacityDomainService;

    public CapacityAppServiceImpl(CapacityDomainService capacityDomainService) {
        this.capacityDomainService = capacityDomainService;
    }

    @Override
    public ResultDO<ReserveCapacityResponseDTO> reserveCapacity(ReserveCapacityRequestDTO requestDTO) {
        ResultDO<Void> checkResult = requestDTO.check();
        if (!checkResult.isSuccess()) {
            return ResultDO.buildFailResult(checkResult.getCode(), checkResult.getMsg());
        }
        try {
            ResultDO<ReserveCapacityResult> result =
                    capacityDomainService.reserve(CapacityAssembler.toReserveParam(requestDTO));
            if (!result.isSuccess()) {
                return ResultDO.buildFailResult(result.getCode(), result.getMsg());
            }
            return ResultDO.buildSuccessResult(CapacityAssembler.toReserveResponseDTO(result.getData()));
        } catch (Throwable e) {
            log.error("产能预占异常, factoryId={}, lineId={}", requestDTO.getFactoryId(), requestDTO.getLineId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }

    @Override
    public ResultDO<ConfirmCapacityResponseDTO> confirmCapacity(ConfirmCapacityRequestDTO requestDTO) {
        ResultDO<Void> checkResult = requestDTO.check();
        if (!checkResult.isSuccess()) {
            return ResultDO.buildFailResult(checkResult.getCode(), checkResult.getMsg());
        }
        try {
            ResultDO<Void> result = capacityDomainService.confirm(
                    CapacityAssembler.toConfirmParam(requestDTO));
            if (!result.isSuccess()) {
                return ResultDO.buildFailResult(result.getCode(), result.getMsg());
            }
            return ResultDO.buildSuccessResult(
                    CapacityAssembler.toConfirmResponseDTO(requestDTO.getHoldId()));
        } catch (Throwable e) {
            log.error("产能确认异常, holdId={}", requestDTO.getHoldId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}