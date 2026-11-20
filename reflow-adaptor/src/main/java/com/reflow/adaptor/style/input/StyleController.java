package com.reflow.adaptor.style.input;

import com.reflow.client.style.StyleQueryAppService;
import com.reflow.client.style.req.QueryStyleDetailRequestDTO;
import com.reflow.client.style.res.QueryStyleDetailResponseDTO;
import com.reflow.common.result.ResultDO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 款查询入口（Input Adaptor）。
 *
 * <p>入口层只做三件事：协议转换、参数装填、调用应用服务。
 * 这里<b>不允许</b>出现"如果款已归档就不返回"这类业务判断——
 * 只要出现一个，业务规则就有了第二个入口，之后必然分叉。</p>
 */
@RestController
@RequestMapping("/api/styles")
public class StyleController {

    private final StyleQueryAppService styleQueryAppService;

    public StyleController(StyleQueryAppService styleQueryAppService) {
        this.styleQueryAppService = styleQueryAppService;
    }

    @GetMapping("/{styleId}")
    public ResultDO<QueryStyleDetailResponseDTO> queryStyleDetail(@PathVariable("styleId") Long styleId) {
        QueryStyleDetailRequestDTO requestDTO = new QueryStyleDetailRequestDTO();
        requestDTO.setStyleId(styleId);
        return styleQueryAppService.queryStyleDetail(requestDTO);
    }
}