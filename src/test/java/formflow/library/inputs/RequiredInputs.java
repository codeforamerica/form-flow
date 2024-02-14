package formflow.library.inputs;

import formflow.library.data.FlowInputs;
import formflow.library.data.annotations.Money;
import formflow.library.data.annotations.Phone;
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
public class RequiredInputs extends FlowInputs {
  
  @NotBlank
  String textInput;
  @NotBlank
  String areaInput;
  @NotBlank
  String dateDay;
  String dateMonth;
  String dateYear;
  @NotBlank(message = "Date may not be empty")
  @Pattern(regexp = "\\d/\\d/\\d\\d\\d\\d", message = "Date must be in the format of mm/dd/yyyy")
  String dateFull;
  @NotBlank
  String numberInput;
  @NotEmpty
  ArrayList<String> checkboxSet;
  @NotBlank
  String radioInput;
  @NotBlank
  String selectInput;
  @NotBlank
  String moneyInput;
  @NotBlank
  String phoneInput;
  @NotBlank
  String ssnInput;
}
