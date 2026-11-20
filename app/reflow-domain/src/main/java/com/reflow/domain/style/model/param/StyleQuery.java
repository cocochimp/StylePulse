package com.reflow.domain.style.model.param;

import com.reflow.common.base.BaseParam;
import com.reflow.model.style.StageEnum;

import java.util.List;

/**
 * 款查询条件（写模式加载聚合根 / 读模式列表查询共用）。
 *
 * <p>命名用 Query 而不是 Param：它是"查询条件对象"，不是"方法入参"。
 * easy-DDD 对仓储查询的入参约定是 {@code XxxQuery}，与 {@code XxxParam} 区分开，
 * 便于一眼看出这次调用是"要对数据做什么"还是"要捞出什么"。</p>
 */
public class StyleQuery extends BaseParam {

    private static final long serialVersionUID = 1L;

    private Long styleId;

    /** 按阶段过滤，空表示不过滤 */
    private List<StageEnum> stages;

    /** 限制条数，用于巡检批量拉取 */
    private Integer limit;

    public static StyleQuery byId(Long styleId) {
        StyleQuery query = new StyleQuery();
        query.setStyleId(styleId);
        return query;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public List<StageEnum> getStages() {
        return stages;
    }

    public void setStages(List<StageEnum> stages) {
        this.stages = stages;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}