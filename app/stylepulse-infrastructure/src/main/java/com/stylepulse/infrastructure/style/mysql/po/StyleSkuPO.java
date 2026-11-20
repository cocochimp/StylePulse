package com.stylepulse.infrastructure.style.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 款 SKU 表 PO（颜色 × 尺码矩阵的一行）。
 */
@TableName("t_style_sku")
public class StyleSkuPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long styleId;

    private String colorCode;

    private String sizeCode;

    private Integer availableStock;

    private Integer recentSales;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
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