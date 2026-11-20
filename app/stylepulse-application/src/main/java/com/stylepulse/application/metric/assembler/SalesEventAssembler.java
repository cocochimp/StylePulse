package com.stylepulse.application.metric.assembler;

import com.stylepulse.client.metric.req.AcceptSalesEventRequestDTO;
import com.stylepulse.client.metric.res.AcceptSalesEventResponseDTO;
import com.stylepulse.domain.metric.model.param.AcceptSalesEventParam;
import com.stylepulse.domain.metric.model.result.AcceptSalesEventResult;

/**
 * 销量事件 DTO ↔ 领域对象转换。
 */
public class SalesEventAssembler {

    private SalesEventAssembler() {
    }

    public static AcceptSalesEventParam toParam(AcceptSalesEventRequestDTO dto) {
        AcceptSalesEventParam param = new AcceptSalesEventParam();
        param.setEventId(dto.getEventId());
        param.setStyleId(dto.getStyleId());
        param.setQty(dto.getQty());
        param.setEventTimeMillis(dto.getEventTimeMillis());
        param.setOperator(dto.getOperator());
        return param;
    }

    public static AcceptSalesEventResponseDTO toResponseDTO(AcceptSalesEventResult result, Integer windowSales) {
        return new AcceptSalesEventResponseDTO(result.isAccepted(), result.getReason(), windowSales);
    }
}