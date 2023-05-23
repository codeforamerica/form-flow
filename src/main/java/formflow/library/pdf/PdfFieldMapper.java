package formflow.library.pdf;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
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
    try {
      return switch (submissionField.getClass().getSimpleName()) {
        case "SingleField" -> List.of(mapSingleFieldFromFlow((SingleField) submissionField, flow));
        case "CheckboxField" -> mapMultiValueFieldFromFlow((CheckboxField) submissionField, flow);
        default -> List.of(mapDatabaseFieldFromFlow((DatabaseField) submissionField, flow));
      };
    } catch (Exception e) {
      log.warn("No field matches %s in pdf-map.yaml config, value could not be mapped!".formatted(submissionField.getName()));
      return List.of();
    }
  }

  @NotNull
  private PdfField mapDatabaseFieldFromFlow(DatabaseField input, String flow) {
    Map<String, Object> pdfDbMapForFlow = getPdfMap(flow).getDbFields();
    String pdfFieldName = pdfDbMapForFlow.get(input.getName()).toString();
    return new PdfField(pdfFieldName, input.getValue());
  }

  @NotNull
  private PdfField mapSingleFieldFromFlow(SingleField input, String flow) {
    Map<String, Object> pdfInputsMap = getPdfMap(flow).getInputFields();
    String pdfFieldName = pdfInputsMap.get(input.getName()).toString();
    return new PdfField(pdfFieldName, input.getValue());
  }

  @NotNull
  private List<PdfField> mapMultiValueFieldFromFlow(CheckboxField input, String flow) {
    Map<String, Object> pdfInputsMap = getPdfMap(flow).getInputFields();
    Map<String, String> pdfFieldMap = (Map<String, String>) pdfInputsMap.get(input.getName());

    return input.getValue().stream()
        .map(value -> new PdfField(pdfFieldMap.get(value), "Yes"))
        .collect(Collectors.toList());
  }

  private PdfMap getPdfMap(String flow) {
    return pdfMapConfigurations.stream()
        .filter(pdfMapConfig -> pdfMapConfig.getFlow().equals(flow))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No PDF configuration found for flow: " + flow));
  }
}
