package formflow.library.address_validation;

import com.smartystreets.api.exceptions.SmartyException;
import com.smartystreets.api.us_street.Batch;
import com.smartystreets.api.us_street.Client;
import formflow.library.data.FormSubmission;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service class for validating addresses using the SmartyStreets API. This service handles the creation of validation requests,
 * interaction with the SmartyStreets client, and processing of the validation results.
 */
@Service
public class AddressValidationService {

  private final ValidationRequestFactory validationRequestFactory;
  private final ClientFactory clientFactory;

  private final String authId;
  private final String authToken;
  private final String license;
  private final boolean isEnabled;

  /**
   * Constructs an AddressValidationService with dependencies and configuration properties.
   *
   * @param validationRequestFactory Factory for creating validation requests.
   * @param clientFactory            Factory for creating SmartyStreets clients.
   * @param authId                   The authentication ID for the SmartyStreets API.
   * @param authToken                The authentication token for the SmartyStreets API.
   * @param license                  The license key for the SmartyStreets API.
   * @param isEnabled               Application property indicating if address validation is enabled.
   */
  public AddressValidationService(
      ValidationRequestFactory validationRequestFactory,
      ClientFactory clientFactory,
      @Value("${form-flow.address-validation.smarty.auth-id:}") String authId,
      @Value("${form-flow.address-validation.smarty.auth-token:}") String authToken,
      @Value("${form-flow.address-validation.smarty.license:}") String license,
      @Value("${form-flow.address-validation.enabled:true}") boolean isEnabled) {
    this.validationRequestFactory = validationRequestFactory;
    this.clientFactory = clientFactory;
    this.authId = authId;
    this.authToken = authToken;
    this.license = license;
    this.isEnabled = isEnabled;
  }

  /**
   * Validates addresses contained in a FormSubmission object. It returns a map where each key is an identifier of an address and
   * each value is the validated address. If address validation is disabled, it returns an empty map.
   *
   * @param formSubmission The form submission containing the addresses to be validated.
   * @return A map of identifier strings to ValidatedAddress objects.
   * @throws SmartyException      If there is an error in the SmartyStreets API call.
   * @throws IOException          If there is an I/O error.
   * @throws InterruptedException If the thread is interrupted during the process.
   */
  public Map<String, ValidatedAddress> validate(FormSubmission formSubmission)
      throws SmartyException, IOException, InterruptedException {

    if (!isEnabled) {
      return Map.of();
    }
    Batch smartyBatch = validationRequestFactory.create(formSubmission);
    Client client = clientFactory.create(authId, authToken, license);
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
}
