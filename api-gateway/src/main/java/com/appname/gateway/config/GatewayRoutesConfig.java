package com.appname.gateway.config;

import com.appname.gateway.filter.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines all gateway routes and applies the JWT authentication filter.
 *
 * <p>Route ordering matters: more specific paths must be listed before
 * broader ones. All routes apply the {@link AuthenticationFilter} which
 * validates JWT tokens and injects {@code X-User-Id} and {@code X-User-Role}
 * headers for downstream services.</p>
 */
@Configuration
@RequiredArgsConstructor
public class GatewayRoutesConfig {

  private final AuthenticationFilter authenticationFilter;

  @Value("${services.auth-service.base-url}")
  private String authServiceUrl;

  @Value("${services.user-service.base-url}")
  private String userServiceUrl;

  @Value("${services.order-service.base-url}")
  private String orderServiceUrl;

  @Bean
  public RouteLocator routeLocator(RouteLocatorBuilder builder) {
    AuthenticationFilter.Config filterConfig = new AuthenticationFilter.Config();

    return builder.routes()
            .route("auth-service", r -> r
                    .path("/api/v1/auth/**")
                    .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                    .uri(authServiceUrl))
            .route("user-service", r -> r
                    .path("/api/v1/users/**")
                    .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                    .uri(userServiceUrl))
            .route("order-service", r -> r
                    .path("/api/v1/orders/**")
                    .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                    .uri(orderServiceUrl))
            .build();
  }

}
