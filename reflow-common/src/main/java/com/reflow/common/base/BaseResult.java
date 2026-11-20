package com.reflow.common.base;

import java.io.Serializable;

/**
 * 领域计算结果基类。
 *
 * <p>和 {@link BaseParam} 对称：{@code XxxResult} 表示"领域算出来的东西"。
 * 它与 DTO 的区别是——Result 属于领域语言（字段名用业务词），DTO 属于对外契约（字段名要稳定、可序列化、不能变）。</p>
 *
 * <p>允许充血：Result 可以带判断/计算方法（如 {@code isFree()}、{@code isBalanced()}），
 * 这样调用方不必重复写判断逻辑。</p>
 */
public abstract class BaseResult implements Serializable {

    private static final long serialVersionUID = 1L;
}