package com.stylepulse.infrastructure.replenish.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stylepulse.infrastructure.replenish.mysql.po.ReplenishRulePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则表 Mapper。
 */
@Mapper
public interface ReplenishRuleMapper extends BaseMapper<ReplenishRulePO> {
}