package formflow.library.pdf;

import java.util.Optional;

public record PdfField(String name, String value) {

  public PdfField(String name, String value) {
    this.name = name;
    this.value = Optional.ofNullable(value).orElse("");
  }

}
