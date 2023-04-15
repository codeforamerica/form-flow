package formflow.library.pdf;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdfMap {
  String flow;
  String pdf;
  Map<String, String> inputs;
}
