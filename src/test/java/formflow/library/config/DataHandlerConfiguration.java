package formflow.library.config;

import formflow.library.interceptors.DataRequiredInterceptor;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DataHandlerConfiguration implements WebMvcConfigurer {
  
  @Autowired
  List<FlowConfiguration> flowConfigurations;
  
  @Override
  public void addInterceptors(InterceptorRegistry registry){
    registry.addInterceptor(new DataRequiredInterceptor(flowConfigurations)).addPathPatterns(List.of(DataRequiredInterceptor.FLOW_PATH_FORMAT, DataRequiredInterceptor.NAVIGATION_FLOW_PATH_FORMAT));
  }
}
