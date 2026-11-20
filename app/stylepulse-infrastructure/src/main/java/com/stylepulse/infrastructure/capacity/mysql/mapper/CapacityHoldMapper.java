package com.stylepulse.infrastructure.capacity.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stylepulse.infrastructure.capacity.mysql.po.CapacityHoldPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产能预占凭证 Mapper。
 */
@Mapper
public interface CapacityHoldMapper extends BaseMapper<CapacityHoldPO> {
}