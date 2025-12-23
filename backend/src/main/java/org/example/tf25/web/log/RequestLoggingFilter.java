package org.example.tf25.web.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger access = LoggerFactory.getLogger("http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String sid = request.getHeader("X-Session-Id");
        if (sid != null && !sid.isBlank()) {
            MDC.put("sid", sid);
        }
        StatusCaptureResponseWrapper wrapped = new StatusCaptureResponseWrapper(response);
        try {
            chain.doFilter(request, wrapped);
        } finally {
            long took = System.currentTimeMillis() - start;
            access.info("method={} path={} status={} ms={} sid={}",
                    request.getMethod(), request.getRequestURI(), wrapped.getStatus(), took, sid);
            MDC.remove("sid");
        }
    }
}
