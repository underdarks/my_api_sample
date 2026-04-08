package com.example.my_api.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {

    // I/O 집중 작업(가상 스레드 사용)
    @Bean("ioExecutor")
    public ExecutorService ioExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // CPU 집중 작업(플랫폼 스레드 사용)
    @Bean("cpuExecutor")
    public ExecutorService cpuExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(cores);
    }

    // 외부 API 호출
    @Bean("externalApiExecutor")
    public ExecutorService externalApiExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
