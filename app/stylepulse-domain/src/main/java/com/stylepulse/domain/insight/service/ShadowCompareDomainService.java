package com.stylepulse.domain.insight.service;

import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.insight.model.param.ShadowCompareParam;
import com.stylepulse.domain.insight.model.result.ShadowCompareResult;

/**
 * 影子对比领域服务（纯计算模式）。
 *
 * <p>看起来它只做一次减法，做成独立服务有点小题大做。但它被两个地方复用：
 * 决策时的实时对比、每日批量的规则调参报告。而且"多大差异算异常"这条口径
 * 必须只有一份——否则实时和报告会给出不同结论，运营就不知道该信哪个。</p>
 */
public interface ShadowCompareDomainService {

    ResultDO<ShadowCompareResult> compare(ShadowCompareParam param);
}