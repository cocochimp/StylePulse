package com.reflow.application.capacity.assembler;

import com.reflow.client.capacity.req.ConfirmCapacityRequestDTO;
import com.reflow.client.capacity.req.ReserveCapacityRequestDTO;
import com.reflow.client.capacity.res.ConfirmCapacityResponseDTO;
import com.reflow.client.capacity.res.ReserveCapacityResponseDTO;
import com.reflow.domain.capacity.model.param.ConfirmCapacityParam;
import com.reflow.domain.capacity.model.param.ReserveCapacityParam;
import com.reflow.domain.capacity.model.result.ReserveCapacityResult;
import com.reflow.model.common.BusinessDate;

import java.time.LocalDate;

/**
 * 产能 DTO ↔ 领域对象转换。
 */
public class CapacityAssembler {

    private CapacityAssembler() {
    }

    public static ReserveCapacityParam toReserveParam(ReserveCapacityRequestDTO dto) {
        ReserveCapacityParam param = new ReserveCapacityParam();
        param.setFactoryId(dto.getFactoryId());
        param.setLineId(dto.getLineId());
        // 字符串日期在边界处一次性转成值对象，内部全程用 BusinessDate，避免"到处解析"
        param.setBizDate(BusinessDate.of(LocalDate.parse(dto.getBizDate())));
        param.setQty(dto.getQty());
        param.setDecisionId(dto.getDecisionId());
        param.setOperator(dto.getOperator());
        return param;
    }

    public static ConfirmCapacityParam toConfirmParam(ConfirmCapacityRequestDTO dto) {
        ConfirmCapacityParam param = new ConfirmCapacityParam();
        param.setHoldId(dto.getHoldId());
        param.setOperator(dto.getOperator());
        return param;
    }

    public static ReserveCapacityResponseDTO toReserveResponseDTO(ReserveCapacityResult result) {
        ReserveCapacityResponseDTO dto = new ReserveCapacityResponseDTO();
        dto.setHoldId(result.getHoldId());
        dto.setSuccess(result.isSuccess());
        dto.setRemainQty(result.getRemainQty());
        dto.setReason(result.getReason());
        return dto;
    }

    public static ConfirmCapacityResponseDTO toConfirmResponseDTO(String holdId) {
        ConfirmCapacityResponseDTO dto = new ConfirmCapacityResponseDTO();
        dto.setHoldId(holdId);
        dto.setConfirmed(true);
        return dto;
    }
}