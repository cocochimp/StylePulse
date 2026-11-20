package com.stylepulse.model.common;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 业务日。
 *
 * <p>为什么不直接用 {@link LocalDate}：业务日不等于自然日。
 * 供货商在多个市场卖货，各国时区差最多 15 小时，"今天的销量"到底是哪个今天，
 * 直接决定动销率算出来是多少。业务日把"时区归一"这件事显式建模，
 * 让"这里用的是业务日还是自然日"在类型上无法混淆。</p>
 *
 * <p>本工程为简化演示，业务日直接等于供货商所在时区的自然日；
 * 多市场场景下应由 Application 层按市场时区换算后传入。</p>
 */
public final class BusinessDate implements Serializable, Comparable<BusinessDate> {

    private static final long serialVersionUID = 1L;

    private final int year;
    private final int month;
    private final int day;

    private BusinessDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static BusinessDate of(LocalDate date) {
        Objects.requireNonNull(date, "date 不能为空");
        return new BusinessDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    public static BusinessDate of(int year, int month, int day) {
        // 借 LocalDate 做合法性校验，非法日期（如 2 月 30 日）在这里就炸掉
        LocalDate.of(year, month, day);
        return new BusinessDate(year, month, day);
    }

    public static BusinessDate today() {
        return of(LocalDate.now());
    }

    public BusinessDate plusDays(int days) {
        return of(toLocalDate().plusDays(days));
    }

    public LocalDate toLocalDate() {
        return LocalDate.of(year, month, day);
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    @Override
    public int compareTo(BusinessDate other) {
        return toLocalDate().compareTo(other.toLocalDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BusinessDate)) {
            return false;
        }
        BusinessDate that = (BusinessDate) o;
        return year == that.year && month == that.month && day == that.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }

    /** 格式固定 yyyy-MM-dd，便于作为 Redis key / DB 列值直接使用 */
    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d", year, month, day);
    }
}