package formflow.library.inputs;

import formflow.library.data.FlowInputs;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
@SuppressWarnings("unused")
public class ConditionsTestFlow extends FlowInputs {

  @NotBlank(message = "{validations.make-sure-to-provide-a-first-name}")
  String firstName;

}
