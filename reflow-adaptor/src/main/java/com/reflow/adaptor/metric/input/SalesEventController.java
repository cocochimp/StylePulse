package com.reflow.adaptor.metric.input;

import com.reflow.client.metric.SalesEventAppService;
import com.reflow.client.metric.req.AcceptSalesEventRequestDTO;
import com.reflow.client.metric.res.AcceptSalesEventResponseDTO;
import com.reflow.common.result.ResultDO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 销量事件联调入口。
 *
 * <p>生产环境这条链路由 MQ 驱动（见 {@link SalesEventMessageConsumer}），
 * 这里额外开一个 HTTP 口是为了本地/联调时能直接灌事件、验证水位。
 * 两个入口调用的是<b>同一个</b> AppService——入口可以有多个，业务路径只能有一条。</p>
 */
@RestController
@RequestMapping("/api/events")
public class SalesEventController {

    private final SalesEventAppService salesEventAppService;

    public SalesEventController(SalesEventAppService salesEventAppService) {
        this.salesEventAppService = salesEventAppService;
    }

    @PostMapping("/sales")
    public ResultDO<AcceptSalesEventResponseDTO> acceptSalesEvent(
            @RequestBody AcceptSalesEventRequestDTO requestDTO) {
        return salesEventAppService.acceptSalesEvent(requestDTO);
    }
}