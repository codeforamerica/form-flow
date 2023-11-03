package formflow.library.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import formflow.library.address_validation.AddressValidationService;
import formflow.library.address_validation.ValidatedAddress;
import formflow.library.utilities.AbstractMockMvcTest;
import formflow.library.utilities.FormScreen;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
@DirtiesContext()
public class InputsTest extends AbstractMockMvcTest {

  @MockBean
  AddressValidationService addressValidationService;

  @Test
  void shouldPersistInputValuesWhenNavigatingBetweenScreens() throws Exception {
    String textInput = "foo";
    String areaInput = "foo bar baz";
    String dateMonth = "10";
    String dateDay = "30";
    String dateYear = "2020";
    String numberInput = "123";
    // First "" value is from hidden input that a screen would submit
    List<String> checkboxSet = List.of("", "Checkbox-A", "Checkbox-B");
    List<String> checkboxInput = List.of("", "checkbox-value");
    String radioInput = "Radio B";
    String selectInput = "Select B";
    String moneyInput = "100";
    String phoneInput = "(555) 555-1234";
    String ssnInput = "333-22-4444";
    String stateInput = messageSource.getMessage("state.nh", null, Locale.ENGLISH).substring(0, 2);

    FormScreen nextPage = postAndFollowRedirect("inputs",
        Map.ofEntries(
            Map.entry("textInput", List.of(textInput)),
            Map.entry("areaInput", List.of(areaInput)),
            Map.entry("dateMonth", List.of(dateMonth)),
            Map.entry("dateDay", List.of(dateDay)),
            Map.entry("dateYear", List.of(dateYear)),
            Map.entry("numberInput", List.of(numberInput)),
            // CheckboxSet's need to have the [] in their name for POST actions
            Map.entry("checkboxSet[]", checkboxSet),
            // Checkboxes need to have the [] in their name for POST actions
            Map.entry("checkboxInput[]", checkboxInput),
            Map.entry("radioInput", List.of(radioInput)),
            Map.entry("selectInput", List.of(selectInput)),
            Map.entry("moneyInput", List.of(moneyInput)),
            Map.entry("phoneInput", List.of(phoneInput)),
            Map.entry("ssnInput", List.of(ssnInput)),
            Map.entry("stateInput", List.of(stateInput)))
    );
    assertThat(nextPage.getTitle()).isEqualTo("Test");

    var inputsScreen = new FormScreen(getPage("inputs"));

    // Remove hidden value (our Screen Controller does this automatically)
    List<String> removedHiddenCheckboxSet = checkboxSet.stream().filter(e -> !e.isEmpty()).toList();
    List<String> removedHiddenCheckboxInput = checkboxInput.stream().filter(e -> !e.isEmpty()).toList();

    assertThat(inputsScreen.getInputValue("textInput")).isEqualTo(textInput);
    assertThat(inputsScreen.getTextAreaValue("areaInput")).isEqualTo(areaInput);
    assertThat(inputsScreen.getInputValue("dateMonth")).isEqualTo(dateMonth);
    assertThat(inputsScreen.getInputValue("dateDay")).isEqualTo(dateDay);
    assertThat(inputsScreen.getInputValue("dateYear")).isEqualTo(dateYear);
    assertThat(inputsScreen.getInputValue("numberInput")).isEqualTo(numberInput);
    assertThat(inputsScreen.getCheckboxSetValues("checkboxSet")).isEqualTo(removedHiddenCheckboxSet);
    assertThat(inputsScreen.getCheckboxSetValues("checkboxInput")).isEqualTo(removedHiddenCheckboxInput);
    assertThat(inputsScreen.getRadioValue("radioInput")).isEqualTo(radioInput);
    assertThat(inputsScreen.getSelectValue("selectInput")).isEqualTo(selectInput);
    assertThat(inputsScreen.getInputValue("moneyInput")).isEqualTo(moneyInput);
    assertThat(inputsScreen.getInputValue("phoneInput")).isEqualTo(phoneInput);
    assertThat(inputsScreen.getInputValue("ssnInput")).isEqualTo(ssnInput);
    assertThat(inputsScreen.getSelectValue("stateInput")).isEqualTo(stateInput);
  }

  @Test
  void shouldOnlyRunValidationIfItHasARequiredAnnotation() throws Exception {
    // Should not validate when value is empty
    postExpectingNextPageTitle("pageWithOptionalValidation", "validatePositiveIfNotEmpty", "", "Success");
    // Should validate when a value is entered
    postExpectingFailureAndAssertErrorDisplaysForThatInput("pageWithOptionalValidation", "validatePositiveIfNotEmpty", "-2",
        "must be greater than 0");
    // Should redirect when input is valid
    postExpectingNextPageTitle("pageWithOptionalValidation", "validatePositiveIfNotEmpty", "2", "Success");
  }

  @Test
  void shouldShowMultipleErrorMessagesOnSingleInput() throws Exception {
    postExpectingFailureAndAssertErrorsDisplaysForThatInput("pageWithMultipleValidationInput", "inputWithMultipleValidations", "",
        2);
    postExpectingFailureAndAssertErrorsDisplayForThatInput("pageWithMultipleValidationInput", "inputWithMultipleValidations", "",
        List.of("You must enter a value 2 characters or longer", "Don't leave this blank"));
  }

  @Nested
  public class Address {

    String streetAddress1 = "1111 N State St";
    String streetAddress2 = "Apt 2";
    String city = "Roswell";
    String state = "NM";
    String zipCode = "88201";

    @Nested
    public class AddressValidationSuccess {

      private FormScreen nextScreen;

      @BeforeEach
      void beforeEach() throws Exception {
        String inputName = "validationOn";
        ValidatedAddress addressValidatedAddress = new ValidatedAddress(
            streetAddress1 + streetAddress2 + "Validated",
            "",
            city + "Validated",
            state,
            zipCode + "Validated");
        when(addressValidationService.validate(any())).thenReturn(Map.of(inputName, addressValidatedAddress));

        nextScreen = postAndFollowRedirect("testAddressValidation",
            Map.ofEntries(
                Map.entry(inputName + "StreetAddress1", List.of(streetAddress1)),
                Map.entry(inputName + "StreetAddress2", List.of(streetAddress2)),
                Map.entry(inputName + "City", List.of(city)),
                Map.entry(inputName + "State", List.of(state)),
                Map.entry(inputName + "ZipCode", List.of(zipCode)),
                Map.entry(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS + inputName, List.of("true"))
            ));
      }

      @Test
      void isValidatedWhenInputNamePlusValidateIsTrue() throws Exception {

        assertThat(nextScreen.getTitle()).isEqualTo("Validation Is On");
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains(
            streetAddress1 + streetAddress2 + "Validated");
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains(city + "Validated");
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains(city + "Validated");
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains(state);
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains(zipCode + "Validated");

        assertThat(nextScreen.getElementTextById("original-address-label")).contains(streetAddress1);
        assertThat(nextScreen.getElementTextById("original-address-label")).contains(streetAddress2);
        assertThat(nextScreen.getElementTextById("original-address-label")).contains(city);
        assertThat(nextScreen.getElementTextById("original-address-label")).contains(state);
        assertThat(nextScreen.getElementTextById("original-address-label")).contains(zipCode);
      }

      @Test
      void removesPreviousSuggestionWhenGoingBackAndEnteringInvalidAddress() throws Exception {
        String inputName = "validationOn";
        HashMap<String, ValidatedAddress> testMap = new HashMap();
        testMap.put(inputName, null);
        when(addressValidationService.validate(any())).thenReturn(testMap);

        var nextScreen = postAndFollowRedirect("testAddressValidation",
            Map.ofEntries(
                Map.entry(inputName + "StreetAddress1", List.of("Total Junk")),
                Map.entry(inputName + "StreetAddress2", List.of(streetAddress2)),
                Map.entry(inputName + "City", List.of("Fake")),
                Map.entry(inputName + "State", List.of(state)),
                Map.entry(inputName + "ZipCode", List.of(zipCode)),
                Map.entry(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS + inputName, List.of("true"))
            ));

        assertThat(nextScreen.getTitle()).isEqualTo("testAddressValidationNotFound");
        assertThat(nextScreen.getElementById("validated-address-label")).isNull();

        assertThat(nextScreen.getElementTextById("original-address-label")).contains("Total Junk");
        assertThat(nextScreen.getElementTextById("original-address-label")).contains(streetAddress2);
        assertThat(nextScreen.getElementTextById("original-address-label")).contains("Fake");
        assertThat(nextScreen.getElementTextById("original-address-label")).contains(state);
        assertThat(nextScreen.getElementTextById("original-address-label")).contains(zipCode);
      }

      @Test
      void updatesSuggestionWhenGoingBackAndUpdatingNewAddress() throws Exception {
        String inputName = "validationOn";
        ValidatedAddress otherValidatedAddress = new ValidatedAddress(
            "123 Other Main Street " + " Apt B" + "Validated",
            "",
            "Other City" + "Validated",
            "OZ",
            "12345" + "Validated");

        when(addressValidationService.validate(any())).thenReturn(Map.of(inputName, otherValidatedAddress));

        var nextScreen = postAndFollowRedirect("testAddressValidation",
            Map.ofEntries(
                Map.entry(inputName + "StreetAddress1", List.of("123 Other Main Street")),
                Map.entry(inputName + "StreetAddress2", List.of("Apt B")),
                Map.entry(inputName + "City", List.of("Other City")),
                Map.entry(inputName + "State", List.of("OZ")),
                Map.entry(inputName + "ZipCode", List.of("12345")),
                Map.entry(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS + inputName, List.of("true"))
            ));

        assertThat(nextScreen.getTitle()).isEqualTo("Validation Is On");
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains("123 Other Main Street Apt BValidated");
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains("Other CityValidated");
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains("OZ");
        assertThat(nextScreen.getElementTextById("validated-address-label")).contains("12345Validated");

        assertThat(nextScreen.getElementTextById("original-address-label")).contains("123 Other Main Street");
        assertThat(nextScreen.getElementTextById("original-address-label")).contains("Apt B");
        assertThat(nextScreen.getElementTextById("original-address-label")).contains("Other City");
        assertThat(nextScreen.getElementTextById("original-address-label")).contains("OZ");
        assertThat(nextScreen.getElementTextById("original-address-label")).contains("12345");
      }
    }
  }

  @Test
  void shouldShowCorrectNumberOfErrorMessagesOnInputs() throws Exception {
    var inputParams = Map.of(
        "inputWithMultipleValidations", "",
        "inputWithSingleValidation", "");

    var expectedErrors = Map.of(
        "inputWithMultipleValidations",
        List.of("You must enter a value 2 characters or longer", "Don't leave this blank"),
        "inputWithSingleValidation", List.of("Enter a value"));

    postExpectingFailureAndAssertInputErrorMessages("pageWithMultipleValidationInput", inputParams,
        expectedErrors);
  }

  @Nested
  public class SubmitButton {

    @Test
    void shouldHaveDefaultClasses() throws Exception {
      var page = new FormScreen(getPage("pageWithDefaultSubmitButton"));
      assertThat(page.getElementById("form-submit-button").classNames()).isEqualTo(Set.of("button", "button--primary"));
    }

    @Test
    void shouldHaveCustomClasses() throws Exception {
      var page = new FormScreen(getPage("pageWithCustomSubmitButton"));
      assertThat(page.getElementById("form-submit-button").classNames()).isEqualTo(Set.of("custom"));
    }
  }
}
