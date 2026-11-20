package com.stylepulse.infrastructure.capacity.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 产能预占凭证 PO。
 *
 * <p>为什么预占要落库而不是只放 Redis：预占是"承诺"，承诺必须可审计、可追溯。
 * Redis 里的计数只是准入闸门，重启就没了；这张表才是真正"谁在什么时候占了多少"的依据，
 * 也是 Redis 与 DB 对账时的基准。</p>
 */
@TableName("t_capacity_hold")
public class CapacityHoldPO {

    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_RELEASED = "RELEASED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String holdId;

    private String slotKey;

    private Integer qty;

    private Long decisionId;

    private String status;

    /** 预占超时时间；确认后置空，表示不再回收 */
    private LocalDateTime expireAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHoldId() {
        return holdId;
    }

    public void setHoldId(String holdId) {
        this.holdId = holdId;
    }

    public String getSlotKey() {
        return slotKey;
    }

    public void setSlotKey(String slotKey) {
        this.slotKey = slotKey;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
}