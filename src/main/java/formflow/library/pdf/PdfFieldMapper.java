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

  public List<PdfField> map(List<SingleField> singleFields, String flow) {
    return singleFields.stream()
        .filter(input -> !input.value().isEmpty())
        .map(documentField -> makePdfFieldsForInput(documentField, flow))
        .filter(pdfField -> pdfField.name() != null)
        .collect(Collectors.toList());
  }

  @NotNull
  private PdfField makePdfFieldsForInput(SingleField input, String flow) {
    return switch (input.type()) {
//      case DATE_VALUE -> simplePdfFields(input, this::getDateFormattedValue);
//      case ENUMERATED_MULTI_VALUE -> binaryPdfFields(input);
//      case UNUSED -> Stream.of();
      default -> mapFieldFromFlow(input, this::getOrDefaultInputValue, flow);
    };
  }

  @NotNull
  private PdfField mapFieldFromFlow(SingleField input,
      Function<SingleField, String> valueMapper, String flow) {

    Map<String, Object> pdfInputsMap = pdfMapConfigurations.stream()
        .filter(pdfMapConfig -> pdfMapConfig.getFlow().equals(flow))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No PDF configuration found for flow: " + flow))
        .getInputs();

    String pdfFieldName = pdfInputsMap.get(input.name()).toString();

    return new PdfField(pdfFieldName, valueMapper.apply(input));
  }

  @NotNull
  private String getDateFormattedValue(SingleField input) {
    return String.join("/", input.value());
  }

  @NotNull
  private String getOrDefaultInputValue(SingleField input) {
    switch (input.value()) {
      case "true":
        return "Yes";
      case "false":
        return "No";
      default:
        return input.value();
    }
  }
}
