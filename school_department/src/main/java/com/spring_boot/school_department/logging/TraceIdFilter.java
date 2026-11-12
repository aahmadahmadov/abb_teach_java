package com.spring_boot.school_department.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String TRACE_HEADER = "X-Trace-Id";

    // log injection olmasin deye regex ile filter edirik yalniz herf, reqem, tire icaze verilir
    private static final Pattern VALID_TRACE_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String traceId = request.getHeader(TRACE_HEADER);

        if (traceId == null || !VALID_TRACE_ID.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        try {

            filterChain.doFilter(request, response);

        } finally {

            // thread pool-a qayidan thread-de kohne id qalmasin deye silirik
            MDC.remove(TRACE_ID);
        }
    }
}
