package com.reflow.application.replenish.scenario;

import com.reflow.application.replenish.assembler.ReplenishAssembler;
import com.reflow.application.replenish.model.DiagnosisInternalDTO;
import com.reflow.client.replenish.ReplenishCalculateQueryAppService;
import com.reflow.client.replenish.req.DiagnoseStyleRequestDTO;
import com.reflow.client.replenish.res.DiagnoseStyleResponseDTO;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.style.model.aggregate.StyleAggregate;
import com.reflow.domain.style.model.param.StyleQuery;
import com.reflow.domain.style.repository.StyleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 款诊断应用服务（规则+计算模式）。
 *
 * <p>它只是"取款 + 交给诊断编排器 + 转 DTO"，真正的计算逻辑在领域层。
 * 如果哪天有人往这个类里加"如果 DOS 小于 3 就多追 20%"，那就是分层破了。</p>
 */
@Service
public class ReplenishCalculateQueryAppServiceImpl implements ReplenishCalculateQueryAppService {

    private static final Logger log = LoggerFactory.getLogger(ReplenishCalculateQueryAppServiceImpl.class);

    private final StyleRepository styleRepository;
    private final ReplenishDiagnosisSupport diagnosisSupport;

    public ReplenishCalculateQueryAppServiceImpl(StyleRepository styleRepository,
                                                 ReplenishDiagnosisSupport diagnosisSupport) {
        this.styleRepository = styleRepository;
        this.diagnosisSupport = diagnosisSupport;
    }

    @Override
    public ResultDO<DiagnoseStyleResponseDTO> diagnoseStyle(DiagnoseStyleRequestDTO requestDTO) {
        ResultDO<Void> checkResult = requestDTO.check();
        if (!checkResult.isSuccess()) {
            return ResultDO.buildFailResult(checkResult.getCode(), checkResult.getMsg());
        }
        try {
            ResultDO<StyleAggregate> styleResult = styleRepository.query(StyleQuery.byId(requestDTO.getStyleId()));
            if (!styleResult.isSuccess()) {
                return ResultDO.buildFailResult(styleResult.getCode(), styleResult.getMsg());
            }
            StyleAggregate style = styleResult.getData();
            if (style == null) {
                return ResultDO.buildFailResult("STYLE_NOT_FOUND", "款不存在");
            }

            ResultDO<DiagnosisInternalDTO> diagnosisResult = diagnosisSupport.diagnose(style);
            if (!diagnosisResult.isSuccess()) {
                return ResultDO.buildFailResult(diagnosisResult.getCode(), diagnosisResult.getMsg());
            }

            return ResultDO.buildSuccessResult(
                    ReplenishAssembler.toDiagnoseDTO(style.getId(), diagnosisResult.getData()));
        } catch (Throwable e) {
            log.error("款诊断异常, styleId={}", requestDTO.getStyleId(), e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        }
    }
}