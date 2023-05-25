package formflow.library.pdf;

import java.util.HashMap;
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
  Map<String, Object> inputFields;
  Map<String, Object> dbFields;
  Map<String, PdfMapSubflow> subflowInfo;

  Map<String, Object> allFields;

  public Map<String, Object> getAllFields() {
    if (allFields == null) {
      allFields = new HashMap<>();
      allFields.putAll(inputFields);
      allFields.putAll(dbFields);
      allFields.putAll(getAllSubflowFields());
    }
    return allFields;
  }

  public Map<String, Object> getAllSubflowFields() {
    Map<String, Object> subflowFields = new HashMap<>();

    subflowInfo.forEach((subflowName, subflow) -> {
      subflowFields.putAll(subflow.getFieldsForIterations());
    });

    return subflowFields;
  }
}
