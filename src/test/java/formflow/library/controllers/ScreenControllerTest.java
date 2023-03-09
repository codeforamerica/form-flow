package formflow.library.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import formflow.library.address_validation.AddressValidationService;
import formflow.library.address_validation.ValidatedAddress;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
public class ScreenControllerTest extends AbstractMockMvcTest {

  @MockBean
  private AddressValidationService addressValidationService;

  @Test
  public void addressValidationShouldOnlyRunWhenSetToTrue() throws Exception {
    when(addressValidationService.validate(any())).thenReturn(Map.of(
        "validationOn",
        new ValidatedAddress("validatedStreetAddress",
            "validatedAptNumber",
            "validatedCity",
            "validatedState",
            "validatedZipCode-1234")
    ));

    var params = new HashMap<String, List<String>>();
    params.put("_validatevalidationOff", List.of("false"));
    params.put("validationOffStreetAddress1", List.of("110 N 6th St"));
    params.put("validationOffStreetAddress2", List.of("Apt 1"));
    params.put("validationOffCity", List.of("Roswell"));
    params.put("validationOffState", List.of("NM"));
    params.put("validationOffZipCode", List.of("88201"));
    params.put("_validatevalidationOn", List.of("true"));
    params.put("validationOnStreetAddress1", List.of("880 N 8th St"));
    params.put("validationOnStreetAddress2", List.of("Apt 2"));
    params.put("validationOnCity", List.of("Roswell"));
    params.put("validationOnState", List.of("NM"));
    params.put("validationOnZipCode", List.of("88201"));

    postExpectingSuccess("testAddressValidation", params);

    verify(addressValidationService, times(1)).validate(any());
  }
}
