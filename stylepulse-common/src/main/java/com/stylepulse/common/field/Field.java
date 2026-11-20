package com.stylepulse.common.field;

import java.io.Serializable;
import java.util.Objects;

/**
 * 单值字段包装。
 *
 * <p>解决的问题：实体从仓储加载时，可能只查了部分字段（比如列表页只查 id + 状态）。
 * 如果属性是裸 {@code Integer stock}，null 既可能表示"没查"，也可能表示"值为空"，
 * 保存时无法判断该不该写这一列，容易把有效数据覆盖成 null。</p>
 *
 * <p>包装后语义明确：{@code field == null} 表示"未加载"，{@code field.value == null}
 * 表示"已加载且值为空"。仓储保存时只写"已加载"的字段。</p>
 *
 * @param <T> 值类型
 */
public final class Field<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final T value;

    private Field(T value) {
        this.value = value;
    }

    public static <T> Field<T> of(T value) {
        return new Field<>(value);
    }

    public static <T> Field<T> empty() {
        return new Field<>(null);
    }

    public T get() {
        return value;
    }

    /** 已加载且非空 */
    public boolean isPresent() {
        return value != null;
    }

    /** 提供默认值，避免调用方到处写判空 */
    public T orElse(T defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Field)) {
            return false;
        }
        return Objects.equals(value, ((Field<?>) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value == null ? "Field{unloaded}" : "Field{" + value + '}';
    }
}