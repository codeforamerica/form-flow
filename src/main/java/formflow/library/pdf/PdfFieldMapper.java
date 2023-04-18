package formflow.library.pdf;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
        .filter(input -> !input.getValue().isEmpty())
        .map(documentField -> makePdfFieldsForInput(documentField, flow))
        .filter(pdfField -> pdfField.name() != null)
        .collect(Collectors.toList());
  }

  @NotNull
  private PdfField makePdfFieldsForInput(SubmissionField input, String flow) {
    return switch (input.getType()) {
//      case DATE_VALUE -> simplePdfFields(input, this::getDateFormattedValue);
//      case ENUMERATED_MULTI_VALUE -> binaryPdfFields(input);
//      case UNUSED -> Stream.of();
      default -> mapFieldFromFlow(input, this::getOrDefaultInputValue, flow);
    };
  }

  @NotNull
  private PdfField mapFieldFromFlow(SubmissionField input,
      Function<SubmissionField, String> valueMapper, String flow) {

    Map<String, Object> pdfInputsMap = pdfMapConfigurations.stream()
        .filter(pdfMapConfig -> pdfMapConfig.getFlow().equals(flow))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No PDF configuration found for flow: " + flow))
        .getInputs();

    String pdfFieldName = input.getFieldNameForPdf(pdfInputsMap);

    return new PdfField(pdfFieldName, valueMapper.apply(input));
  }

  @NotNull
  private String getDateFormattedValue(SubmissionField input) {
    return String.join("/", input.getValue());
  }

  @NotNull
  private String getOrDefaultInputValue(SubmissionField input) {
    switch (input.getValue()) {
      case "true":
        return "Yes";
      case "false":
        return "No";
      default:
        return input.getValue();
    }
  }
}
