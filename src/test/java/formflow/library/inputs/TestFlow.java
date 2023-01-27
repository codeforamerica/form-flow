package formflow.library.inputs;

import formflow.library.data.FlowInputs;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.multipart.MultipartFile;

@TestConfiguration
@SuppressWarnings("unused")
public class TestFlow extends FlowInputs {

  @NotBlank(message = "{validations.make-sure-to-provide-a-first-name}")
  String firstName;

  String textInput;
  String areaInput;
  String dateDay;
  String dateMonth;
  String dateYear;
  String numberInput;
  ArrayList<String> checkboxSet;
  ArrayList<String> checkboxInput;
  String radioInput;
  String selectInput;
  String moneyInput;
  String phoneInput;
  String ssnInput;
  @NotEmpty(message = "Please select at least one")
  List<String> favoriteFruitCheckbox;

  @NotBlank(message = "Don't leave this blank")
  @Size(min = 2, message = "You must enter a value 2 characters or longer")
  String inputWithMultipleValidations;

  String householdMemberFirstName;
  String householdMemberLastName;
  String householdMemberRelationship;
  String householdMemberRecentlyMovedToUS;
  MultipartFile testFile;
  String dropZoneTestInstance;

  @Positive()
  int validatePositiveIfNotEmpty;
}
