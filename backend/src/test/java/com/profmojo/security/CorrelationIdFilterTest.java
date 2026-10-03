package com.profmojo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CorrelationIdFilter Unit Tests")
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Missing header generates a valid UUID and sets it in response and MDC")
    void missingHeader_GeneratesUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValueDuringExecution = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcValueDuringExecution.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        String responseHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader, "Response should have correlation ID header");
        assertFalse(responseHeader.isBlank());
        assertEquals(responseHeader, mdcValueDuringExecution.get(), "MDC should match response header during filter execution");
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY), "MDC should be cleared after filter completion");
    }

    @Test
    @DisplayName("Valid client-supplied correlation ID is preserved")
    void validHeader_Preserved() throws ServletException, IOException {
        String clientCorrelationId = "test-client-corr-id-999";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, clientCorrelationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValue = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcValue.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        assertEquals(clientCorrelationId, response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertEquals(clientCorrelationId, mdcValue.get());
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    @Test
    @DisplayName("Malicious or non-alphanumeric correlation ID is sanitized and replaced")
    void invalidHeader_SanitizedAndReplaced() throws ServletException, IOException {
        String maliciousId = "invalid/id;<script>alert(1)</script>";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, maliciousId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValue = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcValue.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        String resultHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(resultHeader);
        assertNotEquals(maliciousId, resultHeader, "Malicious header should be replaced");
        assertFalse(resultHeader.contains("<"), "Sanitized ID must not contain tags");
        assertEquals(resultHeader, mdcValue.get());
    }

    @Test
    @DisplayName("MDC is cleaned up even when downstream filter chain throws exception")
    void mdcCleanedUp_OnException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain failingChain = (req, res) -> {
            throw new RuntimeException("Downstream filter exception");
        };

        assertThrows(RuntimeException.class, () -> filter.doFilterInternal(request, response, failingChain));
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY), "MDC must be cleared on exception");
    }
}
