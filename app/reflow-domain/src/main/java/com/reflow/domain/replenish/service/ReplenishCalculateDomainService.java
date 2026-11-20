package com.reflow.domain.replenish.service;

import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.param.CalculateQtyParam;
import com.reflow.domain.replenish.model.result.CalculateQtyResult;

/**
 * 追单量计算领域服务（规则+计算模式）。
 *
 * <p>职责只有一条：<b>选出命中规则并让它算</b>。
 * 它不做计算（计算在规则实体里），也不碰数据库（规则由仓储给）。</p>
 */
public interface ReplenishCalculateDomainService {

    ResultDO<CalculateQtyResult> calculateQty(CalculateQtyParam param);
}