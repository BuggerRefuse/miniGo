package org.example.admin.application.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "mini-go.admin.jwt")
public class JwtProperties {

    private String secret = "minigo-local-development-jwt-secret-change-me-now";
    private String issuer = "mini-go-client";
    private Duration accessTtl = Duration.ofMinutes(15);
    private Duration refreshTtl = Duration.ofDays(7);
}
