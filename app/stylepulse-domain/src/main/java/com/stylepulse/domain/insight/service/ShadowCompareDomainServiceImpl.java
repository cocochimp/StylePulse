package com.stylepulse.domain.insight.service;

import com.stylepulse.common.exception.BizException;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.insight.model.param.ShadowCompareParam;
import com.stylepulse.domain.insight.model.result.ShadowCompareResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 影子对比实现。
 */
@Service
public class ShadowCompareDomainServiceImpl implements ShadowCompareDomainService {

    private static final Logger log = LoggerFactory.getLogger(ShadowCompareDomainServiceImpl.class);

    @Override
    public ResultDO<ShadowCompareResult> compare(ShadowCompareParam param) {
        try {
            if (param == null || param.getLiveQty() == null || param.getShadowQty() == null) {
                throw new BizException("PARAM_ERROR", "对比双方的数量都不能为空");
            }

            int live = param.getLiveQty();
            int shadow = param.getShadowQty();
            int diff = shadow - live;

            // 分母用线上量；线上为 0 时用影子量兜底，避免除零
            int denominator = live != 0 ? Math.abs(live) : Math.abs(shadow);
            double rate = denominator == 0 ? 0D : (double) diff / denominator;

            String verdict;
            if (diff == 0) {
                verdict = "SAME";
            } else if (diff > 0) {
                verdict = "SHADOW_HIGHER";
            } else {
                verdict = "SHADOW_LOWER";
            }

            return ResultDO.buildSuccessResult(new ShadowCompareResult(diff, rate, verdict));
        } catch (BizException e) {
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("影子对比异常, param={}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}