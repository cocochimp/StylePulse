package com.stylepulse.domain.capacity.service;

import com.stylepulse.common.exception.AggregateException;
import com.stylepulse.common.exception.BizException;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.capacity.model.param.ConfirmCapacityParam;
import com.stylepulse.domain.capacity.model.param.ReserveCapacityParam;
import com.stylepulse.domain.capacity.model.result.ReserveCapacityResult;
import com.stylepulse.domain.capacity.repository.CapacityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 产能领域服务实现。
 *
 * <p>这个类很短，因为它把脏活都交给了仓储：原子预占、hold 凭证、超时回收。
 * 领域服务在这里只做两件事——校验入参完整性、把仓储结果翻译成领域结果。
 * 这不是"贫血"，而是<b>职责本来就该在这</b>：并发控制是技术问题，不是业务规则。</p>
 */
@Service
public class CapacityDomainServiceImpl implements CapacityDomainService {

    private static final Logger log = LoggerFactory.getLogger(CapacityDomainServiceImpl.class);

    private final CapacityRepository capacityRepository;

    public CapacityDomainServiceImpl(CapacityRepository capacityRepository) {
        this.capacityRepository = capacityRepository;
    }

    @Override
    public ResultDO<ReserveCapacityResult> reserve(ReserveCapacityParam param) {
        try {
            if (param == null || param.getFactoryId() == null || param.getLineId() == null
                    || param.getBizDate() == null || param.getQty() == null || param.getQty() <= 0) {
                throw new BizException("PARAM_ERROR", "产能预占参数不完整");
            }
            return capacityRepository.tryReserve(param);
        } catch (AggregateException e) {
            log.warn("产能预占被拒绝, slot={}, msg={}", param.slotKey(), e.getMessage());
            return ResultDO.buildSuccessResult(ReserveCapacityResult.rejected(e.getMessage(), 0));
        } catch (BizException e) {
            log.warn("产能预占业务异常, msg={}", e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("产能预占系统异常, param={}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }

    @Override
    public ResultDO<Void> confirm(ConfirmCapacityParam param) {
        try {
            if (param == null || param.getHoldId() == null || param.getHoldId().isBlank()) {
                throw new BizException("PARAM_ERROR", "holdId 不能为空");
            }
            return capacityRepository.confirm(param.getHoldId());
        } catch (BizException e) {
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("产能确认异常, holdId={}", param == null ? null : param.getHoldId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }

    @Override
    public ResultDO<Integer> releaseExpired() {
        try {
            ResultDO<java.util.List<String>> expiredResult = capacityRepository.queryExpiredHoldIds(100);
            if (!expiredResult.isSuccess()) {
                return ResultDO.buildFailResult(expiredResult.getCode(), expiredResult.getMsg());
            }
            int released = 0;
            for (String holdId : expiredResult.getData()) {
                ResultDO<Void> result = capacityRepository.release(holdId);
                if (result.isSuccess()) {
                    released++;
                }
            }
            if (released > 0) {
                log.info("回收超时产能预占 {} 条", released);
            }
            return ResultDO.buildSuccessResult(released);
        } catch (Throwable e) {
            log.error("回收超时预占异常", e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "回收超时预占失败");
        }
    }

    @Override
    public ResultDO<Void> release(String holdId) {
        try {
            if (holdId == null || holdId.isBlank()) {
                throw new BizException("PARAM_ERROR", "holdId 不能为空");
            }
            return capacityRepository.release(holdId);
        } catch (BizException e) {
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("产能释放异常, holdId={}", holdId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}