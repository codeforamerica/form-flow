package formflow.library.address_validation;


import com.smartystreets.api.exceptions.BatchFullException;
import com.smartystreets.api.us_street.Batch;
import com.smartystreets.api.us_street.Lookup;
import formflow.library.data.FormSubmission;
import formflow.library.inputs.AddressParts;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS;

/**
 * Factory for creating batches of address validation requests. This class processes a FormSubmission and constructs a batch of
 * address lookups to be used with the SmartyStreets API.
 */
@Slf4j
@Component
public class ValidationRequestFactory {

  /**
   * Default constructor for ValidationRequestFactory.
   */
  public ValidationRequestFactory() {
  }

  /**
   * Creates a batch of address validation lookups from a given FormSubmission. It filters the form data for address fields marked
   * for validation and constructs smarty lookups for each.
   *
   * @param formSubmission The FormSubmission containing the data to be validated.
   * @return A Batch object containing all the address lookups ready to be sent to SmartyStreets.
   * @throws RuntimeException If the number of lookups exceeds the maximum batch size limit.
   */
  public Batch create(FormSubmission formSubmission) {
    Batch smartyBatch = new Batch();
    List<String> addressInputNames = formSubmission.getFormData().keySet().stream()
        .filter(key ->
            key.startsWith(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS.toString()) &&
                formSubmission.getFormData().get(key).equals("true")
        )
        .map(key -> key.substring(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS.toString().length())).toList();

    addressInputNames.forEach(inputName -> {
      Lookup lookup = new Lookup();
      lookup.setInputId(inputName);
      lookup.setStreet(formSubmission.getFormData().get(inputName + AddressParts.STREET_ADDRESS_1).toString());
      lookup.setStreet2(formSubmission.getFormData().get(inputName + AddressParts.STREET_ADDRESS_2).toString());
      lookup.setCity(formSubmission.getFormData().get(inputName + AddressParts.CITY).toString());
      lookup.setState(formSubmission.getFormData().get(inputName + AddressParts.STATE).toString());
      lookup.setZipCode(formSubmission.getFormData().get(inputName + AddressParts.ZIPCODE).toString());
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
