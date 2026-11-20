package com.stylepulse.model.style;

/**
 * 价格带。
 *
 * <p>为什么把连续的价格离散成档：返单规则是按"价格带"配的（低价款周转快、可追得深；
 * 高价款宁可少追），而规则索引是位图，位图只能装整数编号，装不了价格区间。
 * 离散化是让区间条件可以进索引的必要代价。</p>
 *
 * <p>档位边界属于配置，真实项目里建议放配置表；这里硬编码是为了让示例能独立跑起来。</p>
 */
public enum PriceBandEnum {

    /** 低价带：≤ 5 美元，走量款 */
    LOW(1, "低价带"),

    /** 中价带：5 ~ 15 美元，主力款 */
    MID(2, "中价带"),

    /** 高价带：> 15 美元，利润款 */
    HIGH(3, "高价带");

    private final int code;

    private final String label;

    PriceBandEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PriceBandEnum ofCode(int code) {
        for (PriceBandEnum band : values()) {
            if (band.code == code) {
                return band;
            }
        }
        throw new IllegalArgumentException("未知价格带 code: " + code);
    }

    /**
     * 按单价（分为单位）判定价格带。
     *
     * <p>金额一律用整数分参与判断，不用 double——0.1 + 0.2 这类误差在
     * 边界判定上足以把一款从低价带划到中价带，进而选中不同规则。</p>
     */
    public static PriceBandEnum ofPriceCents(long priceCents) {
        if (priceCents <= 500) {
            return LOW;
        }
        if (priceCents <= 1500) {
            return MID;
        }
        return HIGH;
    }
}