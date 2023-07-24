package formflow.library.validation.address;

import com.smartystreets.api.exceptions.SmartyException;
import com.smartystreets.api.us_street.Batch;
import com.smartystreets.api.us_street.Client;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import formflow.library.inputs.UnvalidatedField;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class AddressValidationService {

  private final ValidationRequestFactory validationRequestFactory;
  private final SmartyClientFactory smartyClientFactory;

  private final String authId;
  private final String authToken;
  private final String license;
  private final boolean isDisabled;


  public AddressValidationService(
      ValidationRequestFactory validationRequestFactory,
      SmartyClientFactory smartyClientFactory,
      @Value("${form-flow.address-validation.smarty.auth-id:}") String authId,
      @Value("${form-flow.address-validation.smarty.auth-token:}") String authToken,
      @Value("${form-flow.address-validation.smarty.license:}") String license,
      @Value("${form-flow.address-validation.disabled:false}") boolean isDisabled) {
    this.validationRequestFactory = validationRequestFactory;
    this.smartyClientFactory = smartyClientFactory;
    this.authId = authId;
    this.authToken = authToken;
    this.license = license;
    this.isDisabled = isDisabled;
  }

  public Map<String, ValidatedAddress> runValidationRequest(FormSubmission formSubmission)
      throws SmartyException, IOException, InterruptedException {

    if (isDisabled) {
      return Map.of();
    }
    Batch smartyBatch = validationRequestFactory.create(formSubmission);
    Client client = smartyClientFactory.create(authId, authToken, license);
    client.send(smartyBatch);

    Map<String, ValidatedAddress> validatedAddresses = new HashMap<>();
    smartyBatch.getAllLookups().forEach(lookup -> {
      if (lookup.getResult().isEmpty()) {
        validatedAddresses.put(lookup.getInputId(), null);
      } else {
        String secondaryNumber = lookup.getResult(0).getComponents().getSecondaryNumber() == null ? ""
            : lookup.getResult(0).getComponents().getSecondaryNumber();
        String zipPlus4 = lookup.getResult(0).getComponents().getPlus4Code() == null ? ""
            : "-" + lookup.getResult(0).getComponents().getPlus4Code();
        validatedAddresses.put(
            lookup.getInputId(),
            new ValidatedAddress(
                lookup.getResult(0).getDeliveryLine1(),
                secondaryNumber,
                lookup.getResult(0).getComponents().getCityName(),
                lookup.getResult(0).getComponents().getState(),
                lookup.getResult(0).getComponents().getZipCode() + zipPlus4
            ));
      }
    });

    return validatedAddresses;
  }

  /**
   * Runs address validation on the form submission data, but only if there is an address present in the form submission that
   * validation is requested for. This also clears out any fields in the submission that are related to the validated version of
   * that were previously set.
   *
   * @param submission     Submission data from the database
   * @param formSubmission Form data from current POST
   */
  public void validate(Submission submission, FormSubmission formSubmission)
      throws SmartyException, IOException, InterruptedException {
    List<String> addressValidationFields = formSubmission.getAddressValidationFields();
    if (!addressValidationFields.isEmpty()) {
      Map<String, ValidatedAddress> validatedAddresses = runValidationRequest(formSubmission);
      formSubmission.setValidatedAddress(validatedAddresses);
      formSubmission.getAddressValidationFields().forEach(item -> {
        String inputName = item.replace(UnvalidatedField.VALIDATE_ADDRESS, "");
        submission.clearAddressFields(inputName);
      });
    }
  }
}
