package com.reflow.client.common;

/**
 * 读模式应用服务标记接口。
 *
 * <p>与 {@link ApplicationCmdService} 对称。分离的价值在于：
 * 读接口天然可以被缓存、被降级、被限流，写接口不行；
 * 类型上区分开，后续加治理手段时才能精准作用于一类接口。</p>
 */
public interface ApplicationQueryService {
}