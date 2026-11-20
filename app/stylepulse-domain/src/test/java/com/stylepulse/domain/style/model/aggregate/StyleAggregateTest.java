package com.stylepulse.domain.style.model.aggregate;

import com.stylepulse.common.exception.AggregateException;
import com.stylepulse.domain.style.model.param.ChangeStageParam;
import com.stylepulse.model.style.PriceBandEnum;
import com.stylepulse.model.style.StageEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 款状态机测试。
 *
 * <p>状态机的价值在于"拦住不该发生的流转"，所以测试要覆盖非法路径，
 * 不能只测 happy path。这类测试的性价比极高：它把"业务上不可能发生的事"
 * 永久钉在代码里，比写在文档里可靠。</p>
 */
class StyleAggregateTest {

    @Test
    @DisplayName("合法流转：观察期 → 返单中")
    void shouldAllowLegalTransition() {
        StyleAggregate style = style(StageEnum.OBSERVING, 0);

        style.changeStage(new ChangeStageParam(StageEnum.REPLENISHING, "命中返单规则"));

        assertThat(style.getStage()).isEqualTo(StageEnum.REPLENISHING);
        assertThat(style.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("非法流转：草稿不能直接到放量")
    void shouldRejectIllegalTransition() {
        StyleAggregate style = style(StageEnum.DRAFT, 0);

        assertThatThrownBy(() -> style.changeStage(new ChangeStageParam(StageEnum.SCALING, "跳过测款")))
                .isInstanceOf(AggregateException.class)
                .hasMessageContaining("不允许");
    }

    @Test
    @DisplayName("终态不能再流转：已归档拒绝任何变更")
    void shouldRejectTransitionFromArchived() {
        StyleAggregate style = style(StageEnum.ARCHIVED, 0);

        assertThatThrownBy(() -> style.changeStage(new ChangeStageParam(StageEnum.OBSERVING, "复活")))
                .isInstanceOf(AggregateException.class);
    }

    @Test
    @DisplayName("清仓款不允许开返单决策")
    void shouldNotAllowDecisionInClearance() {
        StyleAggregate style = style(StageEnum.CLEARANCE, 0);
        assertThat(style.canOpenDecision()).isFalse();
    }

    @Test
    @DisplayName("观察期允许开返单决策")
    void shouldAllowDecisionInObserving() {
        StyleAggregate style = style(StageEnum.OBSERVING, 0);
        assertThat(style.canOpenDecision()).isTrue();
    }

    @Test
    @DisplayName("上架天数不能为负：未来日期按 0 处理")
    void shouldClampNegativeAgeDays() {
        StyleAggregate style = new StyleAggregate();
        style.setLaunchDate(LocalDate.now().plusDays(3));
        assertThat(style.ageDays(LocalDate.now())).isZero();
    }

    private StyleAggregate style(StageEnum stage, int version) {
        StyleAggregate style = new StyleAggregate();
        style.setId(1L);
        style.setStyleCode("SH-2026-0001");
        style.setCategoryId(1L);
        style.setPriceBand(PriceBandEnum.LOW);
        style.setLaunchDate(LocalDate.now().minusDays(12));
        style.setFirstOrderQty(600);
        style.setStage(stage);
        style.setVersion(version);
        return style;
    }
}