//package formflow.library.pdf;
//
//import formflow.library.utils.YamlPropertySourceFactory;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.PropertySource;
//import org.springframework.context.annotation.PropertySources;
//
//@Configuration
//@PropertySources({
//    @PropertySource(value = "classpath:formflow/library/pdf/pdf-map.yaml", factory = YamlPropertySourceFactory.class, ignoreResourceNotFound = true),
//    @PropertySource(value = "file:${form-flow.pdf-map-location:pdf-map.yaml}", factory = YamlPropertySourceFactory.class)
//})
//public class PdfMappingConfiguration {
//
//  private String pdfMapLocation;
//
//  public PdfMappingConfiguration(@Value("${form-flow.pdf-map-location:pdf-map.yaml}") String pdfMapLocation) {
//    this.pdfMapLocation = pdfMapLocation;
//  }
//
//  @Bean
//  @ConfigurationProperties(prefix = "inputs")
//  public PdfFieldMapper pdfFieldMapper(
//      Map<String, List<String>> pdfFieldMap
//  ) {
////    new YamlPropertySourceFactory().createPropertySource(null, ResourceLoader.getResource("classpath:pdf-mappings.yaml"));
//    return new PdfFieldMapper(pdfFieldMap, enumMap());
//  }
//
//  @Bean
//  @ConfigurationProperties(prefix = "enums")
//  public Map<String, String> enumMap() {
//    return new HashMap<>();
//  }
//
//  @Bean
//  public PdfFieldMapper pdfFieldMapper(
//      Map<String, List<String>> pdfFieldMap,
//      Map<String, String> enumMap
//  ) {
//    return new PdfFieldMapper(pdfFieldMap, enumMap);
//  }
//}
