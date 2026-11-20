package com.reflow.infrastructure.style.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reflow.infrastructure.style.mysql.po.StyleSkuPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 款 SKU 表 Mapper。
 *
 * <p>注意它和 StyleMapper 是两个 Mapper、两张表，但在领域层是同一个聚合。
 * 这就是"聚合与表不同构"：一个聚合可以对应多张表，由 Repository 负责装配。</p>
 */
@Mapper
public interface StyleSkuMapper extends BaseMapper<StyleSkuPO> {
}