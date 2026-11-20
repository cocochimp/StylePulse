package com.stylepulse.domain.replenish.model.value;

import com.stylepulse.common.base.BaseValue;

/**
 * 整数区间（值对象）。
 *
 * <p>返单规则的条件几乎都是区间：上架天数 0~7、DOS 3~10、售罄率 60%~100%。
 * 如果每个条件都拆成 min/max 两个裸字段，规则实体会有 10 个字段，
 * 匹配逻辑也会退化成一堆 {@code a >= min && a <= max}。</p>
 *
 * <p>封装成值对象后，条件变成"这个点是否落在区间内"，位图索引也能直接复用同一套语义。</p>
 */
public class QtyRangeValue extends BaseValue {

    private static final long serialVersionUID = 1L;

    private final int min;
    private final int max;

    private QtyRangeValue(int min, int max) {
        this.min = min;
        this.max = max;
    }

    /** 全开区间：[Integer.MIN_VALUE, Integer.MAX_VALUE]，用于"不限"的条件 */
    public static QtyRangeValue all() {
        return new QtyRangeValue(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static QtyRangeValue of(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("区间下界不能大于上界: [" + min + ", " + max + "]");
        }
        return new QtyRangeValue(min, max);
    }

    /** 闭区间判定 */
    public boolean contains(int value) {
        return value >= min && value <= max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public String describe() {
        return "[" + min + ", " + max + "]";
    }
}