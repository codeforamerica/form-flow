package formflow.library.pdf;

import java.util.Map;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class SubmissionField {

  String name;
  @NotNull
  String value;
  SubmissionFieldValue type;
  Integer iteration;

  public SubmissionField(String name, @NotNull String value,
      SubmissionFieldValue type, Integer iteration) {
    this.name = name;
    this.value = value;
    this.type = type;
    this.iteration = iteration;
  }


  public String getFieldNameForPdf(Map<String, Object> pdfFieldMap) {
    return pdfFieldMap.get(this.getName()).toString();
  }

  public String getMultiValuePdfName(Map<String, String> pdfFieldMap, String value) {
    String name = pdfFieldMap
        .get(String.join(".", this.getName(), value));
    return getNameWithIteration(name);
  }

  private String getNameWithIteration(String name) {
    return this.getIteration() != null ? name + "_" + this.getIteration() : name;
  }
}
