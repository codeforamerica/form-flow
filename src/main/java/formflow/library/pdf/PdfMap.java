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

  public void setInputFields(Map<String, Object> inputFields) {
    this.inputFields = inputFields;
    updateAllFields();
  }

  public void setDbFields(Map<String, Object> dbFields) {
    this.dbFields = dbFields;
    updateAllFields();
  }

  public void setSubflowInfo(Map<String, PdfMapSubflow> subflowInfo) {
    this.subflowInfo = subflowInfo;
    updateAllFields();
  }

  private void updateAllFields() {
    if (allFields == null) {
      allFields = new HashMap<>();
    } else {
      allFields.clear();
    }
    if (inputFields != null) {
      allFields.putAll(inputFields);
    }
    if (dbFields != null) {
      allFields.putAll(dbFields);
    }
    if (subflowInfo != null) {
      allFields.putAll(getAllSubflowFields());
    }
  }
 /*
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
  }*/

  /**
   * Fetches and returns all the fields for all the subflows, expanding out the fields based on the number of max iterations the
   * PdfMap indicates are necessary.
   *
   * @return All the subflow fields from each subflow iteration flat-mapped to their values.  Each subflow iteration will be
   * flat-mapped to the iteration's fields suffixed with a "_" and an iteration number. This helps keep all the subflow iterations
   * together while we flatten the data.
   */
  public Map<String, Object> getAllSubflowFields() {
    Map<String, Object> subflowFields = new HashMap<>();

    subflowInfo.forEach((subflowName, subflow) -> {
      subflowFields.putAll(subflow.getFieldsForIterations());
    });

    return subflowFields;
  }
}
