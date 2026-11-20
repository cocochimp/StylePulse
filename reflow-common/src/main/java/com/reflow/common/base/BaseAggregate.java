package com.reflow.common.base;

import java.io.Serializable;

/**
 * 聚合根基类。
 *
 * <p>聚合根是"一致性边界"：外部只能通过它的方法改内部状态，不能穿过它直接改实体。
 * 这条约束是 DDD 最容易被破坏的一条，也是最有价值的一条——它把"什么状态下允许做什么"
 * 收拢到一个类里，而不是散落在 5 个 Service 的 if 判断里。</p>
 *
 * @param <ID> 聚合根标识类型
 */
public abstract class BaseAggregate<ID> implements Serializable {

    private static final long serialVersionUID = 1L;

    private ID id;

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    /** 新建聚合根时判空用；已落库的聚合根 id 必然非空 */
    public boolean isNew() {
        return this.id == null;
    }
}