package com.reflow.application.metric.scenario;

import com.reflow.application.metric.assembler.SalesEventAssembler;
import com.reflow.client.metric.SalesEventAppService;
import com.reflow.client.metric.req.AcceptSalesEventRequestDTO;
import com.reflow.client.metric.res.AcceptSalesEventResponseDTO;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.metric.model.param.AcceptSalesEventParam;
import com.reflow.domain.metric.model.result.AcceptSalesEventResult;
import com.reflow.domain.metric.service.SalesWatermarkDomainService;
import com.reflow.model.metric.WindowSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 销量事件接入应用服务（写模式）。
 *
 * <p>它做的事很少：校验 → 转 Param → 调领域服务 → 转 DTO。
 * 这正是应用层该有的样子——编排而非实现。
 * 去重、乱序、计数三件事全部在领域层和仓储层完成。</p>
 */
@Service
public class SalesEventAppServiceImpl implements SalesEventAppService {

    private static final Logger log = LoggerFactory.getLogger(SalesEventAppServiceImpl.class);

    private final SalesWatermarkDomainService salesWatermarkDomainService;

    public SalesEventAppServiceImpl(SalesWatermarkDomainService salesWatermarkDomainService) {
        this.salesWatermarkDomainService = salesWatermarkDomainService;
    }

    @Override
    public ResultDO<AcceptSalesEventResponseDTO> acceptSalesEvent(AcceptSalesEventRequestDTO requestDTO) {
        ResultDO<Void> checkResult = requestDTO.check();
        if (!checkResult.isSuccess()) {
            return ResultDO.buildFailResult(checkResult.getCode(), checkResult.getMsg());
        }
        try {
            AcceptSalesEventParam param = SalesEventAssembler.toParam(requestDTO);

            ResultDO<AcceptSalesEventResult> acceptResult =
                    salesWatermarkDomainService.acceptEvent(param);
            if (!acceptResult.isSuccess()) {
                return ResultDO.buildFailResult(acceptResult.getCode(), acceptResult.getMsg());
            }

            // 顺带回一个 24 小时销量，方便调用方观测（也便于联调时确认事件真的进去了）
            ResultDO<Integer> windowResult = salesWatermarkDomainService.queryWindowSales(
                    requestDTO.getStyleId(), WindowSpec.SHORT_WINDOW_DAYS, System.currentTimeMillis());

            return ResultDO.buildSuccessResult(SalesEventAssembler.toResponseDTO(
                    acceptResult.getData(), windowResult.isSuccess() ? windowResult.getData() : null));
        } catch (Throwable e) {
            log.error("销量事件接入异常, eventId={}", requestDTO.getEventId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}