package formflow.library.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

@TestConfiguration
public class WebDriverConfiguration {

  @Bean(initMethod = "start", destroyMethod = "stop")
  @Scope("singleton")
  public SeleniumFactory seleniumComponent() throws IOException {
    return new SeleniumFactory(tempDir());
  }

  @Bean
  public Path tempDir() throws IOException {
    return Files.createTempDirectory("");
  }
}
