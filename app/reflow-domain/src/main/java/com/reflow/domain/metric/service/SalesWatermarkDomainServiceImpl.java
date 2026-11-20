package com.reflow.domain.metric.service;

import com.reflow.common.exception.BizException;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.metric.model.aggregate.SalesWatermarkAggregate;
import com.reflow.domain.metric.model.param.AcceptSalesEventParam;
import com.reflow.domain.metric.model.param.SalesWindowQuery;
import com.reflow.domain.metric.model.result.AcceptSalesEventResult;
import com.reflow.domain.metric.repository.SalesWatermarkRepository;
import com.reflow.model.metric.WindowSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 销量水位领域服务实现。
 *
 * <p>关键设计：<b>先推水位再加计数</b>。反过来的话，如果加完计数、推水位失败，
 * 重投时计数会被再加一遍——多算会直接导致多追单，代价比少算大得多。
 * 少算的部分由日批对账补偿，属于可修复误差。</p>
 */
@Service
public class SalesWatermarkDomainServiceImpl implements SalesWatermarkDomainService {

    private static final Logger log = LoggerFactory.getLogger(SalesWatermarkDomainServiceImpl.class);

    private final SalesWatermarkRepository salesWatermarkRepository;

    public SalesWatermarkDomainServiceImpl(SalesWatermarkRepository salesWatermarkRepository) {
        this.salesWatermarkRepository = salesWatermarkRepository;
    }

    @Override
    public ResultDO<AcceptSalesEventResult> acceptEvent(AcceptSalesEventParam param) {
        try {
            if (param == null || param.getStyleId() == null || param.getQty() == null) {
                throw new BizException("PARAM_ERROR", "销量事件缺少必要字段");
            }

            // 第一层：幂等去重。必须原子，否则并发下两个线程都会认为"首次"
            ResultDO<Boolean> markResult = salesWatermarkRepository.tryMarkEvent(param.getEventId());
            if (!markResult.isSuccess()) {
                return ResultDO.buildFailResult(markResult.getCode(), markResult.getMsg());
            }
            if (!Boolean.TRUE.equals(markResult.getData())) {
                log.debug("重复事件丢弃, eventId={}", param.getEventId());
                return ResultDO.buildSuccessResult(AcceptSalesEventResult.duplicated(null));
            }

            // 第二层：加载水位聚合根，由它判定是否乱序
            ResultDO<SalesWatermarkAggregate> queryResult = salesWatermarkRepository.query(param.getStyleId());
            if (!queryResult.isSuccess()) {
                return ResultDO.buildFailResult(queryResult.getCode(), queryResult.getMsg());
            }
            SalesWatermarkAggregate watermark = queryResult.getData();

            if (!watermark.advance(param)) {
                // 迟到事件：不推进水位、不计数，但记录下来供对账补偿
                salesWatermarkRepository.markLateEvent(param.getEventId(), param.getStyleId());
                log.debug("迟到事件暂存, eventId={}, eventTime={}, watermark={}",
                        param.getEventId(), param.getEventTimeMillis(), watermark.getLastEventTime());
                return ResultDO.buildSuccessResult(AcceptSalesEventResult.late(watermark.getLastEventTime()));
            }

            ResultDO<Void> saveResult = salesWatermarkRepository.save(watermark);
            if (!saveResult.isSuccess()) {
                return ResultDO.buildFailResult(saveResult.getCode(), saveResult.getMsg());
            }

            // 第三层：落到时间桶。bucketTs 由存储侧按粒度对齐，这里显式算出来是为了让日志可读
            long bucketTs = WindowSpec.bucketOf(param.getEventTimeMillis());
            ResultDO<Void> incrResult = salesWatermarkRepository.incrBucket(param.getStyleId(), bucketTs, param.getQty());
            if (!incrResult.isSuccess()) {
                return ResultDO.buildFailResult(incrResult.getCode(), incrResult.getMsg());
            }

            return ResultDO.buildSuccessResult(AcceptSalesEventResult.accepted(watermark.getLastEventTime()));

        } catch (BizException e) {
            log.warn("销量事件业务异常, param={}, msg={}", param, e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("销量事件系统异常, param={}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }

    @Override
    public ResultDO<Integer> queryWindowSales(Long styleId, int windowDays, long nowMillis) {
        try {
            if (styleId == null || windowDays <= 0) {
                throw new BizException("PARAM_ERROR", "styleId 或窗口天数非法");
            }
            long toBucket = WindowSpec.bucketOf(nowMillis);
            long fromBucket = WindowSpec.bucketOf(nowMillis - windowDays * 24L * 60 * 60 * 1000);
            SalesWindowQuery query = new SalesWindowQuery(styleId, fromBucket, toBucket);
            return salesWatermarkRepository.sumWindow(query);
        } catch (BizException e) {
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("窗口销量查询异常, styleId={}", styleId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}