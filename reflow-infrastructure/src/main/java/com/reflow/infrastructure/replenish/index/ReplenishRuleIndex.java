package com.reflow.infrastructure.replenish.index;

import com.reflow.domain.replenish.model.aggregate.ReplenishRuleAggregate;
import com.reflow.domain.replenish.model.param.MatchStyleParam;
import com.reflow.domain.replenish.model.value.QtyRangeValue;
import com.reflow.model.style.PriceBandEnum;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 返单规则倒排索引（T1 的核心实现）。
 *
 * <p><b>问题</b>：千级款 × 几百条规则，逐款逐规则判断属性 = 几十万次比较，
 * 巡检一轮就顶不住；扩到十万款直接不可行。</p>
 *
 * <p><b>思路</b>：把"款属性"倒过来建成"属性值 → 规则集合"的映射，
 * 每个集合用位图（{@link RoaringBitmap}）表示。查候选规则时，
 * 把这款命中的几个位图做 AND 交集，得到的就是少量候选，再做精确判断。</p>
 *
 * <p><b>为什么用位图而不是 HashSet</b>：
 * 位图交集是"按 64 位字做与运算"，几百条规则的位图只有几个 long，
 * 交集是纳秒级；HashSet 需要逐个元素比对，几条规则时差别不大，
 * 但位图还能做并集/差集，语义更完整。</p>
 *
 * <p><b>桶化的取舍</b>：区间条件（天数、DOS、售罄率）没法直接进位图，
 * 先离散成桶。桶比区间粗，所以会多召回，但绝不会漏召回——
 * 多召回的部分由 {@code matchStyle} 精确剔除。</p>
 */
public class ReplenishRuleIndex {

    /** "不限类目"的规则放在这个哨兵键下，查询时与具体类目做并集 */
    private static final int ALL_CATEGORY = -1;

    /** "不限价格带"同理 */
    private static final int ALL_PRICE_BAND = -1;

    /** 上架天数桶：[0-3][4-7][8-14][15-30][31-60][61+] */
    private static final int[][] AGE_BUCKETS = {
            {0, 3}, {4, 7}, {8, 14}, {15, 30}, {31, 60}, {61, Integer.MAX_VALUE}};

    /** 可售天数桶：[0-2][3-7][8-15][16-30][31+] */
    private static final int[][] DOS_BUCKETS = {
            {0, 2}, {3, 7}, {8, 15}, {16, 30}, {31, Integer.MAX_VALUE}};

    /** 动销率桶（万分比）：[0-20%][20-50%][50-80%][80%+] */
    private static final int[][] SELL_THROUGH_BUCKETS = {
            {0, 2000}, {2001, 5000}, {5001, 8000}, {8001, Integer.MAX_VALUE}};

    private final Map<Integer, RoaringBitmap> byCategory = new HashMap<>();
    private final Map<Integer, RoaringBitmap> byPriceBand = new HashMap<>();
    private final Map<Integer, RoaringBitmap> byAgeBucket = new HashMap<>();
    private final Map<Integer, RoaringBitmap> byDosBucket = new HashMap<>();
    private final Map<Integer, RoaringBitmap> bySellThroughBucket = new HashMap<>();

    /** 规则 ID → 规则聚合，命中后直接取对象，不用回表 */
    private final Map<Integer, ReplenishRuleAggregate> rules = new LinkedHashMap<>();

    private ReplenishRuleIndex() {
    }

    public static ReplenishRuleIndex empty() {
        return new ReplenishRuleIndex();
    }

    /**
     * 建索引。规则数量很少（几十到几百），全量重建的成本可忽略，
     * 所以采用"整体重建 + 引用切换"的双缓冲策略，而不是增量更新。
     * 增量更新的收益抵不过它带来的"索引与规则不一致"的排查成本。
     */
    public static ReplenishRuleIndex build(List<ReplenishRuleAggregate> ruleList) {
        ReplenishRuleIndex index = new ReplenishRuleIndex();
        if (ruleList == null) {
            return index;
        }
        for (ReplenishRuleAggregate rule : ruleList) {
            if (!rule.isAvailable()) {
                continue;
            }
            int ruleId = toRuleId(rule.getId());
            index.rules.put(ruleId, rule);

            // 类目：空集合表示不限，挂到哨兵键下
            if (rule.getRuleEntity().getCategoryIds().isEmpty()) {
                index.byCategory.computeIfAbsent(ALL_CATEGORY, k -> new RoaringBitmap()).add(ruleId);
            } else {
                for (Long categoryId : rule.getRuleEntity().getCategoryIds()) {
                    index.byCategory.computeIfAbsent(categoryId.intValue(), k -> new RoaringBitmap()).add(ruleId);
                }
            }

            // 价格带：同上
            if (rule.getRuleEntity().getPriceBands().isEmpty()) {
                index.byPriceBand.computeIfAbsent(ALL_PRICE_BAND, k -> new RoaringBitmap()).add(ruleId);
            } else {
                for (PriceBandEnum band : rule.getRuleEntity().getPriceBands()) {
                    index.byPriceBand.computeIfAbsent(band.getCode(), k -> new RoaringBitmap()).add(ruleId);
                }
            }

            // 区间条件：投影到所有相交的桶
            index.projectRange(index.byAgeBucket, AGE_BUCKETS, rule.getRuleEntity().getAgeDayRange(), ruleId);
            index.projectRange(index.byDosBucket, DOS_BUCKETS, rule.getRuleEntity().getDosRange(), ruleId);
            index.projectRange(index.bySellThroughBucket, SELL_THROUGH_BUCKETS,
                    rule.getRuleEntity().getSellThroughRange(), ruleId);
        }
        return index;
    }

    /** 把一个区间投影到所有与之相交的桶上 */
    private void projectRange(Map<Integer, RoaringBitmap> target, int[][] buckets,
                              QtyRangeValue range, int ruleId) {
        if (range == null) {
            // 没有条件 = 不限，投到所有桶
            for (int i = 0; i < buckets.length; i++) {
                target.computeIfAbsent(i, k -> new RoaringBitmap()).add(ruleId);
            }
            return;
        }
        for (int i = 0; i < buckets.length; i++) {
            int bucketLow = buckets[i][0];
            int bucketHigh = buckets[i][1];
            boolean overlap = bucketLow <= range.getMax() && bucketHigh >= range.getMin();
            if (overlap) {
                target.computeIfAbsent(i, k -> new RoaringBitmap()).add(ruleId);
            }
        }
    }

    /**
     * 取候选规则（粗筛，可能多召回）。
     *
     * <p>某个维度缺失时直接跳过该维度，而不是返回空集——
     * "不知道类目"应该理解为"不按类目过滤"，而不是"没有规则适用"。</p>
     */
    public List<ReplenishRuleAggregate> candidates(MatchStyleParam param) {
        if (param == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        RoaringBitmap result = null;
        result = intersect(result, categoryBitmap(param.getCategoryId()));
        result = intersect(result, priceBandBitmap(param.getPriceBand()));

        // 区间维度必须区分两种"空"：
        //   ① 参数没给这个维度  -> 跳过该维度（不按它过滤）
        //   ② 参数给了，但该桶里一条规则都没有 -> 候选集直接为空
        // 如果统一按"跳过"处理，就会把明显不匹配的款也放进来，
        // 倒排索引就退化成"全表遍历"了。
        RoaringBitmap ageBitmap = param.getAgeDays() == null
                ? null : byAgeBucket.get(bucketOfAge(param.getAgeDays()));
        if (param.getAgeDays() != null && ageBitmap == null) {
            return Collections.emptyList();
        }
        result = intersect(result, ageBitmap);

        RoaringBitmap dosBitmap = param.getDos() == null
                ? null : byDosBucket.get(bucketOfDos(param.getDos()));
        if (param.getDos() != null && dosBitmap == null) {
            return Collections.emptyList();
        }
        result = intersect(result, dosBitmap);

        RoaringBitmap sellThroughBitmap = param.getSellThroughRate() == null
                ? null : bySellThroughBucket.get(bucketOfSellThrough(param.getSellThroughRate()));
        if (param.getSellThroughRate() != null && sellThroughBitmap == null) {
            return Collections.emptyList();
        }
        result = intersect(result, sellThroughBitmap);

        if (result == null) {
            return new ArrayList<>(rules.values());
        }

        List<ReplenishRuleAggregate> candidates = new ArrayList<>(result.getCardinality());
        for (int ruleId : result) {
            ReplenishRuleAggregate rule = rules.get(ruleId);
            if (rule != null) {
                candidates.add(rule);
            }
        }
        return candidates;
    }

    public List<ReplenishRuleAggregate> allRules() {
        return new ArrayList<>(rules.values());
    }

    public int size() {
        return rules.size();
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private RoaringBitmap categoryBitmap(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        RoaringBitmap specific = byCategory.get(categoryId.intValue());
        RoaringBitmap all = byCategory.get(ALL_CATEGORY);
        if (specific == null) {
            return all;
        }
        if (all == null) {
            return specific;
        }
        // 具体类目的规则 + 不限类目的规则
        RoaringBitmap union = RoaringBitmap.or(specific, all);
        return union;
    }

    private RoaringBitmap priceBandBitmap(PriceBandEnum band) {
        if (band == null) {
            return null;
        }
        RoaringBitmap specific = byPriceBand.get(band.getCode());
        RoaringBitmap all = byPriceBand.get(ALL_PRICE_BAND);
        if (specific == null) {
            return all;
        }
        if (all == null) {
            return specific;
        }
        return RoaringBitmap.or(specific, all);
    }

    /** 第一个维度直接作为初始集合；缺失维度跳过 */
    private RoaringBitmap intersect(RoaringBitmap current, RoaringBitmap next) {
        if (next == null) {
            return current;
        }
        if (current == null) {
            return RoaringBitmap.or(next, next);
        }
        return RoaringBitmap.and(current, next);
    }

    static int bucketOfAge(Integer ageDays) {
        int days = ageDays == null ? 0 : ageDays;
        return bucketOf(AGE_BUCKETS, days);
    }

    static int bucketOfDos(Double dos) {
        int value = dos == null ? 0 : (int) Math.round(dos);
        return bucketOf(DOS_BUCKETS, value);
    }

    static int bucketOfSellThrough(Double sellThroughRate) {
        int bp = sellThroughRate == null ? 0 : (int) Math.round(sellThroughRate * 10000);
        return bucketOf(SELL_THROUGH_BUCKETS, bp);
    }

    private static int bucketOf(int[][] buckets, int value) {
        for (int i = 0; i < buckets.length; i++) {
            if (value >= buckets[i][0] && value <= buckets[i][1]) {
                return i;
            }
        }
        // 超出所有桶（负数等异常值）归到第一个桶，保证不丢规则
        return 0;
    }

    /** RoaringBitmap 只能存 int，规则 ID 用 int 足够（几百条规则） */
    private static int toRuleId(Long ruleId) {
        return ruleId == null ? -2 : ruleId.intValue();
    }
}