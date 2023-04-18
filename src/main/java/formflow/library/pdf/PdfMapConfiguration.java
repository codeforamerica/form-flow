package formflow.library.pdf;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PdfMapConfiguration {

  private final List<PdfMap> maps;

  List<PdfMap> getMaps() {
    return maps;
  }

  public PdfMapConfiguration(List<PdfMap> maps) {
    this.maps = maps;
  }

  public ApplicationFile getPdfFromFlow(String flow) throws IOException {
    PdfMap pdfConfig = getPdfMap(flow);

    String pdfPath = pdfConfig.getPdf().startsWith("/") ? pdfConfig.getPdf() : "/" + pdfConfig.getPdf();

    return new ApplicationFile(requireNonNull(getClass().getResourceAsStream(pdfPath)).readAllBytes(),
        pdfConfig.getPdf());
  }

  public PdfMap getPdfMap(String flow) throws IOException {
    return maps.stream().filter(config -> config.getFlow().equals(flow))
        .findFirst().orElseThrow(IOException::new);
  }
}
