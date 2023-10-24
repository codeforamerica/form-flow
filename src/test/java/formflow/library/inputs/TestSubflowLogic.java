package formflow.library.inputs;

import formflow.library.data.FlowInputs;
import formflow.library.data.validators.Money;
import formflow.library.data.validators.Phone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.multipart.MultipartFile;

@TestConfiguration
@SuppressWarnings("unused")
public class TestSubflowLogic extends FlowInputs {

  @NotBlank(message = "{validations.make-sure-to-provide-a-first-name}")
  String firstName;

  String textInput;
  String areaInput;
  String dateDay;
  String dateMonth;
  String dateYear;
  @NotBlank(message = "Date may not be empty")
  @Pattern(regexp = "\\d/\\d/\\d\\d\\d\\d", message = "Date must be in the format of mm/dd/yyyy")
  String dateFull;

  String numberInput;
  ArrayList<String> checkboxSet;
  ArrayList<String> checkboxInput;
  String radioInput;
  String selectInput;
  String moneyInput;
  String phoneInput;
  @Encrypted
  String ssnInput;
  @Encrypted
  String ssnInputSubflow;
  String stateInput;
  @NotEmpty(message = "Please select at least one")
  List<String> favoriteFruitCheckbox;

  @NotBlank(message = "Don't leave this blank")
  @Size(min = 2, message = "You must enter a value 2 characters or longer")
  String inputWithMultipleValidations;

  @NotBlank(message = "Enter a value")
  String inputWithSingleValidation;

  // now lets test some fields in a subflow
  String firstNameSubflow;
  @NotBlank
  String textInputSubflow;
  @NotBlank
  String areaInputSubflow;
  @NotBlank
  String dateSubflowDay;
  @NotBlank
  String dateSubflowMonth;
  @NotBlank
  String dateSubflowYear;
  @NotBlank(message = "Date may not be empty")
  @Pattern(regexp = "\\d/\\d/\\d\\d\\d\\d", message = "Date must be in the format of mm/dd/yyyy")
  String dateSubflowFull;

  @NotBlank
  @Max(value = 100)
  String numberInputSubflow;
  @NotEmpty
  ArrayList<String> checkboxSetSubflow;
  @NotEmpty
  ArrayList<String> checkboxInputSubflow;
  @NotBlank
  String radioInputSubflow;
  @NotBlank
  String selectInputSubflow;
  @NotBlank
  @Money
  String moneyInputSubflow;
  @NotBlank
  @Phone
  String phoneInputSubflow;
  String doYouWantToEnterTheTestSubflow;
}
