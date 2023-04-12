package formflow.library.pdf;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PdfMapConfiguration {

  String configPath = "pdf-map.yaml";
  List<PdfMap> maps;

  public PdfMapConfiguration(List<PdfMap> maps) {
    this.maps = maps;
  }

  public PdfMap getMap(String flow) {
    return getMaps().stream()
        .filter(pdfMapConfig -> pdfMapConfig.flow().equals(flow))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No PDF configuration found for flow: " + flow));
  }

  public ApplicationFile getPdfFile(String flow) throws IOException {
    String template = getMap(flow).pdf().template();
    return new ApplicationFile(getClass().getClassLoader().getResourceAsStream(template).readAllBytes(), template);
  }

  public record TemplateConfiguration(String template) {

  }

  public record PdfMap(String flow, Map<String, String> inputs, TemplateConfiguration pdf) {

  }
}
