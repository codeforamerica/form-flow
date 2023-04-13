package formflow.library.pdf;

import java.util.Map;
import lombok.Data;

@Data
public class PdfMapConfiguration {

  String flow;
  String pdf;
  Map<String, String> inputs;
}
