package com.reflow.infrastructure.style.converter;

import com.reflow.domain.style.model.aggregate.StyleAggregate;
import com.reflow.domain.style.model.entity.SkuEntity;
import com.reflow.domain.style.model.value.SizeCurveValue;
import com.reflow.infrastructure.style.mysql.po.StylePO;
import com.reflow.infrastructure.style.mysql.po.StyleSkuPO;
import com.reflow.common.field.Field;
import com.reflow.model.style.PriceBandEnum;
import com.reflow.model.style.StageEnum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 款聚合 ↔ PO 转换器。
 *
 * <p>Converter 和 Assembler 的区别要分清：
 * <ul>
 *   <li><b>Converter</b>（基础设施层）：聚合 ↔ PO，是"技术转换"，处理枚举存字符串、日期格式这些脏活</li>
 *   <li><b>Assembler</b>（应用层）：DTO ↔ 领域对象，是"契约转换"，处理对外字段名稳定性</li>
 * </ul>
 * 混用会导致领域模型被 DTO 的重命名需求绑架。</p>
 *
 * <p>本类只做机械转换，<b>不允许出现业务判断</b>。看到 if 里写着"如果库存小于 10 就打折"，
 * 那它放错地方了。</p>
 */
public final class StyleConverter {

    private StyleConverter() {
    }

    /** PO → 聚合根（读） */
    public static StyleAggregate toAggregate(StylePO po, List<StyleSkuPO> skuPOs) {
        if (po == null) {
            return null;
        }
        StyleAggregate aggregate = new StyleAggregate();
        aggregate.setId(po.getId());
        aggregate.setStyleCode(po.getStyleCode());
        aggregate.setCategoryId(po.getCategoryId());
        aggregate.setPriceBand(po.getPriceBand() == null ? null : PriceBandEnum.ofCode(po.getPriceBand()));
        aggregate.setLaunchDate(po.getLaunchDate());
        aggregate.setFirstOrderQty(po.getFirstOrderQty());
        aggregate.setStage(po.getStage() == null ? null : StageEnum.valueOf(po.getStage()));
        aggregate.setVersion(po.getVersion());
        // 曲线字符串 -> 值对象；解析失败返回 null，拆量算法会自动降级
        aggregate.setSizeCurve(parseCurve(po.getSizeCurve()));

        List<SkuEntity> skus = new ArrayList<>();
        if (skuPOs != null) {
            for (StyleSkuPO skuPO : skuPOs) {
                skus.add(new SkuEntity(skuPO.getId(), skuPO.getColorCode(), skuPO.getSizeCode(),
                        skuPO.getAvailableStock(), skuPO.getRecentSales()));
            }
        }
        aggregate.restoreSkus(skus);
        return aggregate;
    }

    /** 聚合根 → PO（写） */
    public static StylePO toPO(StyleAggregate aggregate) {
        StylePO po = new StylePO();
        po.setId(aggregate.getId());
        po.setStyleCode(aggregate.getStyleCode());
        po.setCategoryId(aggregate.getCategoryId());
        po.setPriceBand(aggregate.getPriceBand() == null ? null : aggregate.getPriceBand().getCode());
        po.setLaunchDate(aggregate.getLaunchDate());
        po.setFirstOrderQty(aggregate.getFirstOrderQty());
        po.setStage(aggregate.getStage() == null ? null : aggregate.getStage().name());
        po.setVersion(aggregate.getVersion());
        return po;
    }

    public static List<StyleSkuPO> toSkuPOs(StyleAggregate aggregate) {
        List<StyleSkuPO> result = new ArrayList<>();
        for (SkuEntity sku : aggregate.getSkus()) {
            StyleSkuPO po = new StyleSkuPO();
            po.setId(sku.getId());
            po.setStyleId(aggregate.getId());
            po.setColorCode(sku.getColorCode());
            po.setSizeCode(sku.getSizeCode());
            po.setAvailableStock(sku.getAvailableStock() == null ? null : sku.getAvailableStock().get());
            po.setRecentSales(sku.getRecentSales() == null ? null : sku.getRecentSales().get());
            result.add(po);
        }
        return result;
    }

    /**
     * 解析尺码曲线字符串 "S:2,M:4,L:4,XL:2"。
     *
     * <p>解析失败返回 null 而不是抛异常：曲线缺失时拆量算法会退化为纯销量权重，
     * 属于"降级可接受"的故障。为了一条配置格式错误让整条诊断链路失败，不划算。</p>
     */
    public static SizeCurveValue parseCurve(String curveText) {
        if (curveText == null || curveText.isBlank()) {
            return null;
        }
        try {
            Map<String, Integer> weights = new LinkedHashMap<>();
            for (String pair : curveText.split(",")) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    weights.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                }
            }
            return weights.isEmpty() ? null : new SizeCurveValue(weights);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 反序列化用：把 PO 上的曲线文本转成值对象，供 Provider 使用 */
    public static SizeCurveValue curveOf(StylePO po) {
        return parseCurve(po.getSizeCurve());
    }

    /** 便于测试：构造一个未加载的 Field */
    public static <T> Field<T> unloaded() {
        return Field.empty();
    }
}