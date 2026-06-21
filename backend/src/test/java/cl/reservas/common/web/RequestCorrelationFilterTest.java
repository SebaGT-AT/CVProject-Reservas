package cl.reservas.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void preservesSafeRequestIdAndCleansDiagnosticContext() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER, "booking-12345678");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER)).isEqualTo("booking-12345678");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER, "invalid header value");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
