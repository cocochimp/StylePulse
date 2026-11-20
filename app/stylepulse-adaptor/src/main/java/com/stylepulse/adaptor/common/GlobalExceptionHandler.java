package com.stylepulse.adaptor.common;

import com.stylepulse.common.result.ResultDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常兜底。
 *
 * <p>存在的意义不是"处理异常"，而是"保证出口格式一致"：
 * 正常情况下所有接口都返回 {@link ResultDO}，如果某个异常漏到框架层，
 * 客户端会突然收到一个完全不同的错误结构，前端解析就会崩。
 * 这里把最后一道口子统一成同样的信封。</p>
 *
 * <p>注意：业务失败不应该走到这里——它们已经被转成 ResultDO 了。
 * 能走到这里的，都是"没预料到的异常"，所以要打 error 级别日志。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Throwable.class)
    public ResultDO<Void> handle(Throwable e) {
        log.error("未捕获异常", e);
        return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常，请稍后重试");
    }
}