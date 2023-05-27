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
      if (subflowInfo != null) {
        allFields.putAll(getAllSubflowFields());
      }
      if (dbFields != null) {
        allFields.putAll(dbFields);
      }

    }
    return allFields;
  }

  /**
   * Fetches and returns all the fields for all the subflows, expanding out the fields based on the number of max iterations the
   * PdfMap indicates are necessary
   * <p>
   * The return data will contain all the subflow fields to be accounted for flat-mapped to their values.  The fields for a
   * specific iteration of a subflow will contain all the iteration's fields suffixed with a "_" and an iteration number. This *
   * helps keep all the subflow together while we flatten the data.
   * </p>
   *
   * @return
   */
  public Map<String, Object> getAllSubflowFields() {
    Map<String, Object> subflowFields = new HashMap<>();

    subflowInfo.forEach((subflowName, subflow) -> {
      subflowFields.putAll(subflow.getFieldsForIterations());
    });

    return subflowFields;
  }
}
