package formflow.library.pdf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdfMap {

  String flow;
  String pdf;
  Map<String, Object> inputFields;
  Map<String, Object> dbFields;
  Map<String, PdfMapSubflow> subflowInfo;

  public Map<String, Object> getAllSubflowFields() {
    Map<String, Object> subflowFields = new HashMap<>();

    subflowInfo.forEach((subflowName, subflow) -> {
      subflowFields.putAll(subflow.getFieldsForIterations());
    });

    return subflowFields;
  }


}
