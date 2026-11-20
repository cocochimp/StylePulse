package com.stylepulse.common.exception;

/**
 * 领域服务层业务校验失败时抛出。
 *
 * <p>只在 DomainService 里抛，由上层（AppService）捕获并转成 {@code ResultDO}。
 * 领域层不返回 ResultDO 之外的错误表达方式，避免"有的地方抛异常、有的地方返回错误码"
 * 这种两套体系并存的问题。</p>
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public BizException(String message) {
        this("BIZ_ERROR", message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}