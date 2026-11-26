package org.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class GatewayRoutesConfiguration {

    public static final String CLIENT_SERVICE_ID = "mini-go-client";

    private final GatewayCorsProperties corsProperties;

    public GatewayRoutesConfiguration(GatewayCorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public RouteLocator miniGoClientRoutes(RouteLocatorBuilder builder) {
        String clientUri = "lb://" + CLIENT_SERVICE_ID;
        return builder.routes()
                .route("mini-go-client-api", route -> route
                        .path("/api/**")
                        .uri(clientUri))
                .route("mini-go-client-web", route -> route
                        .path(
                                "/",
                                "/login.html",
                                "/welcome.html",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico"
                        )
                        .uri(clientUri))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }
}
