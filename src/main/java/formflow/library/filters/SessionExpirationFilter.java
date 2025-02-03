package formflow.library.filters;

import formflow.library.ScreenController;
import formflow.library.config.FlowConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionExpirationFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"));
    public static final String FLOW_PATH_FORMAT = ScreenController.FLOW + "/" + ScreenController.FLOW_SCREEN_PATH;
    public static final String NAVIGATION_FLOW_PATH_FORMAT = FLOW_PATH_FORMAT + "/navigation";
    public List<FlowConfiguration> flowConfigurations;

    public SessionExpirationFilter(List<FlowConfiguration> flowConfigurations) {
        this.flowConfigurations = flowConfigurations;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if ("/".equals(path) || "/index".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String pathFormat = path.contains("navigation") ? NAVIGATION_FLOW_PATH_FORMAT : FLOW_PATH_FORMAT;
        Map<String, String> parsedUrl = new AntPathMatcher().extractUriTemplateVariables(pathFormat, request.getRequestURI());

        HttpSession session = request.getSession(false);

        FlowConfiguration flowConfiguration = flowConfigurations.stream()
                .filter(fc -> fc.getName().equals(parsedUrl.get("flow")))
                .findFirst()
                .orElse(null);


        // Check if the session is null or expired
        if (session == null) {
            // Redirect to the session expired page
            if (flowConfiguration != null && !parsedUrl.get("screen").equals(flowConfiguration.getLandmarks().getFirstScreen())) {
                response.sendRedirect("/?sessionExpired=true");
                return;
            }
        }
        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
