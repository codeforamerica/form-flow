package formflow.library.pdf;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PdfMap {

  private final List<PdfMapConfiguration> maps;

  public PdfMap(List<PdfMapConfiguration> maps) {
    this.maps = maps;
  }

  public List<PdfMapConfiguration> getMaps() {
    return maps;
  }
}
