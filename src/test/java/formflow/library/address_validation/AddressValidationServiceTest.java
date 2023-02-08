package formflow.library.address_validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.smartystreets.api.exceptions.SmartyException;
import com.smartystreets.api.us_street.Client;
import com.smartystreets.api.us_street.Lookup;
import formflow.library.data.FormSubmission;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AddressValidationServiceTest {

  Client smartyClient = mock(Client.class);
  ValidationRequestFactory validationRequestFactory = mock(ValidationRequestFactory.class);

  @Test
  void validateShouldCallSmartyToValidateAddress() throws SmartyException, IOException, InterruptedException {
    AddressValidationService addressValidationService = new AddressValidationService(smartyClient, validationRequestFactory,
        "authId",
        "authToken");
//    Lookup lookup = mock(Lookup.class);
//    Candidate candidate = mock(Candidate.class);
//    Components components = mock(Components.class);
//
//    Batch batch = new Batch();
//    List<Candidate> response = List.of(candidate);
//    when(candidate.getComponents()).thenReturn(components);
//    when(candidate.getDeliveryLine1()).thenReturn("validatedStreetAddress");
//    when(components.getCityName()).thenReturn("validatedCity");
//    when(components.getState()).thenReturn("validatedState");
//    when(components.getZipCode()).thenReturn("validatedZipCode");
//    when(lookup.getResult()).thenReturn(response);
//    batch.add(lookup);

    assertThat(addressValidationService.validate(new FormSubmission(Map.of()))).isEqualTo(
        new ValidatedAddress("validatedStreetAddress",
            "validatedCity",
            "validatedState",
            "validatedZipCode"));

    verify(smartyClient, times(1)).send((Lookup) any());
  }
}