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

  private Client smartyClient;

  private ValidationRequestFactory validationRequestFactory;
  @Value("${form-flow.address-validation.smarty.auth-id}")
  private String authId;
  @Value("${form-flow.address-validation.smarty.auth-token}")
  private String authToken;

  public AddressValidationService(Client smartyClient, ValidationRequestFactory validationRequestFactory, String authId,
      String authToken) {
    this.smartyClient = smartyClient;
    this.validationRequestFactory = validationRequestFactory;
    this.authId = authId;
    this.authToken = authToken;
  }

  public Map<String, ValidatedAddress> validate(FormSubmission formSubmission)
      throws SmartyException, IOException, InterruptedException {
    Batch smartyBatch = validationRequestFactory.create(formSubmission);
//    TODO: add authId and authToken to the client
    smartyClient.send(smartyBatch);

    Map<String, ValidatedAddress> validatedAddresses = new HashMap<>();
    smartyBatch.getAllLookups().forEach(lookup ->
        validatedAddresses.put(
            lookup.getInputId(),
            new ValidatedAddress(
                lookup.getResult(0).getDeliveryLine1(),
                lookup.getResult(0).getComponents().getCityName(),
                lookup.getResult(0).getComponents().getState(),
                lookup.getResult(0).getComponents().getZipCode())));

    return validatedAddresses;
  }
}