package com.stylepulse.infrastructure.lock;

import com.stylepulse.common.lock.LevelLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 基于 Redis 的分布式锁实现（基础设施层）。
 *
 * <p>两个必须做对的细节，很多实现都栽在这里：</p>
 * <ol>
 *   <li><b>加锁必须带 TTL</b>：否则进程崩溃后锁永不释放，这个款就再也无法决策了。
 *       TTL 是"锁的保险丝"，不是"锁的过期时间"。</li>
 *   <li><b>解锁必须校验持有者</b>：不能直接 DEL。因为 A 的业务如果执行超过 TTL，
 *       锁会自动过期并被 B 拿到；此时 A 执行完再 DEL，删掉的是 B 的锁。
 *       所以解锁用 Lua 做 "值匹配才删除" 的原子操作。</li>
 * </ol>
 *
 * <p>本实现是"够用级"的：没有看门狗续期。业务执行时间远小于 TTL，
 * 续期机制带来的复杂度（后台线程、续期次数、续期失败处理）不划算。</p>
 */
public class RedisLevelLock implements LevelLock {

    /** 解锁脚本：先比对 token 再删除，保证只删自己加的锁 */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_LUA, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final String key;
    private final String token;
    private final Duration ttl;

    public RedisLevelLock(StringRedisTemplate redisTemplate, String key, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.ttl = ttl;
        // 每次构建都是新的 token，保证不同实例/线程之间不会互相"误删"
        this.token = UUID.randomUUID().toString();
    }

    @Override
    public boolean tryLock() {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void unlock() {
        // 顺序：先判断持有者，再删除。脚本在 Redis 内单线程执行，天然原子
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
    }
}