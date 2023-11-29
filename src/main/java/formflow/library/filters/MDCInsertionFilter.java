package formflow.library.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Inserts useful attributes into the Mapped Diagnostic Context (MDC) for logging by clients.
 */
@Component
public class MDCInsertionFilter implements Filter {

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws ServletException, IOException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;

    var session = request.getSession(false);
    var sessionId = session == null ? null : session.getId();
    UUID submissionId = session == null ? null : (UUID) session.getAttribute("id");

    MDC.put("sessionId", sessionId);
    MDC.put("submissionId", String.valueOf(submissionId));
    MDC.put("xForwardedFor", request.getHeader("X-Forwarded-For"));
    MDC.put("method", request.getMethod());
    MDC.put("request", request.getRequestURI());

    filterChain.doFilter(servletRequest, servletResponse);

    removeMDCAttributes();
  }

  private static void removeMDCAttributes() {
    MDC.remove("sessionId");
    MDC.remove("submissionId");
    MDC.remove("xForwardedFor");
    MDC.remove("method");
    MDC.remove("request");
  }
}
