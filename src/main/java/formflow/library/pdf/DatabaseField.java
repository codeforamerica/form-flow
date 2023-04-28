package formflow.library.pdf;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
@Value
public class DatabaseField extends SubmissionField {

  @NotNull String value;

  public DatabaseField(String name, @NotNull String value) {
    super(name);
    this.value = value;
  }

}
