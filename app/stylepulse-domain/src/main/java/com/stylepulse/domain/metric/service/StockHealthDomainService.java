package com.stylepulse.domain.metric.service;

import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.metric.model.param.StockHealthParam;
import com.stylepulse.domain.metric.model.result.StyleHealthResult;

/**
 * 库存健康度领域服务（纯计算模式）。
 *
 * <p>纯计算模式的三个特征在这里都能对上：
 * 无聚合根、无仓储、无状态。命名用动词开头（StockHealth 是名词化的领域词，
 * 但方法的动词性体现在 calculate 上）。</p>
 *
 * <p>为什么单独抽出来：这套公式要被"实时诊断"和"历史回放对账"两处复用。
 * 放进诊断 AppService 里，回放时就得复制一份，两份口径迟早会分叉。</p>
 */
public interface StockHealthDomainService {

    ResultDO<StyleHealthResult> calculate(StockHealthParam param);
}