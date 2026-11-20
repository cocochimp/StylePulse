package com.reflow.common.base;

import com.reflow.common.field.Field;

import java.io.Serializable;

/**
 * 实体基类。
 *
 * <p>实体 = 有唯一标识、有生命周期、可变的领域对象。它只存在于聚合内部，
 * 外部拿到它必须经过聚合根（{@code aggregate.findXxx()}），不能自己 new 一个改完塞回去。</p>
 *
 * <p>注意：实体属性使用 {@link Field} 包装，而不是裸类型。
 * 这样"字段是 null（未加载）"和"字段是 0（值为零）"就能区分开，
 * 避免仓储层做部分更新时把未加载的字段覆盖成 null。</p>
 *
 * @param <ID> 实体标识类型
 */
public abstract class BaseEntity<ID> implements Serializable {

    private static final long serialVersionUID = 1L;

    private ID id;

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }
}