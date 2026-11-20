package com.stylepulse.application.replenish.port;

import com.stylepulse.common.result.ResultDO;

/**
 * 工厂下单出口（防腐层接口）。
 *
 * <p>定义在应用层、实现在 adaptor/output，这是依赖倒置的第二次应用：
 * 应用层说"我要下一张生产单"，adaptor 负责把它变成工厂能懂的东西。
 * 好处是换工厂、加工厂、工厂接口升级，都不动业务代码。</p>
 */
public interface FactoryOrderPort {

    ResultDO<Void> dispatch(FactoryOrderCommand command);
}