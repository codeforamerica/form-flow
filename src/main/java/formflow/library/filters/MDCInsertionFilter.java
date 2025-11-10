package formflow.library.filters;

import formflow.library.FormFlowController;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * Inserts useful attributes into the Mapped Diagnostic Context (MDC) for logging by clients.
 */
@Component
public class MDCInsertionFilter implements Filter {

    public static final String PATH_FORMAT = "/flow/{flow}/{screen}";

    /**
     * Default constructor.
     */
    public MDCInsertionFilter() {
    }

    private static void removeMDCAttributes() {
        MDC.remove("sessionId");
        MDC.remove("submissionId");
        MDC.remove("xForwardedFor");
        MDC.remove("method");
        MDC.remove("request");
    }

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain
    ) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        HttpSession session = request.getSession(false);
        String sessionId = session == null ? null : session.getId();
        UUID submissionId = null;

        try {
            Map<String, String> parsedUrl = new AntPathMatcher().extractUriTemplateVariables(PATH_FORMAT,
                    request.getRequestURI());
            submissionId = FormFlowController.getSubmissionIdForFlow(session, parsedUrl.get("flow"));
        } catch (Exception e) {
            // We could get an error if there is no session or if the URL didn't match the
            // expected pattern '/flow/{flow}/{screen}' and that's okay. Just ignore this and log
            // what data we have.
        }

        MDC.put("sessionId", sessionId);
        MDC.put("submissionId", String.valueOf(submissionId));
        MDC.put("xForwardedFor", request.getHeader("X-Forwarded-For"));
        MDC.put("method", request.getMethod());
        MDC.put("request", request.getRequestURI());

        filterChain.doFilter(servletRequest, servletResponse);
        removeMDCAttributes();
    }
}
