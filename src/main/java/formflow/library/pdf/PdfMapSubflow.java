package formflow.library.pdf;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdfMapSubflow {

  List<String> subflows;
  int totalIterations;
  String dataAction;
  Map<String, Object> fields;
}
