package com.stylepulse.infrastructure.replenish.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stylepulse.infrastructure.replenish.mysql.po.LocalMessagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表 Mapper。
 */
@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessagePO> {
}