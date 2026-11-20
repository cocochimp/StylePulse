package com.reflow.application.replenish.port;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 下发给工厂的生产指令。
 *
 * <p>它是应用层定义的"出参契约"：应用层知道自己要下什么，
 * 但不知道也不关心工厂是 HTTP 接口、MES 系统还是人工微信群。
 * 具体实现放在 adaptor/output，由它把这个对象翻译成对方的协议。</p>
 */
public class FactoryOrderCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long decisionId;
    private Long styleId;
    private String styleCode;
    private Long factoryId;
    private Long lineId;
    private String bizDate;
    private Integer totalQty;
    private List<SizeLine> lines = new ArrayList<>();

    /** 尺码行 */
    public static class SizeLine implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String colorCode;
        private final String sizeCode;
        private final int qty;

        public SizeLine(String colorCode, String sizeCode, int qty) {
            this.colorCode = colorCode;
            this.sizeCode = sizeCode;
            this.qty = qty;
        }

        public String getColorCode() {
            return colorCode;
        }

        public String getSizeCode() {
            return sizeCode;
        }

        public int getQty() {
            return qty;
        }
    }

    /** 便于日志与消息体展示：把指令压成一行文本 */
    public String toPayload() {
        StringBuilder sb = new StringBuilder();
        sb.append("decisionId=").append(decisionId)
                .append(";styleId=").append(styleId)
                .append(";styleCode=").append(styleCode)
                .append(";factoryId=").append(factoryId)
                .append(";lineId=").append(lineId)
                .append(";bizDate=").append(bizDate)
                .append(";totalQty=").append(totalQty)
                .append(";lines=");
        for (SizeLine line : lines) {
            sb.append(line.getColorCode()).append('/').append(line.getSizeCode())
                    .append(':').append(line.getQty()).append(',');
        }
        return sb.toString();
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
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

    public Long getFactoryId() {
        return factoryId;
    }

    public void setFactoryId(Long factoryId) {
        this.factoryId = factoryId;
    }

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public String getBizDate() {
        return bizDate;
    }

    public void setBizDate(String bizDate) {
        this.bizDate = bizDate;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public List<SizeLine> getLines() {
        return lines;
    }

    public void setLines(List<SizeLine> lines) {
        this.lines = lines;
    }
}