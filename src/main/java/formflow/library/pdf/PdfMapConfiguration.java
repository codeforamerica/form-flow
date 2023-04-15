package formflow.library.pdf;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PdfMapConfiguration {

  private final List<PdfMap> maps;

  public PdfMapConfiguration(List<PdfMap> maps) {
    this.maps = maps;
  }

  public ApplicationFile getPdfFromFlow(String flow) throws IOException {
    PdfMap pdfConfig = maps.stream().filter(config -> config.getFlow().equals(flow))
        .findFirst().orElseThrow(IOException::new);

    return new ApplicationFile(requireNonNull(getClass().getResourceAsStream(pdfConfig.getPdf())).readAllBytes(),
        pdfConfig.getPdf());
  }
}
