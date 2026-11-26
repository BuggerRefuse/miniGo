package org.example;

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
@MapperScan("org.example.adapter.out.persistent.mapper")
public class miniGoClientApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(miniGoClientApp.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
