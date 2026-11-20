package com.stylepulse.common.result;

import java.io.Serializable;

/**
 * 统一返回信封。
 *
 * <p>为什么放在 common 而不是 domain：client（对外契约）、domain、application、infrastructure
 * 四层都要返回它。如果放在 domain，client 就得依赖 domain，违反"对外契约不得暴露内部模型"的约束。
 * 所以它是技术基座的一部分，不是业务概念。</p>
 *
 * <p>为什么不用抛异常表达业务失败：调用方（AppService / Controller）需要把错误码
 * 原样透出给前端或上游系统，异常栈对这种"预期内的失败"没有价值，反而干扰日志。</p>
 *
 * @param <T> 业务数据类型
 */
public class ResultDO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功码。业务侧约定 0 为成功，非 0 均为失败 */
    public static final String SUCCESS_CODE = "0";

    private String code;
    private String msg;
    private T data;

    public ResultDO() {
    }

    private ResultDO(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ResultDO<T> buildSuccessResult() {
        return new ResultDO<>(SUCCESS_CODE, "success", null);
    }

    public static <T> ResultDO<T> buildSuccessResult(T data) {
        return new ResultDO<>(SUCCESS_CODE, "success", data);
    }

    public static <T> ResultDO<T> buildFailResult(String code, String msg) {
        return new ResultDO<>(code, msg, null);
    }

    /** 失败但需要带回数据时使用（例如批量处理的部分成功） */
    public static <T> ResultDO<T> buildFailResult(String code, String msg, T data) {
        return new ResultDO<>(code, msg, data);
    }

    public static <T> ResultDO<T> buildFailResult(String msg) {
        return new ResultDO<>("BIZ_ERROR", msg, null);
    }

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(this.code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ResultDO{code='" + code + "', msg='" + msg + "', data=" + data + '}';
    }
}