package formflow.library.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "form-flow.uploads.virus-scanning.enabled", havingValue = "true")
public class ClammitClientConfiguration {

  @Bean
  public ClammitVirusScanner clammitVirusScanner() {
    return new ClammitVirusScanner();
  }
}
