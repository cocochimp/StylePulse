package com.reflow.infrastructure.replenish.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reflow.infrastructure.replenish.mysql.po.ReplenishDecisionPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 决策单 Mapper。
 */
@Mapper
public interface ReplenishDecisionMapper extends BaseMapper<ReplenishDecisionPO> {
}