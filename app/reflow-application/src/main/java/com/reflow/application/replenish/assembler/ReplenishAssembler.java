package com.reflow.application.replenish.assembler;

import com.reflow.application.replenish.model.DiagnosisInternalDTO;
import com.reflow.client.replenish.res.ApproveDecisionResponseDTO;
import com.reflow.client.replenish.res.DiagnoseStyleResponseDTO;
import com.reflow.client.replenish.res.OpenDecisionResponseDTO;
import com.reflow.domain.replenish.model.result.ReplenishDecisionResult;

/**
 * 返单 DTO ↔ 领域对象转换。
 */
public class ReplenishAssembler {

    private ReplenishAssembler() {
    }

    public static DiagnoseStyleResponseDTO toDiagnoseDTO(Long styleId, DiagnosisInternalDTO diagnosis) {
        DiagnoseStyleResponseDTO dto = new DiagnoseStyleResponseDTO();
        dto.setStyleId(styleId);
        dto.setStage(diagnosis.getStage());
        dto.setSellThroughRate(diagnosis.getSellThroughRate());
        dto.setDos(diagnosis.getDos());
        dto.setBrokenSizeRate(diagnosis.getBrokenSizeRate());
        dto.setHealthScore(diagnosis.getHealthScore());
        dto.setMatchedRuleCode(diagnosis.getRuleCode());
        dto.setRuleVersion(diagnosis.getRuleVersion());
        dto.setSuggestedQty(diagnosis.getQty());
        dto.setBelowMoq(diagnosis.isBelowMoq());
        dto.setReason(diagnosis.getReason());
        return dto;
    }

    public static OpenDecisionResponseDTO toOpenDecisionDTO(ReplenishDecisionResult result) {
        OpenDecisionResponseDTO dto = new OpenDecisionResponseDTO();
        dto.setDecisionId(result.getDecisionId());
        dto.setStyleId(result.getStyleId());
        dto.setRound(result.getRound());
        dto.setTotalQty(result.getTotalQty());
        dto.setStatus(result.getStatus());
        dto.setDuplicated(result.isDuplicated());
        return dto;
    }

    public static ApproveDecisionResponseDTO toApproveDTO(ReplenishDecisionResult decision,
                                                          String holdId,
                                                          boolean dispatched) {
        ApproveDecisionResponseDTO dto = new ApproveDecisionResponseDTO();
        dto.setDecisionId(decision.getDecisionId());
        dto.setStatus(decision.getStatus());
        dto.setTotalQty(decision.getTotalQty());
        dto.setCapacityHoldId(holdId);
        dto.setDispatched(dispatched);
        return dto;
    }
}