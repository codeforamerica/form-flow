package formflow.library.address_validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.github.dockerjava.core.dockerfile.DockerfileStatement.Add;
import com.smartystreets.api.exceptions.SmartyException;
import formflow.library.data.FormSubmission;
import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"}, webEnvironment = RANDOM_PORT)
public class AddressValidationJourneyTest extends AbstractBasePageTest {

  @Autowired
  private AddressValidationService addressValidationService;

  private AddressValidationService spyAddressValidationService;

  @Override
  @BeforeEach
  public void setUp() throws IOException {

    spyAddressValidationService = spy(addressValidationService);
    try {
      FormSubmission goodFormSubmission = new FormSubmission(
          Map.of("validationOnStreetAddress1", "1111 N State St",
              "validationOnStreetAddress2", "Apt 2",
              "validationOnCity", "Roswell",
              "validationOnState", "NM - New Mexico",
              "validationOnZipCode", "88201")
      );
      Map<String, ValidatedAddress> validatedAddress = Map.of(
          "validatedInput",
          new ValidatedAddress("validatedStreetAddress",
              "validatedAptNumber",
              "validatedCity",
              "validatedState",
              "validatedZipCode-1234"
          ));

      doReturn(validatedAddress).when(spyAddressValidationService.validate(goodFormSubmission));
      FormSubmission badFormSubmission = new FormSubmission(
          Map.of("validationOnStreetAddress1", "1234 junk",
              "validationOnStreetAddress2", "Apt 2",
              "validationOnCity", "Made Up City",
              "validationOnState", "NM - New Mexico",
              "validationOnZipCode", "12345")
      );
      Map<String, ValidatedAddress> badValidationAddress = Map.of(
          "validatedInput", null);
      doReturn(badValidationAddress).when(spyAddressValidationService.validate(badFormSubmission));
    } catch (Exception e) {
      System.out.println("error caught: " + e.getMessage());
    }
  }

  @Test
  public void testAddressValidationInFlow() throws SmartyException, IOException, InterruptedException {
    startingPage = "flow/testFlow/testAddressValidation";
    super.setUp();
    assertThat(testPage.getTitle()).isEqualTo("Enter Address");
    testPage.enter("validationOnStreetAddress1", "1111 N State St");
    testPage.enter("validationOnStreetAddress2", "Apt 2");
    testPage.enter("validationOnCity", "Roswell");
    testPage.enter("validationOnState", "NM - New Mexico");
    testPage.enter("validationOnZipCode", "88201");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("Validation Is On");

    testPage.goBack();
    testPage.enter("validationOnStreetAddress1", "1234 junk");
    testPage.enter("validationOnStreetAddress2", "Apt 2");
    testPage.enter("validationOnCity", "Made Up City");
    testPage.enter("validationOnState", "NM - New Mexico");
    testPage.enter("validationOnZipCode", "12345");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("testAddressValidationNotFound");
  }

  @Test
  public void testAddressValidationInSubflow() throws SmartyException, IOException, InterruptedException {

    startingPage = "flow/testFlow/testSubflowAddressValidation";
    super.setUp();

    assertThat(testPage.getTitle()).isEqualTo("Enter Address (subflow)");
    testPage.enter("validationOnStreetAddress1", "1111 N State St");
    testPage.enter("validationOnStreetAddress2", "Apt 2");
    testPage.enter("validationOnCity", "Roswell");
    testPage.enter("validationOnState", "NM - New Mexico");
    testPage.enter("validationOnZipCode", "88201");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("Validation Is On (subflow)");

    testPage.goBack();
    testPage.enter("validationOnStreetAddress1", "1234 junk");
    testPage.enter("validationOnStreetAddress2", "Apt 2");
    testPage.enter("validationOnCity", "Made Up City");
    testPage.enter("validationOnState", "NM - New Mexico");
    testPage.enter("validationOnZipCode", "12345");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("testAddressValidationNotFound (subflow)");
  }
}
