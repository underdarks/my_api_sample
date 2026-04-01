package com.example.my_api.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {

    @Bean
    @ServiceConnection // 스프링 부트 4.0이 컨테이너 정보를 자동으로 DataSource에 주입함
    public PostgreSQLContainer<?> postgresContainer(
        @Value("${testcontainers.postgres.image:postgres:16-alpine}")
        String image) {

        // username/password 없음 - @ServiceConnection이 알아서 처리
        return new PostgreSQLContainer<>(image); // 가벼운 alpine 이미지 권장

    }

}
