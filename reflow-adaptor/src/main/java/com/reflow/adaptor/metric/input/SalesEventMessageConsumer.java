package com.reflow.adaptor.metric.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reflow.client.metric.SalesEventAppService;
import com.reflow.client.metric.req.AcceptSalesEventRequestDTO;
import com.reflow.client.metric.res.AcceptSalesEventResponseDTO;
import com.reflow.common.result.ResultDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 销量事件消费者（MQ 入口）。
 *
 * <p>本工程没有引入 RocketMQ 客户端，是为了让示例能在没有中间件的环境里跑起来；
 * 这个类就是"如果接了 MQ，消费者长什么样"的样子——把消息体解析成 DTO，
 * 然后交给 AppService，别的什么都不做。</p>
 *
 * <p>接入真实 MQ 时只需两处改动：
 * <ol>
 *   <li>类上换成 {@code @RocketMQMessageListener(topic = "reflow-sales", consumerGroup = "...")} 并实现
 *       {@code RocketMQListener<String>}</li>
 *   <li>保留 {@link #consume(String)} 的方法体不动</li>
 * </ol>
 * 业务代码一行都不用改——这是"入口与业务解耦"的直接收益。</p>
 *
 * <p>关于返回值语义：{@code accepted=false}（重复/迟到）应当正常 ACK，
 * 不能抛异常让 MQ 重投，否则重复消息会被反复投递，形成死循环。</p>
 */
@Component
public class SalesEventMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(SalesEventMessageConsumer.class);

    private final SalesEventAppService salesEventAppService;
    private final ObjectMapper objectMapper;

    public SalesEventMessageConsumer(SalesEventAppService salesEventAppService, ObjectMapper objectMapper) {
        this.salesEventAppService = salesEventAppService;
        this.objectMapper = objectMapper;
    }

    /**
     * 消费一条消息。
     *
     * @param payload JSON 消息体
     */
    public void consume(String payload) {
        try {
            AcceptSalesEventRequestDTO requestDTO =
                    objectMapper.readValue(payload, AcceptSalesEventRequestDTO.class);
            ResultDO<AcceptSalesEventResponseDTO> result = salesEventAppService.acceptSalesEvent(requestDTO);
            if (!result.isSuccess()) {
                // 系统级失败（Redis 不可用等）才需要重投，业务级"被丢弃"不需要
                log.error("销量事件处理失败, payload={}, msg={}", payload, result.getMsg());
                throw new IllegalStateException("销量事件处理失败: " + result.getMsg());
            }
            log.debug("销量事件已处理, eventId={}, accepted={}",
                    requestDTO.getEventId(),
                    result.getData() == null ? null : result.getData().isAccepted());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable e) {
            log.error("销量事件解析异常, payload={}", payload, e);
        }
    }
}