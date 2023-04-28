package formflow.library.pdf;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
@Value
public class SingleField extends SubmissionField {

  @NotNull String value;
  Integer iteration;

  public SingleField(String name, @NotNull String value, Integer iteration) {
    super(name);
    this.value = value;
    this.iteration = iteration;
  }
}

