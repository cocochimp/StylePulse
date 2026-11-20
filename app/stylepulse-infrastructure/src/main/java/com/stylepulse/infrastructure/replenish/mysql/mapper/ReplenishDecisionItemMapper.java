package com.stylepulse.infrastructure.replenish.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stylepulse.infrastructure.replenish.mysql.po.ReplenishDecisionItemPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 决策单尺码行 Mapper。
 */
@Mapper
public interface ReplenishDecisionItemMapper extends BaseMapper<ReplenishDecisionItemPO> {
}