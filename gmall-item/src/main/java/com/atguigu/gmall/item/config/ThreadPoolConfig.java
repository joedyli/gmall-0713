package com.atguigu.gmall.item.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(
            @Value("${threadPool.coreSize}")Integer coreSize,
            @Value("${threadPool.maxSize}")Integer maxSize,
            @Value("${threadPool.keepAlive}")Integer keepAlive,
            @Value("${threadPool.blockSize}")Integer blockSize
    ){
        return new ThreadPoolExecutor(coreSize, maxSize, keepAlive, TimeUnit.SECONDS, new ArrayBlockingQueue<>(blockSize), Executors.defaultThreadFactory(), (Runnable r, ThreadPoolExecutor executor) -> {
            log.warn("执行了拒绝策略");
        });
    }
}
