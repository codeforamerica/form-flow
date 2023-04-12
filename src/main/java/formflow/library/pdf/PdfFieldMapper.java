package formflow.library.pdf;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class PdfFieldMapper {

  private final PdfMapConfiguration pdfMapConfiguration;

  public PdfFieldMapper(PdfMapConfiguration pdfMapConfiguration) {
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  public List<PdfField> map(List<DocumentField> documentFields, String flow) {
    return documentFields.stream()
        .filter(input -> !input.getValue().isEmpty())
        .map(documentField -> makePdfFieldsForInput(documentField, flow))
        .filter(pdfField -> pdfField.name() != null)
        .collect(Collectors.toList());
  }

  @NotNull
  private PdfField makePdfFieldsForInput(DocumentField input, String flow) {
    return switch (input.getType()) {
//      case DATE_VALUE -> simplePdfFields(input, this::getDateFormattedValue);
//      case ENUMERATED_MULTI_VALUE -> binaryPdfFields(input);
//      case UNUSED -> Stream.of();
      default -> mapFieldFromFlow(input, this::getOrDefaultInputValue, flow);
    };
  }

  @NotNull
  private PdfField mapFieldFromFlow(DocumentField input,
      Function<DocumentField, String> valueMapper, String flow) {
    Map<String, String> pdfInputsMap = pdfMapConfiguration.getMap(flow).inputs();

    String pdfInputName = input.getPdfName(pdfInputsMap);

    return new PdfField(pdfInputName, valueMapper.apply(input));
  }

  @NotNull
  private String getDateFormattedValue(DocumentField input) {
    return String.join("/", input.getValue());
  }

  @NotNull
  private String getOrDefaultInputValue(DocumentField input) {
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
