package formflow.library.pdf;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class CheckboxField extends SubmissionField {

  List<String> value;
  Integer iteration;

  public CheckboxField(String name, List<String> value, Integer iteration) {
    super(name);
    this.value = value;
    this.iteration = iteration;
  }
}
