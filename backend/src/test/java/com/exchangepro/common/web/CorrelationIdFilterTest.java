package com.exchangepro.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesIdWhenHeaderMissingAndExposesItDuringRequest() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/system/status");
        var response = new MockHttpServletResponse();
        AtomicReference<String> seenInMdc = new AtomicReference<>();
        FilterChain chain = (req, res) -> seenInMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(header).isNotBlank().hasSize(36); // random UUID
        assertThat(seenInMdc.get()).isEqualTo(header);
    }

    @Test
    void reusesWellFormedIncomingId() throws Exception {
        var request = new MockHttpServletRequest("GET", "/");
        request.addHeader(CorrelationIdFilter.HEADER, "client-trace-12345");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("client-trace-12345");
    }

    @Test
    void rejectsUnsafeIncomingIdToPreventLogInjection() {
        assertThat(CorrelationIdFilter.resolve("abc\nFAKE LOG LINE")).doesNotContain("\n").hasSize(36);
        assertThat(CorrelationIdFilter.resolve("short")).hasSize(36);
        assertThat(CorrelationIdFilter.resolve("x".repeat(65))).hasSize(36);
    }

    @Test
    void clearsMdcAfterRequestEvenWhenChainThrows() {
        var request = new MockHttpServletRequest("GET", "/");
        var response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (req, res) -> {
                throw new IllegalStateException("boom");
            });
        } catch (Exception ignored) {
            // expected
        }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
