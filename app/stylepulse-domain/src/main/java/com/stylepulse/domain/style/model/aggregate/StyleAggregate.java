package com.stylepulse.domain.style.model.aggregate;

import com.stylepulse.common.base.BaseAggregate;
import com.stylepulse.common.exception.AggregateException;
import com.stylepulse.domain.style.model.entity.SkuEntity;
import com.stylepulse.domain.style.model.param.ChangeStageParam;
import com.stylepulse.domain.style.model.result.SkuSnapshotResult;
import com.stylepulse.domain.style.model.value.SizeCurveValue;
import com.stylepulse.model.style.PriceBandEnum;
import com.stylepulse.model.style.StageEnum;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 款聚合根（写模式）。
 *
 * <p>一致性边界：款的基本信息 + 它的颜色尺码矩阵。库存、阶段、首单量必须一起变，
 * 所以它们必须在一个聚合里；而返单决策单、产能槽都是别的聚合，只通过 ID 引用。</p>
 *
 * <p>为什么阶段流转写在这里而不是 Service：状态机的"谁能进谁不能进"是业务不变量，
 * 分散到多个 Service 后必然出现某个入口绕过校验。放在聚合根，任何改阶段都得过这一关。</p>
 */
public class StyleAggregate extends BaseAggregate<Long> {

    private static final long serialVersionUID = 1L;

    private String styleCode;

    private Long categoryId;

    private PriceBandEnum priceBand;

    /** 上架日期，用于算"上架天数"——返单规则的第一维条件 */
    private LocalDate launchDate;

    /** 首单量（100~200 件），动销率的分母 */
    private Integer firstOrderQty;

    private StageEnum stage;

    /** 乐观锁版本号：并发保存时只有版本匹配的更新才会生效 */
    private Integer version;

    private List<SkuEntity> skus = new ArrayList<>();

    /** 尺码曲线：拆量时按尺码分配权重用；允许为空，为空则退化为纯销量权重 */
    private SizeCurveValue sizeCurve;

    // ------------------------------------------------------------------
    // 计算类方法：不改变状态
    // ------------------------------------------------------------------

    /** 上架天数，用于规则匹配的 ageDays 维度；未来日期按 0 处理，避免负数进索引 */
    public int ageDays(LocalDate bizDate) {
        if (launchDate == null || bizDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(launchDate, bizDate);
        return days < 0 ? 0 : (int) days;
    }

    public int totalAvailableStock() {
        return skus.stream()
                .mapToInt(sku -> sku.getAvailableStock() == null ? 0 : sku.getAvailableStock().orElse(0))
                .sum();
    }

    public int totalRecentSales() {
        return skus.stream()
                .mapToInt(sku -> sku.getRecentSales() == null ? 0 : sku.getRecentSales().orElse(0))
                .sum();
    }

    /**
     * 断码率 = 断码 SKU 数 / 有销量的 SKU 数。
     *
     * <p>分母用"有销量的 SKU"而不是"全部 SKU"：没卖出过的颜色尺码断了不算断码，
     * 用全量做分母会让新款永远看起来断码率很低，掩盖真实问题。</p>
     */
    public double brokenSizeRate() {
        long demandSkus = skus.stream().filter(sku -> sku.getRecentSales() != null && sku.getRecentSales().orElse(0) > 0).count();
        if (demandSkus == 0) {
            return 0D;
        }
        long broken = skus.stream().filter(SkuEntity::isBroken).count();
        return (double) broken / demandSkus;
    }

    /** 对外输出快照，供拆量算法使用 */
    public List<SkuSnapshotResult> skuSnapshots() {
        List<SkuSnapshotResult> snapshots = new ArrayList<>(skus.size());
        for (SkuEntity sku : skus) {
            snapshots.add(new SkuSnapshotResult(
                    sku.getId(),
                    sku.getColorCode(),
                    sku.getSizeCode(),
                    sku.getAvailableStock() == null ? 0 : sku.getAvailableStock().orElse(0),
                    sku.getRecentSales() == null ? 0 : sku.getRecentSales().orElse(0)));
        }
        return Collections.unmodifiableList(snapshots);
    }

    // ------------------------------------------------------------------
    // 判断类方法：不改变状态
    // ------------------------------------------------------------------

    /** 当前阶段是否允许开启返单决策——这是防止"清仓款还被追单"的第一道闸 */
    public boolean canOpenDecision() {
        return stage != null && stage.canOpenDecision();
    }

    public boolean isActive() {
        return stage != null && stage.isActive();
    }

    // ------------------------------------------------------------------
    // 写操作：改变状态，必须先校验不变量
    // ------------------------------------------------------------------

    /**
     * 阶段流转。
     *
     * @throws AggregateException 目标为空或非法流转时抛出
     */
    public void changeStage(ChangeStageParam param) {
        if (param == null || param.getTargetStage() == null) {
            throw new AggregateException("PARAM_NULL", "目标阶段不能为空");
        }
        if (this.stage == null) {
            throw new AggregateException("STAGE_NULL", "款当前阶段未初始化");
        }
        if (!this.stage.canTransitTo(param.getTargetStage())) {
            throw new AggregateException("STAGE_TRANSIT_DENIED",
                    "不允许从 " + this.stage.getLabel() + " 流转到 " + param.getTargetStage().getLabel());
        }
        this.stage = param.getTargetStage();
        this.version = this.version == null ? 1 : this.version + 1;
    }

    /**
     * 重建聚合时装配 SKU 集合。
     *
     * <p>这是唯一一个"不是业务动作"的方法，仅供 Converter 使用。
     * 之所以允许它存在：聚合根由仓储从 PO 重建时，必须能整体装回内部实体；
     * 但业务代码不应调用它，所以命名和注释都要显式警示。</p>
     */
    public void restoreSkus(List<SkuEntity> skus) {
        this.skus = skus == null ? new ArrayList<>() : new ArrayList<>(skus);
    }

    // ------------------------------------------------------------------
    // 访问器
    // ------------------------------------------------------------------

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public PriceBandEnum getPriceBand() {
        return priceBand;
    }

    public void setPriceBand(PriceBandEnum priceBand) {
        this.priceBand = priceBand;
    }

    public LocalDate getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(LocalDate launchDate) {
        this.launchDate = launchDate;
    }

    public Integer getFirstOrderQty() {
        return firstOrderQty;
    }

    public void setFirstOrderQty(Integer firstOrderQty) {
        this.firstOrderQty = firstOrderQty;
    }

    public StageEnum getStage() {
        return stage;
    }

    public void setStage(StageEnum stage) {
        this.stage = stage;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<SkuEntity> getSkus() {
        return Collections.unmodifiableList(skus);
    }

    public SizeCurveValue getSizeCurve() {
        return sizeCurve;
    }

    public void setSizeCurve(SizeCurveValue sizeCurve) {
        this.sizeCurve = sizeCurve;
    }
}