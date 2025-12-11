package formflow.library.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;

@Slf4j
public class SessionExpiredCSRFAccessDeniedHandler implements AccessDeniedHandler {

    private final AccessDeniedHandler defaultHandler;

    public SessionExpiredCSRFAccessDeniedHandler() {
        this.defaultHandler = new AccessDeniedHandlerImpl();
    }

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex)
            throws IOException, ServletException {

        // Not CSRF? Preserve Spring Security default behavior.
        if (!(ex instanceof CsrfException)) {
            defaultHandler.handle(request, response, ex);
            return;
        }

        // Decide if CSRF denial is likely from expired/missing session.
        HttpSession session = request.getSession(false);
        boolean sessionMissing = (session == null);

        CsrfToken tokenInSession = null;
        if (!sessionMissing) {
            tokenInSession = (CsrfToken) session.getAttribute(CsrfToken.class.getName());
        }

        boolean likelyExpired = sessionMissing || tokenInSession == null;

        if (likelyExpired) {
            log.info("CSRF denied with missing session. Treating as expired session. URI: {}", request.getRequestURI());

            if (isAjaxRequest(request)) {
                // XHR callers cannot follow redirects, so return an error they can handle.
                response.setHeader("X-Session-Expired", "true");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Session expired");
            } else {
                // Use param to denote expired session on home screen
                response.sendRedirect(request.getContextPath() + "/?sessionExpired=true");
            }
            return;
        }

        // Preserve Spring Security default behavior in a real CSRF mismatch
        defaultHandler.handle(request, response, ex);
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}