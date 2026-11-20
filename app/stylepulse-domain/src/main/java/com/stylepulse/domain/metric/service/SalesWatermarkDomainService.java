package com.stylepulse.domain.metric.service;

import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.metric.model.param.AcceptSalesEventParam;
import com.stylepulse.domain.metric.model.result.AcceptSalesEventResult;

/**
 * 销量水位领域服务（写模式）。
 *
 * <p>它负责编排"去重 → 判乱序 → 推水位 → 累加计数"这条链路，
 * 每一步的原子性由仓储保证，业务判定由聚合根保证。
 * 这个分工很重要：如果把去重也放进聚合根，就变成了"先查后写"，并发下必然失效。</p>
 */
public interface SalesWatermarkDomainService {

    ResultDO<AcceptSalesEventResult> acceptEvent(AcceptSalesEventParam param);

    /** 查询某款近 N 天销量（供诊断使用） */
    ResultDO<Integer> queryWindowSales(Long styleId, int windowDays, long nowMillis);
}