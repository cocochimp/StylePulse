package com.reflow.domain.replenish.model.param;

import com.reflow.domain.style.model.value.SizeCurveValue;

/**
 * 尺码曲线提供者（领域层定义的函数式接口）。
 *
 * <p>这是 easy-DDD 里"领域服务按需取外部数据"的标准解法：
 * 尺码曲线属于 style 上下文，replenish 上下文不该直接依赖它；
 * 于是领域层只声明"我需要一个能给我曲线的函数"，由应用层注入具体实现。</p>
 *
 * <p>好处有两个：一是领域层保持零外部依赖；二是可以按需懒加载——
 * 拆量算法在循环里逐个 SKU 要权重，不需要提前把所有曲线批量查出来。</p>
 */
@FunctionalInterface
public interface SizeCurveProvider {

    /**
     * 取款对应的尺码曲线。
     *
     * @return 曲线；返回 null 时拆量算法会退化为"均分 + 断码加权"
     */
    SizeCurveValue getSizeCurve(Long styleId);
}