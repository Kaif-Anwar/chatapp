package com.kaif.chatapp.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    @Test
    void resolveClientIp_prefersXRealIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.10");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1, 10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("172.18.0.4");

        assertThat(RateLimitFilter.resolveClientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void resolveClientIp_usesFirstForwardedForWhenNoRealIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1, 10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("172.18.0.4");

        assertThat(RateLimitFilter.resolveClientIp(request)).isEqualTo("198.51.100.1");
    }

    @Test
    void resolveClientIp_fallsBackToRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("172.18.0.4");

        assertThat(RateLimitFilter.resolveClientIp(request)).isEqualTo("172.18.0.4");
    }
}
