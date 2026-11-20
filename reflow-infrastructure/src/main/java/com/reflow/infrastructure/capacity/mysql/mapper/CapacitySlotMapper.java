package com.reflow.infrastructure.capacity.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reflow.infrastructure.capacity.mysql.po.CapacitySlotPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产能槽 Mapper。
 */
@Mapper
public interface CapacitySlotMapper extends BaseMapper<CapacitySlotPO> {
}