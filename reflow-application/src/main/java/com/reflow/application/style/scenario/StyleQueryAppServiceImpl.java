package com.reflow.application.style.scenario;

import com.reflow.client.style.StyleQueryAppService;
import com.reflow.client.style.req.QueryStyleDetailRequestDTO;
import com.reflow.client.style.res.QueryStyleDetailResponseDTO;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.style.model.aggregate.StyleAggregate;
import com.reflow.domain.style.model.param.StyleQuery;
import com.reflow.domain.style.repository.StyleRepository;
import com.reflow.application.style.assembler.StyleAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 款查询应用服务（读模式的完整骨架）。
 *
 * <p>读模式刻意不走 DomainService：没有状态变更、没有业务规则，
 * 再套一层领域服务只是多一次转发。它的调用链是：
 * <pre>Controller → QueryAppService → Repository → Assembler → DTO</pre>
 *
 * <p>注意方法名是 {@code queryXxx} 而不是 {@code getXxx}：
 * easy-DDD 要求查询方法用动词明确意图，"get" 太泛，看不出是查详情还是查列表。</p>
 */
@Service
public class StyleQueryAppServiceImpl implements StyleQueryAppService {

    private static final Logger log = LoggerFactory.getLogger(StyleQueryAppServiceImpl.class);

    private final StyleRepository styleRepository;

    public StyleQueryAppServiceImpl(StyleRepository styleRepository) {
        this.styleRepository = styleRepository;
    }

    @Override
    public ResultDO<QueryStyleDetailResponseDTO> queryStyleDetail(QueryStyleDetailRequestDTO requestDTO) {
        // 1. 参数自校验交给 DTO，AppService 不写 if 判断
        ResultDO<Void> checkResult = requestDTO.check();
        if (!checkResult.isSuccess()) {
            return ResultDO.buildFailResult(checkResult.getCode(), checkResult.getMsg());
        }
        try {
            // 2. 查本领域数据
            ResultDO<StyleAggregate> queryResult = styleRepository.query(StyleQuery.byId(requestDTO.getStyleId()));
            if (!queryResult.isSuccess()) {
                return ResultDO.buildFailResult(queryResult.getCode(), queryResult.getMsg());
            }
            StyleAggregate style = queryResult.getData();
            if (style == null) {
                return ResultDO.buildFailResult("STYLE_NOT_FOUND", "款不存在");
            }
            // 3. 转 DTO 返回
            return ResultDO.buildSuccessResult(StyleAssembler.toStyleDetailDTO(style));
        } catch (Throwable e) {
            log.error("查询款详情异常, styleId={}", requestDTO.getStyleId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}