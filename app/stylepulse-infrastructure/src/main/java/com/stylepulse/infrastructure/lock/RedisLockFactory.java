package com.stylepulse.infrastructure.lock;

import com.stylepulse.common.lock.LevelLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 锁工厂：把"锁的构建"集中在一个地方。
 *
 * <p>领域层只写 {@code buildLock("style:decide:1001")}，不知道也不关心实现。
 * 好处是将来要换成 Redisson（带看门狗）时，只改这一个类；
 * 也可以在这里按 key 前缀切换实现（比如对一致性要求极高的走 ZooKeeper）。</p>
 */
@Component
public class RedisLockFactory {

    private final StringRedisTemplate redisTemplate;

    /** 锁的保险丝时长：必须大于一次业务方法的正常耗时，又要小于"人发现异常"的时间 */
    @Value("${stylepulse.lock.ttl-millis:10000}")
    private long ttlMillis;

    public RedisLockFactory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public LevelLock build(String lockKey) {
        return new RedisLevelLock(redisTemplate, lockKey, Duration.ofMillis(ttlMillis));
    }
}