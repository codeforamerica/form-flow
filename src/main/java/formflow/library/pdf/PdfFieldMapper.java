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

  /**
   * Maps SubmissionFields to PdfFields, taking into account if they are a SingleField or a CheckboxField type of
   * SubmissionField.
   *
   * @param submissionFields List of SubmissionFields containing user input data
   * @param flow             The flow that we should get the PdfMap of to use in mapping
   * @return List of PdfFields containing a PdfField and it's data value
   */
  public List<PdfField> map(List<SubmissionField> submissionFields, String flow) {
    PdfMap pdfMap = getPdfMap(flow);
    return submissionFields.stream()
        .map(submissionField -> makePdfFieldsForInput(submissionField, pdfMap))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @NotNull
  private List<PdfField> makePdfFieldsForInput(SubmissionField submissionField, PdfMap pdfMap) {
    try {
      return switch (submissionField.getClass().getSimpleName()) {
        case "SingleField" -> List.of(mapSingleFieldFromFlow((SingleField) submissionField, pdfMap));
        case "CheckboxField" -> mapMultiValueFieldFromFlow((CheckboxField) submissionField, pdfMap);
        default -> List.of(mapDatabaseFieldFromFlow((DatabaseField) submissionField, pdfMap));
      };
    } catch (Exception e) {
      log.warn("No field matches %s in pdf-map.yaml config, value could not be mapped!".formatted(submissionField.getName()));
      return List.of();
    }
  }

  @NotNull
  private PdfField mapDatabaseFieldFromFlow(DatabaseField input, PdfMap pdfMap) {
    Map<String, Object> pdfDbMapForFlow = pdfMap.getDbFields();
    String pdfFieldName = pdfDbMapForFlow.get(input.getName()).toString();
    return new PdfField(pdfFieldName, input.getValue());
  }

  @NotNull
  private PdfField mapSingleFieldFromFlow(SingleField input, PdfMap pdfMap) {
    Map<String, Object> pdfInputsMap = pdfMap.getAllFields();
    String submissionFieldName = input.getNameWithIteration();
    String pdfFieldName = pdfInputsMap.get(submissionFieldName).toString();

    return new PdfField(pdfFieldName, input.getValue());
  }

  @NotNull
  private List<PdfField> mapMultiValueFieldFromFlow(CheckboxField input, PdfMap pdfMap) {
    Map<String, Object> pdfInputsMap = pdfMap.getAllFields();
    String submissionFieldName = input.getNameWithIteration();

    Map<String, String> pdfFieldMap = (Map<String, String>) pdfInputsMap.get(submissionFieldName);

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
