//package formflow.library.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebMvcConfig implements WebMvcConfigurer {
//
//  @Bean
//  public ThreadPoolTaskExecutor mvcTaskExecutor() {
//    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
//    taskExecutor.setCorePoolSize(10);
//    taskExecutor.setMaxPoolSize(10);
//    return taskExecutor;
//  }
//
//  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
//    configurer.setTaskExecutor(mvcTaskExecutor());
//  }
//}
