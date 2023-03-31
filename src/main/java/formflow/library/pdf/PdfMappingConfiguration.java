package formflow.library.pdf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:pdf-mappings.yaml", factory = YamlPropertySourceFactory.class)
public class PdfMappingConfiguration {

  @Bean
  public PdfFieldMapper pdfFieldMapper(
      Map<String, Map<String, List<String>>> pdfFieldMap
  ) {
    return new PdfFieldMapper(pdfFieldMap);
  }
}
