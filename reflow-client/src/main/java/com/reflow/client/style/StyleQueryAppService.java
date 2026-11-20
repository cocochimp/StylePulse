package com.reflow.client.style;

import com.reflow.client.common.ApplicationQueryService;
import com.reflow.client.style.req.QueryStyleDetailRequestDTO;
import com.reflow.client.style.res.QueryStyleDetailResponseDTO;
import com.reflow.common.result.ResultDO;

/**
 * 款查询应用服务（读模式）。
 *
 * <p>读模式刻意不经过领域服务：没有状态变更、没有业务规则，
 * 走 DomainService 只会多一层无意义转发。它直接查仓储，把聚合根当数据载体，
 * 由 Assembler 转成 DTO 返回。</p>
 */
public interface StyleQueryAppService extends ApplicationQueryService {

    ResultDO<QueryStyleDetailResponseDTO> queryStyleDetail(QueryStyleDetailRequestDTO requestDTO);
}