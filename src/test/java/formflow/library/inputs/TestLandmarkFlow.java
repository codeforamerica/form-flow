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
public class TestLandmarkFlow extends FlowInputs {

  @NotBlank(message = "{validations.make-sure-to-provide-a-first-name}")
  String firstName;
}
