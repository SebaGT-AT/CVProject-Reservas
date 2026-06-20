package cl.reservas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthRateLimitFilterTest {
    @Test
    void blocksLoginAttemptsAfterTheConfiguredLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);

        for (int attempt = 1; attempt <= 10; attempt++) {
            var allowedRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            allowedRequest.setRemoteAddr("192.0.2.10");
            var allowedResponse = new MockHttpServletResponse();
            filter.doFilter(allowedRequest, allowedResponse, chain);
            assertThat(allowedResponse.getStatus()).isEqualTo(200);
        }

        var blockedRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        blockedRequest.setRemoteAddr("192.0.2.10");
        var blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blockedRequest, blockedResponse, chain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isNotBlank();
        assertThat(blockedResponse.getContentAsString()).contains("Demasiadas solicitudes");
    }
}

