package com.stylepulse.common.base;

import java.io.Serializable;

/**
 * 领域方法入参基类。
 *
 * <p>约定：所有 DomainService / 聚合根方法的入参都叫 {@code XxxParam} 并继承本类。
 * 目的是让"领域方法的入参"在类型系统上可识别——评审时看到 Param 就知道它穿过领域边界。</p>
 *
 * <p>为什么不用 Map：Map 让调用方和实现方对字段名只有口头约定，
 * 重构时编译器帮不上忙，这是领域层最不该有的脆弱点。</p>
 */
public abstract class BaseParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 操作人，便于审计；领域层不关心它怎么来的，只负责往下传 */
    private String operator;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}