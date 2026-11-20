package com.reflow.domain.style.model.value;

import com.reflow.common.base.BaseValue;
import com.reflow.common.exception.AggregateException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 尺码曲线（值对象）。
 *
 * <p>服装 SKU 的销量不是均匀分布在尺码上的：女装常见 M/L 走量、S 和 XL 次之、XS 最少。
 * 把这条"经验"显式建模成值对象，追单拆量时才有依据——
 * 按总量平均分是断码率居高不下的直接原因。</p>
 *
 * <p>不可变：构造时复制入参并冻结，任何"改曲线"的诉求都应当产生一个新对象。
 * 这样规则聚合和款聚合可以共享同一个曲线实例，不会互相污染。</p>
 */
public class SizeCurveValue extends BaseValue {

    private static final long serialVersionUID = 1L;

    /** 尺码 → 权重。用 LinkedHashMap 保持声明顺序，便于日志按尺码可读输出 */
    private final Map<String, Integer> weights;

    public SizeCurveValue(Map<String, Integer> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new AggregateException("尺码曲线不能为空");
        }
        this.weights = Collections.unmodifiableMap(new LinkedHashMap<>(weights));
    }

    /** 常用默认曲线：符合大多数女装品类的经验分布 */
    public static SizeCurveValue defaultCurve() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("S", 2);
        map.put("M", 4);
        map.put("L", 4);
        map.put("XL", 2);
        return new SizeCurveValue(map);
    }

    /**
     * 取某个尺码的权重。未声明的尺码返回 1——即"按普通尺码对待"，
     * 而不是返回 0：返回 0 会让这个尺码永远分不到货，属于静默丢数据。
     */
    public int weightOf(String sizeCode) {
        return weights.getOrDefault(sizeCode, 1);
    }

    public Map<String, Integer> getWeights() {
        return weights;
    }

    /** 权重总和，供调用方做归一化 */
    public int totalWeight() {
        return weights.values().stream().mapToInt(Integer::intValue).sum();
    }
}