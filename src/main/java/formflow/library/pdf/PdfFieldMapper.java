package formflow.library.pdf;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class PdfFieldMapper {

  private final List<PdfMap> pdfMapConfigurations;

  public PdfFieldMapper(List<PdfMap> pdfMapConfigurations) {
    this.pdfMapConfigurations = pdfMapConfigurations;
  }

  public List<PdfField> map(List<SubmissionField> submissionFields, String flow) {
    return submissionFields.stream()
        .map(submissionField -> makePdfFieldsForInput(submissionField, flow))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @NotNull
  private List<PdfField> makePdfFieldsForInput(SubmissionField submissionField, String flow) {
    return switch (submissionField.getClass().getSimpleName()) {
      case "SingleField" -> List.of(mapSingleFieldFromFlow((SingleField) submissionField, flow));
      case "CheckboxField" -> mapMultiValueFieldFromFlow((CheckboxField) submissionField, flow);
      default -> List.of(mapDatabaseFieldFromFlow((DatabaseField) submissionField, flow));
    };
  }

  @NotNull
  private PdfField mapDatabaseFieldFromFlow(DatabaseField input, String flow) {
    Map<String, Object> pdfDbMapForFlow = getPdfDbMapForFlow(flow);
    String pdfFieldName = pdfDbMapForFlow.get(input.getName()).toString();
    return new PdfField(pdfFieldName, input.getValue());
  }

  @NotNull
  private PdfField mapSingleFieldFromFlow(SingleField input, String flow) {
    Map<String, Object> pdfInputsMap = getPdfInputMapForFlow(flow);
    String pdfFieldName = pdfInputsMap.get(input.getName()).toString();
    return new PdfField(pdfFieldName, input.getValue());
  }

  @NotNull
  private List<PdfField> mapMultiValueFieldFromFlow(CheckboxField input, String flow) {
    Map<String, Object> pdfInputsMap = getPdfInputMapForFlow(flow);

    Map<String, String> pdfFieldMap = (Map<String, String>) pdfInputsMap.get(input.getName());

    return input.getValue().stream()
        .map(value -> new PdfField(pdfFieldMap.get(value), "Yes"))
        .collect(Collectors.toList());
  }

  private Map<String, Object> getPdfInputMapForFlow(String flow) {
    return pdfMapConfigurations.stream()
        .filter(pdfMapConfig -> pdfMapConfig.getFlow().equals(flow))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No PDF configuration found for flow: " + flow))
        .getInputFields();
  }

  private Map<String, Object> getPdfDbMapForFlow(String flow) {
    return pdfMapConfigurations.stream()
        .filter(pdfMapConfig -> pdfMapConfig.getFlow().equals(flow))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No PDF configuration found for flow: " + flow))
        .getDbFields();
  }
}
