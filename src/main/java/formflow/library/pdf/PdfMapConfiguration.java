package formflow.library.pdf;

import java.util.HashMap;
import lombok.Data;

@Data
public class PdfMapConfiguration {

  String flow;

  HashMap<String, String> inputs;

}
