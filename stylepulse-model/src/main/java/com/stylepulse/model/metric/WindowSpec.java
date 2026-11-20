package com.stylepulse.model.metric;

/**
 * 滑动窗口规格（跨上下文共享常量）。
 *
 * <p>为什么放 model：分桶粒度同时被三处使用——metric 上下文算水位、
 * infrastructure 读写 Redis、adaptation 层做对账。放在任何一侧都会造成反向依赖。</p>
 *
 * <p>为什么是 5 分钟：桶粒度是"精度"和"内存"的折中。
 * 1 分钟桶能让水位误差小于 1 分钟，但 7 天需要 10080 个桶/款；
 * 5 分钟桶降到 2016 个，而误差对"几天窗口的动销率"几乎无影响。</p>
 */
public final class WindowSpec {

    /** 短窗口：近 24 小时，5 分钟桶 */
    public static final long BUCKET_MILLIS = 5 * 60 * 1000L;

    /** 短窗口覆盖天数 */
    public static final int SHORT_WINDOW_DAYS = 1;

    /** 长窗口覆盖天数（动销率默认用 3 天，可配） */
    public static final int DEFAULT_WINDOW_DAYS = 3;

    /** 事件去重标记的 TTL：必须覆盖最长窗口，否则窗口内的重复事件会被重复计数 */
    public static final long EVENT_DEDUP_TTL_SECONDS = 8L * 24 * 60 * 60;

    private WindowSpec() {
    }

    /** 把任意时间戳对齐到桶起点。向下取整，保证同一 5 分钟内的所有事件落进同一个桶 */
    public static long bucketOf(long epochMillis) {
        return epochMillis - (epochMillis % BUCKET_MILLIS);
    }
}