package com.reflow.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Reflow 启动类。
 *
 * <p>它是整个工程里唯一带 {@code @SpringBootApplication} 的类，也是唯一知道
 * "所有模块都要装配起来"的地方。其他模块都不知道彼此的存在，只依赖接口。</p>
 *
 * <p>为什么组件扫描范围写到 {@code com.reflow}：六个层的实现分散在
 * domain / application / infrastructure / adaptor 四个模块里，
 * 只有把它们都扫进来，Spring 才能把"领域接口 → 基础设施实现"装配上。
 * 这也是单体分模块（而不是微服务）时保持依赖清晰的关键：
 * 编译期靠 Maven 依赖约束，运行期靠 Spring 装配。</p>
 */
@SpringBootApplication(scanBasePackages = "com.reflow")
@MapperScan("com.reflow.infrastructure")
@EnableScheduling
public class ReflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReflowApplication.class, args);
    }
}