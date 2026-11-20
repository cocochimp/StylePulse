package com.stylepulse.domain.style.service;

import com.stylepulse.common.exception.AggregateException;
import com.stylepulse.common.exception.BizException;
import com.stylepulse.common.lock.LevelLock;
import com.stylepulse.common.result.ResultDO;
import com.stylepulse.domain.style.model.aggregate.StyleAggregate;
import com.stylepulse.domain.style.model.param.ChangeStageParam;
import com.stylepulse.domain.style.model.param.StyleQuery;
import com.stylepulse.domain.style.repository.StyleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 款领域服务实现。
 *
 * <p>看这个类能看清 easy-DDD 写模式的标准骨架：
 * <pre>
 * 取锁 → 加载聚合根 → 判空 → 调聚合根方法（业务规则都在里面）→ 保存
 * </pre>
 * Service 里没有 if-else 业务判断，只有"加载不到怎么办"这类流程控制。
 * 一旦发现自己在 Service 里写"如果库存小于 10 就打折"这种规则，就该挪回聚合根。</p>
 */
@Service
public class StyleDomainServiceImpl implements StyleDomainService {

    private static final Logger log = LoggerFactory.getLogger(StyleDomainServiceImpl.class);

    private final StyleRepository styleRepository;

    public StyleDomainServiceImpl(StyleRepository styleRepository) {
        this.styleRepository = styleRepository;
    }

    @Override
    public ResultDO<Void> changeStage(Long styleId, ChangeStageParam param) {
        LevelLock lock = styleRepository.buildLock("style:stage:" + styleId);
        try {
            if (!lock.tryLock()) {
                return ResultDO.buildFailResult("LOCK_FAIL", "该款正在被其他操作处理，请稍后重试");
            }

            ResultDO<StyleAggregate> queryResult = styleRepository.query(StyleQuery.byId(styleId));
            if (!queryResult.isSuccess()) {
                return ResultDO.buildFailResult(queryResult.getCode(), queryResult.getMsg());
            }
            StyleAggregate style = queryResult.getData();
            if (style == null) {
                return ResultDO.buildFailResult("STYLE_NOT_FOUND", "款不存在: " + styleId);
            }

            // 业务规则全部在聚合根里，这里只负责调用
            style.changeStage(param);

            ResultDO<Void> saveResult = styleRepository.save(style);
            if (!saveResult.isSuccess()) {
                return saveResult;
            }
            return ResultDO.buildSuccessResult();

        } catch (AggregateException e) {
            // 聚合根不变量被破坏：是"调用时机不对"，不是系统故障
            log.warn("阶段流转被拒绝, styleId={}, param={}, reason={}", styleId, param.getTargetStage(), e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (BizException e) {
            log.warn("阶段流转业务异常, styleId={}, msg={}", styleId, e.getMessage());
            return ResultDO.buildFailResult(e.getCode(), e.getMessage());
        } catch (Throwable e) {
            log.error("阶段流转系统异常, styleId={}", styleId, e);
            return ResultDO.buildFailResult("SYSTEM_ERROR", "系统异常");
        } finally {
            lock.unlock();
        }
    }
}