package formflow.library.config;

import formflow.library.pdf.PdfMap;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Factory for PdfMapConfiguration configuration.
 */
@Configuration
public class PdfMapFactoryConfig {

  @Bean
  public PdfMapFactory pdfMapFactory() {
    return new PdfMapFactory();
  }

  /**
   * Bean to get a list of FlowConfiguration objects.
   *
   * @return list of flow configuration objects
   */
  @Bean
  public List<PdfMap> pdfMaps() {
    return pdfMapFactory().getObject();
  }
}
