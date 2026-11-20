package com.stylepulse.client.metric;

import com.stylepulse.client.common.ApplicationCmdService;
import com.stylepulse.client.metric.req.AcceptSalesEventRequestDTO;
import com.stylepulse.client.metric.res.AcceptSalesEventResponseDTO;
import com.stylepulse.common.result.ResultDO;

/**
 * 销量事件接入应用服务（写模式）。
 *
 * <p>为什么归到"写模式"：它改变水位状态。虽然消费者通常不关心返回，
 * 但接口返回 ResultDO 仍然必要——落库失败要能被上游感知并重投。</p>
 */
public interface SalesEventAppService extends ApplicationCmdService {

    /**
     * 接收一条销量事件。
     *
     * <p>内部要处理三件事：去重（eventId）、乱序（eventTime 水位线）、分桶累加。
     * 这三件事都不该由消费者做——消费者只负责协议转换。
     */
    ResultDO<AcceptSalesEventResponseDTO> acceptSalesEvent(AcceptSalesEventRequestDTO requestDTO);
}