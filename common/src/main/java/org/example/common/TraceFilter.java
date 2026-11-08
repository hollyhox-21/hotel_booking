package org.example.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@Component
public class TraceFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String id = req.getHeader("X-Trace-Id");
    if (id == null || !id.matches("[a-zA-Z0-9-]{1,64}")) id = UUID.randomUUID().toString();
    MDC.put("traceId", id);
    res.setHeader("X-Trace-Id", id);
    try {
      chain.doFilter(req, res);
    } finally {
      MDC.clear();
    }
  }
}
