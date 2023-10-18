//package formflow.library.config;
//
//import formflow.library.ScreenController;
//import formflow.library.interceptors.DisabledFlowInterceptor;
//import formflow.library.interceptors.SessionContinuityInterceptor;
//import java.util.ArrayList;
//import java.util.List;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
///***
// * Adds SessionContinuityInterceptor to the Interceptor registry.
// */
//@Configuration
//public class DisabledFlowInterceptorConfiguration implements WebMvcConfigurer {
//
//  private final String[] disabledFlows;
//
//  public DisabledFlowInterceptorConfiguration(@Value("${form-flow.disabled-flows}") String[] disabledFlows) {
//    this.disabledFlows = disabledFlows;
//  }
//  
//  @Override
//  public void addInterceptors(InterceptorRegistry registry) {
//    registry.addInterceptor(new DisabledFlowInterceptor(disabledFlows))
//        .addPathPatterns(List.of(ScreenController.FLOW + ScreenController.FLOW_SCREEN_PATH));
//
//  }
//}
