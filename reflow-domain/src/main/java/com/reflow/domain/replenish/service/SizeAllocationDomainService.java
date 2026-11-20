package com.reflow.domain.replenish.service;

import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.param.SizeAllocationParam;
import com.reflow.domain.replenish.model.result.SizeAllocationResult;

/**
 * 尺码矩阵拆量领域服务（纯计算模式）。
 *
 * <p>它回答的是：款级总量 600 件，怎么落到 颜色×尺码 上——
 * 既要总量守恒、又要整数、又要贴合尺码曲线、还要满足单 SKU 起订量。</p>
 */
public interface SizeAllocationDomainService {

    ResultDO<SizeAllocationResult> allocate(SizeAllocationParam param);
}