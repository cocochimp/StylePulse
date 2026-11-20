package com.reflow.common.exception;

/**
 * 聚合根/实体内部校验失败时抛出。
 *
 * <p>语义边界很关键：{@code AggregateException} 表示"对象当前状态不允许这个操作"，
 * 属于不变量被破坏；而 {@link BizException} 表示"业务规则不允许"，
 * 比如规则没命中、款不存在。</p>
 *
 * <p>为什么区分：前者说明是调用方的调用时机不对（通常是并发或状态机被绕过），
 * 后者是正常的业务分支。日志级别和告警策略不同，混在一起就没法运维。</p>
 */
public class AggregateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public AggregateException(String message) {
        this("AGGREGATE_ERROR", message);
    }

    public AggregateException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}