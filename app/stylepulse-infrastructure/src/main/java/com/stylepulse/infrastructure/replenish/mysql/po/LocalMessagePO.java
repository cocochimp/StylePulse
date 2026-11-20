package com.stylepulse.infrastructure.replenish.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 本地消息表 PO。
 *
 * <p>字段设计对应"至少投递一次"的三个要素：
 * <b>状态</b>（INIT/SENT/ACK）、<b>重试次数</b>（超过阈值转人工）、
 * <b>下次重试时间</b>（指数退避，避免失败时把下游打爆）。</p>
 */
@TableName("t_local_message")
public class LocalMessagePO {

    /** 消息状态：待投递 */
    public static final String STATUS_INIT = "INIT";
    /** 消息状态：已投递，等待对方确认 */
    public static final String STATUS_SENT = "SENT";
    /** 消息状态：对方已确认 */
    public static final String STATUS_ACK = "ACK";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String msgId;

    private String bizType;

    private Long bizId;

    private String payload;

    private String status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
}