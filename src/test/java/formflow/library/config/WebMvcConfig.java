package formflow.library.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class WebMvcConfig implements AsyncConfigurer {

  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);  // sets the number of threads to keep in the pool
    executor.setMaxPoolSize(10);  // sets the maximum allowed number of threads
    executor.setQueueCapacity(100);  // sets the queue capacity for tasks
    executor.setThreadNamePrefix("AsyncTaskExecutor-");  // sets the prefix of the names for new threads
    executor.initialize();
    return executor;
  }
}
