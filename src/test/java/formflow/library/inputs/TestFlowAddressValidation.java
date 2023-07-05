package formflow.library.inputs;

import formflow.library.data.FlowInputs;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
@SuppressWarnings("unused")
public class TestFlowAddressValidation extends FlowInputs {

  @NotBlank
  String validationOffStreetAddress1;
  String validationOffStreetAddress2;
  @NotBlank
  String validationOffCity;
  @NotBlank
  String validationOffState;
  @NotBlank
  String validationOffZipCode;

  @NotBlank
  String validationOnStreetAddress1;
  String validationOnStreetAddress2;
  @NotBlank
  String validationOnCity;
  @NotBlank
  String validationOnState;
  @NotBlank
  String validationOnZipCode;
  Boolean useValidatedValidationOn;
}
