package com.stylepulse.client.style.res;

import java.util.ArrayList;
import java.util.List;

/**
 * 款详情响应（读模式出参）。
 *
 * <p>注意这里的字段全是"对外可稳定"的表达：阶段用 String 而不是 StageEnum。
 * 因为 client 不允许依赖 model，外部系统也不该被我们的枚举重构影响。
 * 枚举 → 字符串的转换由 Application 层的 Assembler 负责。</p>
 */
public class QueryStyleDetailResponseDTO {

    private Long styleId;
    private String styleCode;
    private Long categoryId;
    private String priceBand;
    private String launchDate;
    private Integer firstOrderQty;
    private String stage;
    private String stageLabel;
    private Integer version;
    private List<SkuLine> skus = new ArrayList<>();

    /** 颜色尺码行：读模式里它是数据载体，不带任何行为 */
    public static class SkuLine {
        private Long skuId;
        private String colorCode;
        private String sizeCode;
        private Integer availableStock;
        private Integer recentSales;

        public Long getSkuId() {
            return skuId;
        }

        public void setSkuId(Long skuId) {
            this.skuId = skuId;
        }

        public String getColorCode() {
            return colorCode;
        }

        public void setColorCode(String colorCode) {
            this.colorCode = colorCode;
        }

        public String getSizeCode() {
            return sizeCode;
        }

        public void setSizeCode(String sizeCode) {
            this.sizeCode = sizeCode;
        }

        public Integer getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(Integer availableStock) {
            this.availableStock = availableStock;
        }

        public Integer getRecentSales() {
            return recentSales;
        }

        public void setRecentSales(Integer recentSales) {
            this.recentSales = recentSales;
        }
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

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

    public String getPriceBand() {
        return priceBand;
    }

    public void setPriceBand(String priceBand) {
        this.priceBand = priceBand;
    }

    public String getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(String launchDate) {
        this.launchDate = launchDate;
    }

    public Integer getFirstOrderQty() {
        return firstOrderQty;
    }

    public void setFirstOrderQty(Integer firstOrderQty) {
        this.firstOrderQty = firstOrderQty;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getStageLabel() {
        return stageLabel;
    }

    public void setStageLabel(String stageLabel) {
        this.stageLabel = stageLabel;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<SkuLine> getSkus() {
        return skus;
    }

    public void setSkus(List<SkuLine> skus) {
        this.skus = skus;
    }
}