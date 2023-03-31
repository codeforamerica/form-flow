package formflow.library.pdf;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class PdfFieldMapper {

  private final Map<String, Map<String, List<String>>> pdfFieldMap;

  public PdfFieldMapper(Map<String, Map<String, List<String>>> pdfFieldMap) {
    this.pdfFieldMap = pdfFieldMap; // ubi -> inputs -> applicantFirstName: APPLICANT_FIRST_NAME
  }

  public List<PdfField> map(List<DocumentField> documentFields) {
    return documentFields.stream()
        .filter(input -> !input.getValue().isEmpty())
        .flatMap(this::makePdfFieldsForInput)
        .filter(pdfField -> pdfField.getName() != null)
        .collect(Collectors.toList());
  }

  @NotNull
  private Stream<? extends PdfField> makePdfFieldsForInput(DocumentField input) {
    return switch (input.getType()) {
      case DATE_VALUE -> simplePdfFields(input, this::getDateFormattedValue);
      case ENUMERATED_MULTI_VALUE -> binaryPdfFields(input);
      case UNUSED -> Stream.of();
      default -> simplePdfFields(input, this::getOrDefaultInputValue);
    };
  }

  @NotNull
  private Stream<SimplePdfField> simplePdfFields(DocumentField input,
      Function<DocumentField, String> valueMapper) {
    return input.getPdfName(pdfFieldMap).stream()
        .map(pdfName -> new SimplePdfField(pdfName, valueMapper.apply(input)));
  }

  @NotNull
  private Stream<BinaryPdfField> binaryPdfFields(DocumentField input) {
    return input.getValue().stream()
        .map(value -> new BinaryPdfField(input.getMultiValuePdfName(pdfFieldMap, value)));
  }

  @NotNull
  private String getDateFormattedValue(DocumentField input) {
    return String.join("/", input.getValue());
  }

  @NotNull
  private String getOrDefaultInputValue(DocumentField input) {
    return enumMap.getOrDefault(input.getValue(0), input.getValue(0));
  }
}
