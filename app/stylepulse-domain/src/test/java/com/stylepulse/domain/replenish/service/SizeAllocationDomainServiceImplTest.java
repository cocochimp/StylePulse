package com.stylepulse.domain.replenish.service;

import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.replenish.model.param.SizeAllocationParam;
import com.stylepulse.domain.replenish.model.param.SkuAllocItemParam;
import com.stylepulse.domain.replenish.model.result.SizeAllocationResult;
import com.stylepulse.domain.style.model.value.SizeCurveValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 尺码拆量测试。
 *
 * <p>这个算法最容易出错的地方不是"算得准不准"，而是三个硬约束：
 * <b>总量守恒</b>、<b>整数</b>、<b>MOQ 下界</b>。所以测试重点放在约束上，
 * 而不是某个具体的分配数字（具体数字会随权重口径调整而变化）。</p>
 */
class SizeAllocationDomainServiceImplTest {

    private final SizeAllocationDomainService service = new SizeAllocationDomainServiceImpl();

    @Test
    @DisplayName("总量守恒：分配结果之和必须等于款级总量")
    void shouldKeepTotalBalanced() {
        SizeAllocationParam param = param(660, 100, skus(
                sku(1L, "S", 0, 90),
                sku(2L, "M", 30, 210),
                sku(3L, "L", 25, 200),
                sku(4L, "XL", 40, 50)));

        ResultDO<SizeAllocationResult> result = service.allocate(param);

        assertThat(result.isSuccess()).isTrue();
        int sum = result.getData().getItems().stream().mapToInt(SizeAllocationResult.Item::getQty).sum();
        assertThat(sum).isEqualTo(660);
        assertThat(result.getData().getTotalAllocated()).isEqualTo(660);
    }

    @Test
    @DisplayName("断码尺码应获得更高权重：库存为 0 且有销量的尺码分得更多")
    void shouldBoostBrokenSize() {
        // S 码库存 0 但卖了 90 件（断码），XL 卖了 50 件且库存充足
        SizeAllocationParam param = param(400, 50, skus(
                sku(1L, "S", 0, 90),
                sku(4L, "XL", 100, 50)));

        ResultDO<SizeAllocationResult> result = service.allocate(param);

        int sQty = qtyOf(result.getData(), "S");
        int xlQty = qtyOf(result.getData(), "XL");
        // S 的原始销量是 XL 的 1.8 倍，再叠加断码放大，分配量应显著高于 XL
        assertThat(sQty).isGreaterThan(xlQty);
    }

    @Test
    @DisplayName("MOQ 收敛：单尺码低于起订量时归零，量转给其他尺码")
    void shouldReleaseQtyBelowMoq() {
        // 总量 400、MOQ 100、3 个尺码：按权重本会分出很小的量，收敛后应只保留 >=100 的尺码
        SizeAllocationParam param = param(400, 100, skus(
                sku(1L, "S", 10, 10),
                sku(2L, "M", 10, 500),
                sku(3L, "L", 10, 500)));

        ResultDO<SizeAllocationResult> result = service.allocate(param);

        assertThat(result.isSuccess()).isTrue();
        for (SizeAllocationResult.Item item : result.getData().getItems()) {
            assertThat(item.getQty()).isGreaterThanOrEqualTo(100);
        }
        int sum = result.getData().getItems().stream().mapToInt(SizeAllocationResult.Item::getQty).sum();
        assertThat(sum).isEqualTo(400);
    }

    @Test
    @DisplayName("整款放弃：总量连一个 MOQ 都凑不出时返回 belowMoq")
    void shouldGiveUpWhenTotalBelowMoq() {
        SizeAllocationParam param = param(60, 100, skus(
                sku(1L, "S", 10, 20),
                sku(2L, "M", 10, 20)));

        ResultDO<SizeAllocationResult> result = service.allocate(param);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().isUsable()).isFalse();
        assertThat(result.getData().isBelowMoq()).isTrue();
    }

    @Test
    @DisplayName("尺码曲线生效：同等销量下，曲线权重高的尺码分得更多")
    void shouldFollowSizeCurve() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("S", 1);
        weights.put("M", 10);
        SizeCurveValue curve = new SizeCurveValue(weights);

        SizeAllocationParam param = param(400, 100, skus(
                sku(1L, "S", 0, 100),
                sku(2L, "M", 0, 100)));
        param.setCurveProvider(styleId -> curve);

        ResultDO<SizeAllocationResult> result = service.allocate(param);

        assertThat(qtyOf(result.getData(), "M")).isGreaterThan(qtyOf(result.getData(), "S"));
    }

    // ------------------------------------------------------------------

    private SizeAllocationParam param(int totalQty, int moq, List<SkuAllocItemParam> skus) {
        SizeAllocationParam param = new SizeAllocationParam();
        param.setStyleId(1L);
        param.setTotalQty(totalQty);
        param.setMoq(moq);
        param.setSkus(skus);
        return param;
    }

    private List<SkuAllocItemParam> skus(SkuAllocItemParam... items) {
        List<SkuAllocItemParam> list = new ArrayList<>();
        for (SkuAllocItemParam item : items) {
            list.add(item);
        }
        return list;
    }

    private SkuAllocItemParam sku(Long skuId, String size, int stock, int sales) {
        return new SkuAllocItemParam(skuId, "BLACK", size, stock, sales);
    }

    private int qtyOf(SizeAllocationResult result, String size) {
        return result.getItems().stream()
                .filter(item -> size.equals(item.getSizeCode()))
                .mapToInt(SizeAllocationResult.Item::getQty)
                .findFirst()
                .orElse(0);
    }
}