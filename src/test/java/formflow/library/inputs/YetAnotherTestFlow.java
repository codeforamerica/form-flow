package formflow.library.inputs;

import formflow.library.data.FlowInputs;
import formflow.library.data.validators.Money;
import formflow.library.data.validators.Phone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
@SuppressWarnings("unused")
public class YetAnotherTestFlow extends FlowInputs {
  
  String firstName;

  String textInput;
  String areaInput;
  String dateDay;
  String dateMonth;
  String dateYear;
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
  List<String> favoriteFruitCheckbox;

  @Size(min = 2, message = "You must enter a value 2 characters or longer")
  String inputWithMultipleValidations;

  String inputWithSingleValidation;

  // now lets test some fields in a subflow
  String firstNameSubflow;
  String textInputSubflow;
  String areaInputSubflow;
  String dateSubflowDay;
  String dateSubflowMonth;
  String dateSubflowYear;
  @Pattern(regexp = "\\d/\\d/\\d\\d\\d\\d", message = "Date must be in the format of mm/dd/yyyy")
  String dateSubflowFull;

  @Max(value = 100)
  String numberInputSubflow;
  ArrayList<String> checkboxSetSubflow;
  ArrayList<String> checkboxInputSubflow;
  String radioInputSubflow;
  String selectInputSubflow;
  @Money
  String moneyInputSubflow;
  @Phone
  String phoneInputSubflow;

  String firstNameSubflowPage2;
  String textInputSubflowPage2;
  String areaInputSubflowPage2;
  String dateSubflowPage2Day;
  String dateSubflowPage2Month;
  String dateSubflowPage2Year;
  @Pattern(regexp = "\\d/\\d/\\d\\d\\d\\d", message = "Date must be in the format of mm/dd/yyyy")
  String dateSubflowPage2Full;
  @Max(value = 100)
  String numberInputSubflowPage2;
  ArrayList<String> checkboxSetSubflowPage2;
  ArrayList<String> checkboxInputSubflowPage2;
  String radioInputSubflowPage2;
  String selectInputSubflowPage2;
  @Money
  String moneyInputSubflowPage2;
  @Phone
  String phoneInputSubflowPage2;
  String doYouWantToEnterTheTestSubflow;
}
