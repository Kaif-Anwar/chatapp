package com.kaif.chatapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed per-user (or per-IP for public routes) rate limiting.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, RateRule> RULES = Map.of(
            "POST:/api/auth/login", new RateRule(10, Duration.ofMinutes(1)),
            "POST:/api/auth/register", new RateRule(5, Duration.ofMinutes(1)),
            "POST:/api/auth/forgot-password", new RateRule(3, Duration.ofHours(1)),
            "POST:/api/auth/reset-password", new RateRule(5, Duration.ofMinutes(1)),
            "POST:/api/conversations/", new RateRule(60, Duration.ofMinutes(1)),
            "POST:/api/users/me/avatar", new RateRule(10, Duration.ofMinutes(1))
    );

    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = buildKey(request);
        RateRule rule = findRule(request);
        if (rule != null && key != null && isRateLimited(key, rule)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private RateRule findRule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        for (Map.Entry<String, RateRule> e : RULES.entrySet()) {
            String[] parts = e.getKey().split(":", 2);
            if (parts[0].equals(method) && path.startsWith(parts[1])) {
                return e.getValue();
            }
        }
        return null;
    }

    private String buildKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kaif.chatapp.user.model.User user) {
            return "ratelimit:user:" + user.getId() + ":" + request.getMethod() + ":" + simplifyPath(request.getRequestURI());
        }
        String ip = request.getRemoteAddr();
        return "ratelimit:ip:" + ip + ":" + request.getMethod() + ":" + simplifyPath(request.getRequestURI());
    }

    private String simplifyPath(String path) {
        if (path.startsWith("/api/conversations/") && path.contains("/messages")) {
            return "/api/conversations/*/messages";
        }
        return path;
    }

    private boolean isRateLimited(String key, RateRule rule) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, rule.window);
        }
        return count != null && count > rule.maxRequests;
    }

    private record RateRule(int maxRequests, Duration window) {}
}
