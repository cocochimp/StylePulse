package com.stylepulse.infrastructure.style.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stylepulse.infrastructure.style.mysql.po.StylePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 款表 Mapper。
 *
 * <p>只继承 BaseMapper，不写 XML：本例的 SQL 都很直白，
 * 用 MyBatis-Plus 的通用方法足够。复杂报表 SQL 才值得单独写 XML。
 * 这也是刻意示范——不要为了"看起来像 MyBatis"而给简单查询手写 XML。</p>
 */
@Mapper
public interface StyleMapper extends BaseMapper<StylePO> {
}