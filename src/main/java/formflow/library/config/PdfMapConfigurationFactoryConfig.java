package formflow.library.config;

import formflow.library.pdf.PdfMapConfiguration;
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
  public PdfMapConfiguration pdfMapConfiguration() {
    return new PdfMapConfiguration(new PdfMapConfigurationFactory().getObject());
  }
}
