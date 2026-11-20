package com.stylepulse.client.common;

/**
 * 写模式应用服务标记接口。
 *
 * <p>作用不是"必须实现什么方法"，而是让"这是写场景"在类型上可识别：
 * 静态检查可以断言写服务不得调用读服务，评审时也能一眼分清 CQRS 的两侧。</p>
 *
 * <p>放在 client 而不是 application：接口是契约，实现是可替换的。
 * 外部系统依赖 client 就够了，不需要也不应该依赖 application 的实现。</p>
 */
public interface ApplicationCmdService {
}