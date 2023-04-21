package formflow.library.pdf;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class DocumentField {

  String name;
  @NotNull
  List<String> value;
  DocumentFieldType type;
  Integer iteration;

  public DocumentField(String name, @NotNull List<String> value,
      DocumentFieldType type, Integer iteration) {
    this.name = name;
    this.value = value;
    this.type = type;
    this.iteration = iteration;
  }

  public DocumentField(String name, @NotNull List<String> value,
      DocumentFieldType type) {
    this(name, value, type, null);
  }

  // Make an application input with only a single value
  public DocumentField(String name, String value, DocumentFieldType type) {
    this(name, value, type, null);
  }

  // Make an application input for an iteration with only a single value
  public DocumentField(String name, String value, DocumentFieldType type,
      Integer iteration) {
    this(name, value == null ? emptyList() : List.of(value), type, iteration);
  }

  public List<String> getPdfName(Map<String, List<String>> pdfFieldMap) {
    List<String> names = pdfFieldMap.get(String.join(".", this.getGroupName(), this.getName()));
    return this.getNameWithIteration(names);
  }

  public String getMultiValuePdfName(Map<String, List<String>> pdfFieldMap, String value) {
    List<String> names = pdfFieldMap
        .get(String.join(".", this.getGroupName(), this.getName(), value));
    if (getNameWithIteration(names).size() > 0) {
      return getNameWithIteration(names).get(0);
    } else {
      return null;
    }
  }

  private List<String> getNameWithIteration(List<String> names) {
    if (names == null) {
      return emptyList();
    }

    return names.stream()
        .map(name -> this.getIteration() != null ? name + "_" + this.getIteration() : name)
        .collect(Collectors.toList());
  }

  public String getValue(int i) {
    return getValue().get(i);
  }
}
