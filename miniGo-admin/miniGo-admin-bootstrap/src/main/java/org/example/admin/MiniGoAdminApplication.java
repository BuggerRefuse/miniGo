package org.example.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
@MapperScan("org.example.admin.adapter.out.persistent.mapper")
public class MiniGoAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniGoAdminApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
