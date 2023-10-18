//package formflow.library.interceptors;
//
//import formflow.library.FormFlowController;
//import formflow.library.config.FlowConfiguration;
//import formflow.library.exceptions.LandmarkNotSetException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.servlet.http.HttpSession;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import lombok.extern.slf4j.Slf4j;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.core.Ordered;
//import org.springframework.stereotype.Component;
//import org.springframework.util.AntPathMatcher;
//import org.springframework.web.servlet.HandlerInterceptor;
//
///**
// * This interceptor redirects users to the configured screen if a flow is marked as disabled.
// */
//@Component
//@Slf4j
//// TODO: once format of value is locked down, make an applicable conditional
////@ConditionalOnProperty(name = "form-flow.disabled-flows", havingValue = "*")
//public class DisabledFlowInterceptor implements HandlerInterceptor, Ordered {
//  String[] disabledFlows;
//
//  public DisabledFlowInterceptor(String[] disabledFlows) {
//    this.disabledFlows = disabledFlows;
//  }
//
//  @Override
//  public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
//    log.info("Disabled flows is equal to: " + Arrays.toString(disabledFlows));
//    return true;
//  }
//
//  @Override
//  public int getOrder() {
//    return HIGHEST_PRECEDENCE;
//  }
//}
