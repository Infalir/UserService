package com.appname.gateway.filter;

import com.appname.gateway.exception.GatewayAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class AuthenticationFilter
        extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

  private static final String BEARER_PREFIX = "Bearer ";

  private static final List<String> PUBLIC_PATHS = List.of(
          "/api/v1/auth/login",
          "/api/v1/auth/refresh",
          "/api/v1/auth/validate",
          "/api/v1/gateway/register",
          "/actuator/**"
  );

  private final JwtValidator jwtValidator;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public AuthenticationFilter(JwtValidator jwtValidator) {
    super(Config.class);
    this.jwtValidator = jwtValidator;
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      String path = exchange.getRequest().getURI().getPath();

      if (isPublicPath(path)) {
        return chain.filter(exchange);
      }

      String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
        log.warn("Missing or malformed Authorization header for path: {}", path);
        return unauthorized(exchange, "Missing or malformed Authorization header");
      }

      String token = authHeader.substring(BEARER_PREFIX.length());

      if (!jwtValidator.isValid(token)) {
        log.warn("Invalid JWT token for path: {}", path);
        return unauthorized(exchange, "Invalid or expired JWT token");
      }

      String userId = jwtValidator.extractUserId(token);
      String role = jwtValidator.extractRole(token);

      ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
              .header("X-User-Id",   userId).header("X-User-Role", role)
              .build();

      log.debug("Authenticated userId={} role={} → {}", userId, role, path);
      return chain.filter(exchange.mutate().request(mutatedRequest).build());

    };
  }

  private boolean isPublicPath(String path) {
    return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().add("Content-Type", "application/json");
    String body = """
                {"status":401,"error":"Unauthorized","message":"%s"}
                """.formatted(message);
    var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());

    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  public static class Config {  }

}
