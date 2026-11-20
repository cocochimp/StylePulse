package com.reflow.domain.replenish.service;

import com.reflow.common.exception.AggregateException;
import com.reflow.common.exception.BizException;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.param.SkuAllocItemParam;
import com.reflow.domain.replenish.model.param.SizeAllocationParam;
import com.reflow.domain.replenish.model.result.SizeAllocationResult;
import com.reflow.domain.style.model.value.SizeCurveValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 尺码拆量实现：最大余额法 + MOQ 收敛。
 *
 * <p>为什么不用简单按比例取整：{@code round(600 × 0.33) = 198}，
 * 六个尺码各取整一次后总额可能变成 601 或 599。总额不守恒在生产环节是致命的——
 * 工厂按 SKU 生产，总量对不上意味着必然有某个尺码多产或少产。</p>
 *
 * <p>最大余额法是唯一同时满足"整数"和"守恒"的简单算法：
 * 先向下取整，再把剩下的零头按小数部分从大到小逐个 +1。</p>
 *
 * <p>之后还有一次 MOQ 收敛：单 SKU 分配量小于起订量时，这个 SKU 根本下不了单，
 * 必须把它的量让给别的尺码，否则会出现"计划追 600、实际只能生产 480"的落差。</p>
 */
@Service
public class SizeAllocationDomainServiceImpl implements SizeAllocationDomainService {

    private static final Logger log = LoggerFactory.getLogger(SizeAllocationDomainServiceImpl.class);

    /** 断码 SKU 的权重放大倍数：断得越狠，越该优先补 */
    private static final double BROKEN_BOOST = 2.0D;

    /** 没有历史销量时给一个很小的权重，避免它完全分不到货 */
    private static final double NO_SALES_WEIGHT = 0.5D;

    @Override
    public ResultDO<SizeAllocationResult> allocate(SizeAllocationParam param) {
        try {
            if (param == null || param.getTotalQty() == null || param.getTotalQty() <= 0) {
                throw new BizException("PARAM_ERROR", "拆量总量必须大于 0");
            }
            List<SkuAllocItemParam> skus = param.getSkus();
            if (skus == null || skus.isEmpty()) {
                throw new BizException("PARAM_ERROR", "没有可拆分的 SKU");
            }

            int totalQty = param.getTotalQty();
            int moq = param.getMoq() == null || param.getMoq() <= 0 ? 1 : param.getMoq();
            int size = skus.size();

            // ---------- 1. 计算权重 ----------
            SizeCurveValue curve = loadCurve(param);
            double[] weights = new double[size];
            double sumWeight = 0D;
            for (int i = 0; i < size; i++) {
                SkuAllocItemParam sku = skus.get(i);
                int sales = sku.getRecentSales() == null ? 0 : Math.max(0, sku.getRecentSales());
                int stock = sku.getAvailableStock() == null ? 0 : Math.max(0, sku.getAvailableStock());

                double base = sales > 0 ? sales : NO_SALES_WEIGHT;
                // 断码（有需求但库存为 0）应优先补，这是降低断码率最直接的手段
                double boost = (stock <= 0 && sales > 0) ? BROKEN_BOOST : 1.0D;
                // 尺码曲线是品类经验值，和历史销量相乘后能同时照顾"卖得好"和"该尺码本来就该多"
                double curveWeight = curve == null ? 1.0D : curve.weightOf(sku.getSizeCode());

                weights[i] = base * boost * curveWeight;
                sumWeight += weights[i];
            }
            if (sumWeight <= 0) {
                // 全部无销量：退化为均分，至少保证能下单
                for (int i = 0; i < size; i++) {
                    weights[i] = 1.0D;
                }
                sumWeight = size;
            }

            // ---------- 2. 最大余额法初分配 ----------
            int[] alloc = new int[size];
            double[] fractions = new double[size];
            int assigned = 0;
            for (int i = 0; i < size; i++) {
                double exact = totalQty * weights[i] / sumWeight;
                alloc[i] = (int) Math.floor(exact);
                fractions[i] = exact - alloc[i];
                assigned += alloc[i];
            }

            int remainder = totalQty - assigned;
            if (remainder > 0) {
                // 按小数部分降序补零头；小数相同时按 skuId 升序，保证结果可复现
                Integer[] order = new Integer[size];
                for (int i = 0; i < size; i++) {
                    order[i] = i;
                }
                java.util.Arrays.sort(order, Comparator
                        .comparingDouble((Integer i) -> fractions[i]).reversed()
                        .thenComparingLong(i -> skus.get(i).getSkuId()));

                for (int k = 0; k < remainder; k++) {
                    alloc[order[k % size]]++;
                }
            }

            // ---------- 3. MOQ 收敛 ----------
            int released = 0;
            for (int i = 0; i < size; i++) {
                if (alloc[i] > 0 && alloc[i] < moq) {
                    released += alloc[i];
                    alloc[i] = 0;
                }
            }

            boolean belowMoq = false;
            if (released > 0) {
                List<Integer> candidates = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    if (alloc[i] >= moq) {
                        candidates.add(i);
                    }
                }
                if (candidates.isEmpty()) {
                    // 没有任何 SKU 能达到起订量：整款不追，交由上层按"低于 MOQ 放弃"处理
                    belowMoq = true;
                    log.info("拆量低于 MOQ 整款放弃, styleId={}, totalQty={}, moq={}",
                            param.getStyleId(), totalQty, moq);
                    return ResultDO.buildSuccessResult(SizeAllocationResult.empty());
                }
                // 候选按权重降序，把释放出来的量优先补给"最该补"的尺码
                candidates.sort(Comparator.comparingDouble((Integer i) -> weights[i]).reversed());
                for (int k = 0; k < released; k++) {
                    alloc[candidates.get(k % candidates.size())]++;
                }
            }

            // ---------- 4. 守恒校验 ----------
            int sum = 0;
            List<SizeAllocationResult.Item> items = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                sum += alloc[i];
                if (alloc[i] > 0) {
                    SkuAllocItemParam sku = skus.get(i);
                    items.add(new SizeAllocationResult.Item(
                            sku.getSkuId(), sku.getColorCode(), sku.getSizeCode(), alloc[i]));
                }
            }
            if (sum != totalQty) {
                // 走到这里说明算法有 bug，宁可失败也不能把不守恒的单放出去
                throw new AggregateException("ALLOC_NOT_BALANCED",
                        "拆量结果不守恒: 合计 " + sum + " != 总量 " + totalQty);
            }

            return ResultDO.buildSuccessResult(new SizeAllocationResult(items, sum, belowMoq));

        } catch (AggregateException e) {
            log.error("拆量不变量被破坏, param={}, msg={}", param, e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (BizException e) {
            log.warn("拆量业务异常, msg={}", e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("拆量系统异常, param={}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }

    /** 曲线可能取不到（新品类还没配），取不到就退化为纯销量权重 */
    private SizeCurveValue loadCurve(SizeAllocationParam param) {
        if (param.getCurveProvider() == null) {
            return null;
        }
        try {
            return param.getCurveProvider().getSizeCurve(param.getStyleId());
        } catch (RuntimeException e) {
            log.warn("尺码曲线获取失败，降级为无曲线, styleId={}", param.getStyleId());
            return null;
        }
    }
}