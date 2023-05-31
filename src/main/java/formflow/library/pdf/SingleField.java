package formflow.library.pdf;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SingleField extends SubmissionField {

  @ToString.Include
  @EqualsAndHashCode.Include
  @NotNull String value;

  public SingleField(String name, @NotNull String value, Integer iteration) {
    super(name, iteration);
    this.value = value;
  }
}

