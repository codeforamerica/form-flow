package formflow.library.address_validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartystreets.api.exceptions.SmartyException;
import com.smartystreets.api.us_street.Batch;
import com.smartystreets.api.us_street.Candidate;
import com.smartystreets.api.us_street.Client;
import com.smartystreets.api.us_street.Components;
import com.smartystreets.api.us_street.Lookup;
import formflow.library.data.FormSubmission;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AddressValidationServiceTest {

  ValidationRequestFactory validationRequestFactory = mock(ValidationRequestFactory.class);
  Client client = mock(Client.class);
  ClientFactory clientFactory = mock(ClientFactory.class);

  @Test
  void validateShouldCallSmartyToValidateAddress() throws SmartyException, IOException, InterruptedException {
    FormSubmission formSubmission = new FormSubmission(Map.of());
    String authId = "authId";
    String authToken = "authToken";
    String license = "license";

    AddressValidationService addressValidationService = new AddressValidationService(
        validationRequestFactory,
        clientFactory,
        authId,
        authToken,
        license
    );
    Lookup lookup = mock(Lookup.class);
    Candidate candidate = mock(Candidate.class);
    Components components = mock(Components.class);

    Batch batch = new Batch();
    when(lookup.getInputId()).thenReturn("validatedInput");
    when(candidate.getComponents()).thenReturn(components);
    when(candidate.getDeliveryLine1()).thenReturn("validatedStreetAddress");
    when(components.getCityName()).thenReturn("validatedCity");
    when(components.getState()).thenReturn("validatedState");
    when(components.getZipCode()).thenReturn("validatedZipCode");
    when(lookup.getResult(0)).thenReturn(candidate);
    batch.add(lookup);

    when(validationRequestFactory.create(formSubmission)).thenReturn(batch);
    when(clientFactory.create(authId, authToken, license)).thenReturn(client);

    assertThat(addressValidationService.validate(formSubmission)).isEqualTo(Map.of(
        "validatedInput",
        new ValidatedAddress("validatedStreetAddress",
            "validatedCity",
            "validatedState",
            "validatedZipCode")
    ));
    verify(client, times(1)).send(batch);
  }
}