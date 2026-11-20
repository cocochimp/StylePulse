package com.reflow.infrastructure.style.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

/**
 * 款表 PO（持久化对象）。
 *
 * <p>PO 和聚合根长得像，但职责完全不同：PO 只描述"这一行怎么写进表"，
 * 它是表结构的镜像，字段类型要迁就数据库（枚举存字符串、金额存十进制）。
 * 一旦把业务方法写进 PO，就等于把表结构钉死在业务逻辑里。</p>
 */
@TableName("t_style")
public class StylePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String styleCode;

    private Long categoryId;

    /** 价格带，存的是枚举的 code（整数），不是名称——名称会改，code 不会 */
    private Integer priceBand;

    private LocalDate launchDate;

    private Integer firstOrderQty;

    /** 阶段，存枚举名称，便于直接在数据库里肉眼排查 */
    private String stage;

    /** 乐观锁版本 */
    private Integer version;

    /** 尺码曲线，格式 S:2,M:4,L:4,XL:2；生产环境可换 JSON 或独立配置表 */
    private String sizeCurve;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getPriceBand() {
        return priceBand;
    }

    public void setPriceBand(Integer priceBand) {
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

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getSizeCurve() {
        return sizeCurve;
    }

    public void setSizeCurve(String sizeCurve) {
        this.sizeCurve = sizeCurve;
    }
}