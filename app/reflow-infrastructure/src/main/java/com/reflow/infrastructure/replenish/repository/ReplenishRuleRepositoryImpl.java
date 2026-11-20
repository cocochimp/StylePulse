package com.reflow.infrastructure.replenish.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reflow.common.result.ResultDO;
import com.reflow.domain.replenish.model.aggregate.ReplenishRuleAggregate;
import com.reflow.domain.replenish.model.param.MatchStyleParam;
import com.reflow.domain.replenish.repository.ReplenishRuleRepository;
import com.reflow.infrastructure.replenish.converter.ReplenishRuleConverter;
import com.reflow.infrastructure.replenish.index.ReplenishRuleIndex;
import com.reflow.infrastructure.replenish.mysql.mapper.ReplenishRuleMapper;
import com.reflow.infrastructure.replenish.mysql.po.ReplenishRulePO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 返单规则仓储实现 —— 倒排索引的持有者。
 *
 * <p>索引放在 {@code volatile} 字段里，刷新时整体替换引用（双缓冲）：
 * 构建新索引期间，读请求继续用旧索引，不会出现"索引正在重建、查询返回空"的抖动。
 * 这正是"规则热更新不影响线上"的实现方式。</p>
 */
@Component
public class ReplenishRuleRepositoryImpl implements ReplenishRuleRepository {

    private static final Logger log = LoggerFactory.getLogger(ReplenishRuleRepositoryImpl.class);

    private final ReplenishRuleMapper replenishRuleMapper;

    /** 当前生效的索引；volatile 保证刷新后其他线程立即可见 */
    private volatile ReplenishRuleIndex index = ReplenishRuleIndex.empty();

    public ReplenishRuleRepositoryImpl(ReplenishRuleMapper replenishRuleMapper) {
        this.replenishRuleMapper = replenishRuleMapper;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 重建索引。
     *
     * <p>失败时保留旧索引继续服务——这是"规则改坏了不该让系统停摆"的具体体现。</p>
     */
    public void refresh() {
        try {
            ResultDO<List<ReplenishRuleAggregate>> all = queryAllEnabled();
            if (!all.isSuccess()) {
                log.warn("规则索引重建失败，保留旧索引: {}", all.getMsg());
                return;
            }
            ReplenishRuleIndex newIndex = ReplenishRuleIndex.build(all.getData());
            this.index = newIndex;
            log.info("规则索引重建完成，规则数={}", newIndex.size());
        } catch (Throwable e) {
            log.error("规则索引重建异常，保留旧索引", e);
        }
    }

    @Override
    public ResultDO<List<ReplenishRuleAggregate>> queryMatchedRules(MatchStyleParam param) {
        try {
            // 第一段：位图交集粗筛（可能多召回）
            List<ReplenishRuleAggregate> candidates = index.candidates(param);
            if (candidates.isEmpty()) {
                return ResultDO.buildSuccessResult(new ArrayList<>());
            }
            // 第二段：精确判断，剔除桶化带来的误召回
            List<ReplenishRuleAggregate> matched = new ArrayList<>();
            for (ReplenishRuleAggregate rule : candidates) {
                if (rule.matchStyle(param)) {
                    matched.add(rule);
                }
            }
            log.debug("规则命中: 候选={}, 精确命中={}", candidates.size(), matched.size());
            return ResultDO.buildSuccessResult(matched);
        } catch (Throwable e) {
            log.error("规则匹配异常, param={}", param, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "规则匹配失败");
        }
    }

    @Override
    public ResultDO<List<ReplenishRuleAggregate>> queryAllEnabled() {
        try {
            List<ReplenishRulePO> pos = replenishRuleMapper.selectList(
                    new LambdaQueryWrapper<ReplenishRulePO>()
                            .eq(ReplenishRulePO::getEnabled, 1)
                            .orderByDesc(ReplenishRulePO::getPriority));
            List<ReplenishRuleAggregate> rules = new ArrayList<>(pos.size());
            for (ReplenishRulePO po : pos) {
                rules.add(ReplenishRuleConverter.toAggregate(po));
            }
            return ResultDO.buildSuccessResult(rules);
        } catch (Throwable e) {
            log.error("查询规则异常", e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "查询规则失败");
        }
    }
}