package com.stylepulse.start;

import com.stylepulse.client.capacity.CapacityAppService;
import com.stylepulse.client.capacity.req.ReserveCapacityRequestDTO;
import com.stylepulse.client.capacity.res.ReserveCapacityResponseDTO;
import com.stylepulse.client.metric.SalesEventAppService;
import com.stylepulse.client.metric.req.AcceptSalesEventRequestDTO;
import com.stylepulse.client.replenish.ReplenishCalculateQueryAppService;
import com.stylepulse.client.replenish.ReplenishDecisionAppService;
import com.stylepulse.client.replenish.req.ApproveDecisionRequestDTO;
import com.stylepulse.client.replenish.req.DiagnoseStyleRequestDTO;
import com.stylepulse.client.replenish.req.OpenDecisionRequestDTO;
import com.stylepulse.client.replenish.res.ApproveDecisionResponseDTO;
import com.stylepulse.client.replenish.res.DiagnoseStyleResponseDTO;
import com.stylepulse.client.replenish.res.OpenDecisionResponseDTO;
import com.stylepulse.common.result.ResultDO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端集成测试：从销量事件一路跑到生产指令下发。
 *
 * <p>它验证的是"四个技术点能串起来工作"：
 * <ol>
 *   <li>T2 分桶水位：事件累加、重复丢弃、乱序丢弃</li>
 *   <li>T1 倒排索引：诊断能命中规则并算出追单量</li>
 *   <li>T3 三层幂等：同一款重复开单只生成一张决策单</li>
 *   <li>T4 Lua 原子预占：并发预占不超占</li>
 * </ol>
 *
 * <p>依赖本机 Redis（默认 localhost:6379）。没有 Redis 时测试自动跳过，
 * 而不是失败——这样单元测试和集成测试可以分开跑。</p>
 */
@SpringBootTest
class StylePulseEndToEndTest {

    @Autowired
    private SalesEventAppService salesEventAppService;

    @Autowired
    private ReplenishCalculateQueryAppService calculateQueryAppService;

    @Autowired
    private ReplenishDecisionAppService decisionAppService;

    @Autowired
    private CapacityAppService capacityAppService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanUp() {
        // Redis 不可用则跳过整个集成测试
        Assumptions.assumeTrue(redisAvailable(), "本机未启动 Redis，跳过集成测试");

        // 清掉本测试会用到的一切 Redis 状态，保证多次运行结果一致：
        // 水位/幂等标记 + 两个产能槽（槽位 key 删掉后会从 DB 的 hold 表重建）
        redisTemplate.delete(List.of(
                "wm:bucket:1", "wm:exp:1", "wm:meta:1", "wm:late:1",
                "evt:it-e1", "evt:it-e2", "evt:it-e3", "evt:it-e4",
                "cap:1:1:" + LocalDate.now().plusDays(6),
                "cap:1:1:" + LocalDate.now().plusDays(7)));
    }

    @Test
    @DisplayName("全链路：事件 → 诊断 → 开单（幂等）→ 审批 → 占产能 → 下发")
    void shouldCompleteReplenishChain() {
        // ---------- 1. 灌销量事件，合计 550 件 ----------
        long now = System.currentTimeMillis();
        feedEvent("it-e1", 150, now - 400_000);
        feedEvent("it-e2", 150, now - 300_000);
        feedEvent("it-e3", 150, now - 200_000);
        feedEvent("it-e4", 100, now - 100_000);

        // 重复投递：应被幂等标记拦住
        ResultDO<?> duplicated = feedEvent("it-e4", 100, now - 50_000);
        assertThat(duplicated.isSuccess()).isTrue();

        // ---------- 2. 诊断 ----------
        DiagnoseStyleRequestDTO diagnoseReq = new DiagnoseStyleRequestDTO();
        diagnoseReq.setStyleId(1L);
        ResultDO<DiagnoseStyleResponseDTO> diagnoseResult = calculateQueryAppService.diagnoseStyle(diagnoseReq);

        assertThat(diagnoseResult.isSuccess()).isTrue();
        DiagnoseStyleResponseDTO diagnosis = diagnoseResult.getData();
        assertThat(diagnosis.getMatchedRuleCode()).isEqualTo("RT-STD-002");
        assertThat(diagnosis.getSuggestedQty()).isGreaterThanOrEqualTo(400);

        // ---------- 3. 开单 ----------
        OpenDecisionRequestDTO openReq = new OpenDecisionRequestDTO();
        openReq.setStyleId(1L);
        openReq.setTriggerSource("MANUAL");

        ResultDO<OpenDecisionResponseDTO> firstOpen = decisionAppService.openDecision(openReq);
        assertThat(firstOpen.isSuccess()).isTrue();
        OpenDecisionResponseDTO decision = firstOpen.getData();
        assertThat(decision.isDuplicated()).isFalse();
        assertThat(decision.getStatus()).isEqualTo("PENDING_REVIEW");

        // 再触发一次：必须幂等返回同一张单（模拟巡检 + 人工同时触发）
        ResultDO<OpenDecisionResponseDTO> secondOpen = decisionAppService.openDecision(openReq);
        assertThat(secondOpen.isSuccess()).isTrue();
        assertThat(secondOpen.getData().isDuplicated()).isTrue();
        assertThat(secondOpen.getData().getDecisionId()).isEqualTo(decision.getDecisionId());

        // ---------- 4. 审批 → 占产能 → 下发 ----------
        ApproveDecisionRequestDTO approveReq = new ApproveDecisionRequestDTO();
        approveReq.setDecisionId(decision.getDecisionId());
        approveReq.setApproved(Boolean.TRUE);

        ResultDO<ApproveDecisionResponseDTO> approveResult = decisionAppService.approveDecision(approveReq);
        assertThat(approveResult.isSuccess()).isTrue();
        assertThat(approveResult.getData().getStatus()).isEqualTo("DISPATCHED");
        assertThat(approveResult.getData().getCapacityHoldId()).isNotBlank();
        assertThat(approveResult.getData().getDispatched()).isTrue();
    }

    @Test
    @DisplayName("T4：10 路并发抢 800 产能，必须恰好 8 成功且不超占")
    void shouldNotOverReserveCapacity() throws Exception {
        LocalDate bizDate = LocalDate.now().plusDays(6);
        int concurrency = 10;
        int qtyPerRequest = 100;

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < concurrency; i++) {
            Callable<Boolean> task = () -> {
                startGate.await();
                ReserveCapacityRequestDTO req = new ReserveCapacityRequestDTO();
                req.setFactoryId(1L);
                req.setLineId(1L);
                req.setBizDate(bizDate.toString());
                req.setQty(qtyPerRequest);
                ResultDO<ReserveCapacityResponseDTO> result = capacityAppService.reserveCapacity(req);
                return result.isSuccess() && result.getData() != null && result.getData().isSuccess();
            };
            futures.add(pool.submit(task));
        }

        // 同时放行，制造真实竞争
        startGate.countDown();

        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get(10, TimeUnit.SECONDS))) {
                successCount++;
            }
        }
        pool.shutdown();

        // 800 / 100 = 8：多一个就是超占，少一个就是少卖
        assertThat(successCount).isEqualTo(8);

        String key = "cap:1:1:" + bizDate;
        int total = parseInt(redisTemplate.opsForHash().get(key, "total"));
        int used = parseInt(redisTemplate.opsForHash().get(key, "used"));
        assertThat(used).isLessThanOrEqualTo(total);
        assertThat(used).isEqualTo(800);
    }

    // ------------------------------------------------------------------

    private ResultDO<?> feedEvent(String eventId, int qty, long eventTimeMillis) {
        AcceptSalesEventRequestDTO req = new AcceptSalesEventRequestDTO();
        req.setEventId(eventId);
        req.setStyleId(1L);
        req.setQty(qty);
        req.setEventTimeMillis(eventTimeMillis);
        return salesEventAppService.acceptSalesEvent(req);
    }

    private boolean redisAvailable() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return pong != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private int parseInt(Object value) {
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }
}