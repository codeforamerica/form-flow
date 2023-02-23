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


@Service
public class AddressValidationService {

  private final ValidationRequestFactory validationRequestFactory;
  private final ClientFactory clientFactory;

  private final String authId;
  private final String authToken;
  private final String license;


  public AddressValidationService(
      ValidationRequestFactory validationRequestFactory,
      ClientFactory clientFactory, @Value("${form-flow.address-validation.smarty.auth-id}") String authId,
      @Value("${form-flow.address-validation.smarty.auth-token}") String authToken,
      @Value("${form-flow.address-validation.smarty.license}") String license) {
    this.validationRequestFactory = validationRequestFactory;
    this.clientFactory = clientFactory;
    this.authId = authId;
    this.authToken = authToken;
    this.license = license;
  }

  public Map<String, ValidatedAddress> validate(FormSubmission formSubmission)
      throws SmartyException, IOException, InterruptedException {
    Batch smartyBatch = validationRequestFactory.create(formSubmission);
    Client client = clientFactory.create(authId, authToken, license);

    client.send(smartyBatch);

    Map<String, ValidatedAddress> validatedAddresses = new HashMap<>();
    smartyBatch.getAllLookups().forEach(lookup -> {
      if (lookup.getResult().isEmpty()) {
        validatedAddresses.put(lookup.getInputId(), null);
      } else {
        validatedAddresses.put(
            lookup.getInputId(),
            new ValidatedAddress(
                lookup.getResult(0).getDeliveryLine1(),
                lookup.getResult(0).getComponents().getCityName(),
                lookup.getResult(0).getComponents().getState(),
                lookup.getResult(0).getComponents().getZipCode()));
      }
    });

    return validatedAddresses;
  }
}
