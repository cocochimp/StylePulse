package com.reflow.application.replenish.assembler;

import com.reflow.application.replenish.port.FactoryOrderCommand;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.result.ReplenishDecisionResult;
import com.reflow.domain.style.model.aggregate.StyleAggregate;
import com.reflow.domain.style.model.param.StyleQuery;
import com.reflow.domain.style.repository.StyleRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产指令组装器。
 *
 * <p>抽成独立组件的原因：首次下发和失败重试要用<b>完全一样</b>的指令结构。
 * 如果两处各写一份，重试时发出去的单子很可能和首次不一致
 * （少个字段、规格不同），工厂那边就成了两套数据。</p>
 */
@Component
public class FactoryOrderCommandAssembler {

    /** 默认排产日：留 7 天生产周期 */
    private static final int DEFAULT_PRODUCTION_LEAD_DAYS = 7;
    private static final Long DEFAULT_FACTORY_ID = 1L;
    private static final Long DEFAULT_LINE_ID = 1L;

    private final StyleRepository styleRepository;

    public FactoryOrderCommandAssembler(StyleRepository styleRepository) {
        this.styleRepository = styleRepository;
    }

    public FactoryOrderCommand build(ReplenishDecisionResult decision) {
        FactoryOrderCommand command = new FactoryOrderCommand();
        command.setDecisionId(decision.getDecisionId());
        command.setStyleId(decision.getStyleId());
        command.setFactoryId(DEFAULT_FACTORY_ID);
        command.setLineId(DEFAULT_LINE_ID);
        command.setBizDate(LocalDate.now().plusDays(DEFAULT_PRODUCTION_LEAD_DAYS).toString());
        command.setTotalQty(decision.getTotalQty());

        ResultDO<StyleAggregate> styleResult = styleRepository.query(StyleQuery.byId(decision.getStyleId()));
        if (styleResult.isSuccess() && styleResult.getData() != null) {
            command.setStyleCode(styleResult.getData().getStyleCode());
        }

        List<FactoryOrderCommand.SizeLine> lines = new ArrayList<>();
        decision.getItems().forEach(item ->
                lines.add(new FactoryOrderCommand.SizeLine(item.getColorCode(), item.getSizeCode(), item.getQty())));
        command.setLines(lines);
        return command;
    }

    public long leadDays() {
        return DEFAULT_PRODUCTION_LEAD_DAYS;
    }
}