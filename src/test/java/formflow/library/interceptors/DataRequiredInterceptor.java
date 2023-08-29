package formflow.library.interceptors;

import formflow.library.config.FlowConfiguration;
import formflow.library.data.SubmissionRepositoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class DataRequiredInterceptor implements HandlerInterceptor {
    public static final String FLOW_PATH_FORMAT = "/flow/{flow}/{screen}";
    public static final String NAVIGATION_FLOW_PATH_FORMAT = "/flow/{flow}/{screen}/navigation";
    
    List<FlowConfiguration> flowConfigurations;
    
    public DataRequiredInterceptor(List<FlowConfiguration> flowConfigurations){
        this.flowConfigurations = flowConfigurations;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        try {
            String pathFormat = request.getRequestURI().contains("navigation") ? NAVIGATION_FLOW_PATH_FORMAT : FLOW_PATH_FORMAT;
            Map<String, String> parsedUrl = new AntPathMatcher().extractUriTemplateVariables(pathFormat, request.getRequestURI());
            String redirect_url = "/";
            String screen = parsedUrl.get("screen");
            String flow = parsedUrl.get("flow");
            FlowConfiguration flowConfiguration = flowConfigurations.stream().filter(fc -> fc.getName().equals(flow)).findFirst()
                .orElse(null);
            // TODO Do we return 404 if flow config is null here instead of in controller?
            String firstScreen = flowConfiguration.getLandmarks().getFirstScreen();
            HttpSession session = request.getSession(false);
            if (session == null && !screen.equals(firstScreen)) {
                response.sendRedirect(redirect_url);
                return false;
            }
            return true;
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
