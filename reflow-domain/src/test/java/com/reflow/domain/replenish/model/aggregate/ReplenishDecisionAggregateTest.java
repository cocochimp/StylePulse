package com.reflow.domain.replenish.model.aggregate;

import com.reflow.common.exception.AggregateException;
import com.reflow.domain.replenish.model.param.ApproveDecisionParam;
import com.reflow.domain.replenish.model.param.DecisionItemParam;
import com.reflow.domain.replenish.model.param.OpenDecisionParam;
import com.reflow.model.replenish.DecisionStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 决策单测试：不变量 + 状态机。
 *
 * <p>不变量测试比状态机测试更重要：状态错了只是流程卡住，
 * 数量不守恒则会直接变成"工厂多生产或少生产"，是真金白银的损失。</p>
 */
class ReplenishDecisionAggregateTest {

    @Test
    @DisplayName("尺码行合计必须等于总量")
    void shouldRejectUnbalancedItems() {
        OpenDecisionParam param = openParam(660, List.of(
                new DecisionItemParam(11L, "BLACK", "S", 113),
                new DecisionItemParam(12L, "BLACK", "M", 264)));

        assertThatThrownBy(() -> ReplenishDecisionAggregate.open(param))
                .isInstanceOf(AggregateException.class)
                .hasMessageContaining("不一致");
    }

    @Test
    @DisplayName("正常开单：状态为待审批，版本从 0 开始")
    void shouldOpenPendingReview() {
        OpenDecisionParam param = openParam(300, List.of(
                new DecisionItemParam(11L, "BLACK", "S", 100),
                new DecisionItemParam(12L, "BLACK", "M", 200)));

        ReplenishDecisionAggregate decision = ReplenishDecisionAggregate.open(param);

        assertThat(decision.getStatus()).isEqualTo(DecisionStatusEnum.PENDING_REVIEW);
        assertThat(decision.getVersion()).isZero();
        assertThat(decision.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("驳回后进入终态，不能再审批")
    void shouldRejectAfterRejected() {
        ReplenishDecisionAggregate decision = ReplenishDecisionAggregate.open(openParam(200,
                List.of(new DecisionItemParam(11L, "BLACK", "S", 200))));

        ApproveDecisionParam param = new ApproveDecisionParam();
        param.setDecisionId(1L);
        param.setApproved(false);
        decision.approve(param);

        assertThat(decision.getStatus()).isEqualTo(DecisionStatusEnum.REJECTED);
        assertThat(decision.getStatus().isFinal()).isTrue();

        param.setApproved(true);
        assertThatThrownBy(() -> decision.approve(param)).isInstanceOf(AggregateException.class);
    }

    @Test
    @DisplayName("影子决策不允许下发")
    void shouldNotDispatchShadowDecision() {
        OpenDecisionParam param = openParam(200,
                List.of(new DecisionItemParam(11L, "BLACK", "S", 200)));
        param.setShadow(Boolean.TRUE);
        ReplenishDecisionAggregate decision = ReplenishDecisionAggregate.open(param);

        ApproveDecisionParam approve = new ApproveDecisionParam();
        approve.setDecisionId(1L);
        approve.setApproved(true);
        decision.approve(approve);

        assertThatThrownBy(decision::markDispatched)
                .isInstanceOf(AggregateException.class)
                .hasMessageContaining("影子");
    }

    @Test
    @DisplayName("状态流转必须递增版本号（乐观锁依据）")
    void shouldIncreaseVersionOnTransition() {
        ReplenishDecisionAggregate decision = ReplenishDecisionAggregate.open(openParam(200,
                List.of(new DecisionItemParam(11L, "BLACK", "S", 200))));

        ApproveDecisionParam param = new ApproveDecisionParam();
        param.setDecisionId(1L);
        param.setApproved(true);
        decision.approve(param);

        assertThat(decision.getVersion()).isEqualTo(1);
    }

    private OpenDecisionParam openParam(int totalQty, List<DecisionItemParam> items) {
        OpenDecisionParam param = new OpenDecisionParam();
        param.setStyleId(1L);
        param.setRound(1);
        param.setRuleId(2L);
        param.setRuleCode("RT-STD-002");
        param.setRuleVersion(3);
        param.setTotalQty(totalQty);
        param.setTriggerSource("MANUAL");
        param.setItems(items);
        return param;
    }
}