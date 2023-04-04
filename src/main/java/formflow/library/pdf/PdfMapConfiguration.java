package formflow.library.pdf;

import java.util.Map;
import lombok.Data;

@Data
public class PdfMapConfiguration {

  String flow;

  Map<String, String> inputs;

}
