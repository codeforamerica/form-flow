package formflow.library.address_validation;


import com.smartystreets.api.exceptions.BatchFullException;
import com.smartystreets.api.us_street.Batch;
import com.smartystreets.api.us_street.Lookup;
import formflow.library.data.FormSubmission;
import formflow.library.inputs.UnvalidatedField;
import formflow.library.inputs.Inputs;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ValidationRequestFactory {

  public ValidationRequestFactory() {
  }

  public Batch create(FormSubmission formSubmission) {
    Batch smartyBatch = new Batch();
    List<String> addressInputNames = formSubmission.getFormData().keySet().stream()
        .filter(key -> key.startsWith(UnvalidatedField.VALIDATE_ADDRESS) && formSubmission.getFormData().get(key).equals("true"))
        .map(key -> key.substring(UnvalidatedField.VALIDATE_ADDRESS.length())).toList();

    addressInputNames.forEach(inputName -> {
      Lookup lookup = new Lookup();
      lookup.setInputId(inputName);
      lookup.setStreet(formSubmission.getFormData().get(inputName + Inputs.STREET_ADDRESS_1).toString());
      lookup.setStreet2(formSubmission.getFormData().get(inputName + Inputs.STREET_ADDRESS_2).toString());
      lookup.setCity(formSubmission.getFormData().get(inputName + Inputs.CITY).toString());
      lookup.setState(formSubmission.getFormData().get(inputName + Inputs.STATE).toString());
      lookup.setZipCode(formSubmission.getFormData().get(inputName + Inputs.ZIPCODE).toString());
      try {
        smartyBatch.add(lookup);
      } catch (BatchFullException e) {
        log.error("You have exceeded the maximum number of lookups (100) in a batch!");
        throw new RuntimeException(e);
      }
    });
    return smartyBatch;
  }
}
