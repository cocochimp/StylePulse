package com.stylepulse.common.base;

/**
 * 仓储基类接口。
 *
 * <p>所有领域仓储都继承它，作用只有一个：让"这是仓储"这件事在类型上可识别，
 * 便于评审和静态检查（比如可以用 ArchUnit 断言 domain.repository 包下的接口
 * 全部继承本接口）。</p>
 *
 * <p>仓储的职责边界：只管<b>本领域内</b>的持久化、缓存、发消息。
 * 调第三方 HTTP 不属于仓储，属于 Adaptor 的 output。</p>
 *
 * @param <A>  聚合根类型
 * @param <ID> 聚合根标识类型
 */
public interface AggregateRepository<A extends BaseAggregate<ID>, ID> {
}