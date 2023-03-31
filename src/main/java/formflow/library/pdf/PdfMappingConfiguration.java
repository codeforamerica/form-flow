package formflow.library.pdf;

import formflow.library.utils.YamlPropertySourceFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:pdf-map.yaml", factory = YamlPropertySourceFactory.class)
public class PdfMappingConfiguration {

  @Bean
  @ConfigurationProperties(prefix = "inputs")
  public PdfFieldMapper pdfFieldMapper(
      Map<String, List<String>> pdfFieldMap
  ) {
    return new PdfFieldMapper(pdfFieldMap, enumMap);
  }

  @Bean
  @ConfigurationProperties(prefix = "enums")
  public Map<String, String> enumMap() {
    return new HashMap<>();
  }

  @Bean
  public PdfFieldMapper pdfFieldMapper(
      Map<String, List<String>> pdfFieldMap,
      Map<String, String> enumMap
  ) {
    return new PdfFieldMapper(pdfFieldMap, enumMap);
  }
}
