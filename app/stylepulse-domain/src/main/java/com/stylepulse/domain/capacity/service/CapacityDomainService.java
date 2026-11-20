package com.stylepulse.domain.capacity.service;

import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.capacity.model.param.ConfirmCapacityParam;
import com.stylepulse.domain.capacity.model.param.ReserveCapacityParam;
import com.stylepulse.domain.capacity.model.result.ReserveCapacityResult;

/**
 * 产能领域服务（写模式）。
 */
public interface CapacityDomainService {

    ResultDO<ReserveCapacityResult> reserve(ReserveCapacityParam param);

    ResultDO<Void> confirm(ConfirmCapacityParam param);

    ResultDO<Void> release(String holdId);

    /** 回收所有超时未确认的预占，返回回收条数 */
    ResultDO<Integer> releaseExpired();
}