package formflow.library.address_validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.smartystreets.api.exceptions.SmartyException;
import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow-address-validation.yaml"}, webEnvironment = RANDOM_PORT)
public class AddressValidationJourneyTest extends AbstractBasePageTest {

  @Autowired
  private AddressValidationService addressValidationService;

  private final String startPagePrefix = "flow/testFlowAddressValidation/";

  @Test
  public void testAddressValidationInFlowValidationSuccess() throws IOException, SmartyException, InterruptedException {
    startingPage = startPagePrefix + "testAddressValidation";
    super.setUp();
    Map<String, ValidatedAddress> goodValidatedAddress = Map.of(
        "validationOn",
        new ValidatedAddress("validatedStreetAddress",
            "validatedAptNumber",
            "validatedCity",
            "validatedState",
            "validatedZipCode-1234"
        ));

    when(addressValidationService.validate(any())).thenReturn(goodValidatedAddress);

    assertThat(testPage.getTitle()).isEqualTo("Enter Address");
    testPage.enter("validationOnStreetAddress1", "1111 N State St");
    testPage.enter("validationOnStreetAddress2", "Apt 2");
    testPage.enter("validationOnCity", "Roswell");
    testPage.enter("validationOnState", "NM - New Mexico");
    testPage.enter("validationOnZipCode", "88201");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("Validation Is On");
  }

  @Test
  public void testAddressValidationInFlowValidationNotFound() throws IOException, SmartyException, InterruptedException {
    startingPage = startPagePrefix + "testAddressValidation";
    super.setUp();

    Map<String, ValidatedAddress> badValidationAddress = new HashMap<>();
    badValidationAddress.put("validationOn", null);

    when(addressValidationService.validate(any())).thenReturn(badValidationAddress);

    testPage.enter("validationOnStreetAddress1", "1234 junk");
    testPage.enter("validationOnStreetAddress2", "Apt 2");
    testPage.enter("validationOnCity", "Made Up City");
    testPage.enter("validationOnState", "NM - New Mexico");
    testPage.enter("validationOnZipCode", "12345");
    testPage.clickContinue();

    assertThat(testPage.getTitle()).isEqualTo("testAddressValidationNotFound");
  }

  @Test
  public void testAddressValidationInSubflowAddressFound() throws IOException, SmartyException, InterruptedException {
    startingPage = startPagePrefix + "testSubflowAddressValidation";
    super.setUp();
    Map<String, ValidatedAddress> goodValidatedAddress = Map.of(
        "validationOn",
        new ValidatedAddress("validatedStreetAddress",
            "validatedAptNumber",
            "validatedCity",
            "validatedState",
            "validatedZipCode-1234"
        ));

    when(addressValidationService.validate(any())).thenReturn(goodValidatedAddress);

    assertThat(testPage.getTitle()).isEqualTo("Enter Address (subflow)");
    testPage.enter("validationOnStreetAddress1", "1111 N State St");
    testPage.enter("validationOnStreetAddress2", "Apt 2");
    testPage.enter("validationOnCity", "Roswell");
    testPage.enter("validationOnState", "NM - New Mexico");
    testPage.enter("validationOnZipCode", "88201");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("Validation Is On (subflow)");
  }

  @Test
  public void testAddressValidationInSubflowAddressNotFound() throws IOException, SmartyException, InterruptedException {
    startingPage = startPagePrefix + "testSubflowAddressValidation";
    super.setUp();

    Map<String, ValidatedAddress> badValidationAddress = new HashMap<>();
    badValidationAddress.put("validationOn", null);

    when(addressValidationService.validate(any())).thenReturn(badValidationAddress);

    testPage.enter("validationOnStreetAddress1", "1234 junk");
    testPage.enter("validationOnStreetAddress2", "Apt 2");
    testPage.enter("validationOnCity", "Made Up City");
    testPage.enter("validationOnState", "NM - New Mexico");
    testPage.enter("validationOnZipCode", "12345");
    testPage.clickContinue();

    assertThat(testPage.getTitle()).isEqualTo("testAddressValidationNotFound (subflow)");
  }
}
