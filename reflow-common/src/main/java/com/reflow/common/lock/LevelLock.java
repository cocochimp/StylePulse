package com.reflow.common.lock;

/**
 * 分布式锁抽象。
 *
 * <p>领域层需要"同一个款不能被并发决策"这个能力，但领域层不能依赖 Redis。
 * 解法：领域层只认这个接口，具体是 Redis 锁、ZooKeeper 锁还是单机 ReentrantLock，
 * 由 infrastructure 层实现并在启动时注入。</p>
 *
 * <p>这是依赖倒置在"锁"这个具体能力上的一次应用，和仓储接口是同一个套路。</p>
 */
public interface LevelLock {

    /**
     * 尝试加锁，不阻塞。
     *
     * @return true 表示拿到锁，false 表示已被他人持有
     */
    boolean tryLock();

    /** 释放锁。必须在 finally 中调用，否则锁会等到 TTL 过期才释放 */
    void unlock();
}