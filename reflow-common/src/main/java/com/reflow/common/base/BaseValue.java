package com.reflow.common.base;

import java.io.Serializable;

/**
 * 值对象基类。
 *
 * <p>值对象 = 没有标识、不可变、按值相等。判断标准很简单：
 * 两个对象所有属性都一样，它们在业务上就是同一个东西（比如"尺码曲线"、"数量区间"）。</p>
 *
 * <p>为什么强调不可变：值对象会被多个聚合共享引用（如规则的两条都用同一个区间），
 * 一旦可变，A 改区间会悄悄影响 B，这类 bug 极难排查。所以值对象的方法只能是
 * 计算类/判断类，不能是修改类。</p>
 */
public abstract class BaseValue implements Serializable {

    private static final long serialVersionUID = 1L;
}