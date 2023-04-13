package formflow.library.config;

import formflow.library.pdf.PdfMapConfiguration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Factory for PdfMapConfiguration configuration.
 */
@Configuration
public class PdfMapConfigurationFactoryConfig {

  @Bean
  public PdfMapConfigurationFactory pdfMapConfigurationFactory() {
    return new PdfMapConfigurationFactory();
  }

  /**
   * Bean to get a list of FlowConfiguration objects.
   *
   * @return list of flow configuration objects
   */
  @Bean
  public List<PdfMapConfiguration> pdfMapConfiguration() {
    return new PdfMapConfigurationFactory().getObject();
  }
}
