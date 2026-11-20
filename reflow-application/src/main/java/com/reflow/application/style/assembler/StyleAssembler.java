package com.reflow.application.style.assembler;

import com.reflow.client.style.res.QueryStyleDetailResponseDTO;
import com.reflow.domain.style.model.aggregate.StyleAggregate;
import com.reflow.domain.style.model.result.SkuSnapshotResult;

/**
 * 款 DTO ↔ 领域对象转换（应用层 Assembler）。
 *
 * <p>和基础设施层的 Converter 分工不同：Converter 管"聚合 ↔ PO"，
 * Assembler 管"DTO ↔ 领域对象"。这里负责把枚举转成字符串、日期转成文本——
 * 这些是<b>契约稳定性</b>的要求，不是业务要求。</p>
 */
public class StyleAssembler {

    private StyleAssembler() {
    }

    public static QueryStyleDetailResponseDTO toStyleDetailDTO(StyleAggregate style) {
        QueryStyleDetailResponseDTO dto = new QueryStyleDetailResponseDTO();
        dto.setStyleId(style.getId());
        dto.setStyleCode(style.getStyleCode());
        dto.setCategoryId(style.getCategoryId());
        dto.setPriceBand(style.getPriceBand() == null ? null : style.getPriceBand().name());
        dto.setLaunchDate(style.getLaunchDate() == null ? null : style.getLaunchDate().toString());
        dto.setFirstOrderQty(style.getFirstOrderQty());
        dto.setStage(style.getStage() == null ? null : style.getStage().name());
        dto.setStageLabel(style.getStage() == null ? null : style.getStage().getLabel());
        dto.setVersion(style.getVersion());

        for (SkuSnapshotResult snapshot : style.skuSnapshots()) {
            QueryStyleDetailResponseDTO.SkuLine line = new QueryStyleDetailResponseDTO.SkuLine();
            line.setSkuId(snapshot.getSkuId());
            line.setColorCode(snapshot.getColorCode());
            line.setSizeCode(snapshot.getSizeCode());
            line.setAvailableStock(snapshot.getAvailableStock());
            line.setRecentSales(snapshot.getRecentSales());
            dto.getSkus().add(line);
        }
        return dto;
    }
}