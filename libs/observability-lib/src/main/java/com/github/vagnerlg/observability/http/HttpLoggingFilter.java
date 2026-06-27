package com.github.vagnerlg.observability.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Pattern;

class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);

    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final Pattern JWT_SUB = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"");

    private final int maxBodyBytes;

    HttpLoggingFilter(int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String userId = extractUserId(request);
        if (userId != null) {
            MDC.put("userId", userId);
        }

        var wrappedRequest = new ContentCachingRequestWrapper(request, maxBodyBytes);
        var wrappedResponse = new ContentCachingResponseWrapper(response);
        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            String traceId = MDC.get("traceId");
            if (traceId != null) {
                wrappedResponse.setHeader("X-Trace-Id", traceId);
            }

            logAccess(wrappedRequest, wrappedResponse, durationMs);
            wrappedResponse.copyBodyToResponse();

            if (userId != null) {
                MDC.remove("userId");
            }
        }
    }

    private void logAccess(ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response, long durationMs) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();

        String requestBody = buildBodyLog(request.getContentAsByteArray(),
                request.getContentType(), BODY_METHODS.contains(method));

        String responseBody = buildBodyLog(response.getContentAsByteArray(),
                response.getContentType(), true);

        log.atInfo()
                .addKeyValue("http.method", method)
                .addKeyValue("http.path", path)
                .addKeyValue("http.status", status)
                .addKeyValue("http.duration_ms", durationMs)
                .addKeyValue("http.request_body", requestBody)
                .addKeyValue("http.response_body", responseBody)
                .log("{} {} -> {} ({}ms)", method, path, status, durationMs);
    }

    private String buildBodyLog(byte[] body, String contentType, boolean shouldLog) {
        if (!shouldLog || body == null || body.length == 0) {
            return null;
        }
        if (contentType != null && contentType.contains("application/json")) {
            String raw = new String(body, StandardCharsets.UTF_8);
            return SensitiveDataMasker.mask(raw);
        }
        return "[" + body.length + " bytes, content-type: " + contentType + "]";
    }

    private static String extractUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            String[] parts = auth.substring(7).split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            var matcher = JWT_SUB.matcher(payload);
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
