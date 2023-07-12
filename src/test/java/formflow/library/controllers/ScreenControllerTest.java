package formflow.library.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import formflow.library.address_validation.AddressValidationService;
import formflow.library.address_validation.ValidatedAddress;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import formflow.library.utilities.FormScreen;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
public class ScreenControllerTest extends AbstractMockMvcTest {

  @MockBean
  private AddressValidationService addressValidationService;

  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).urlParams(new HashMap<>()).inputData(new HashMap<>()).build();
    when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    super.setUp();
  }

  @Nested
  public class UrlParameterPersistence {

    @Test
    public void passedUrlParametersShouldBeSaved() throws Exception {
      Map<String, String> queryParams = new HashMap<>();
      queryParams.put("lang", "en");
      getWithQueryParam("test", "lang", "en");
      assert (submission.getUrlParams().equals(queryParams));
    }
  }

  @Nested
  public class SubflowParameters {

    @Test
    public void modelIncludesCurrentSubflowItem() throws Exception {
      HashMap<String, String> subflowItem = new HashMap<>();
      subflowItem.put("uuid", "aaa-bbb-ccc");
      subflowItem.put("firstNameSubflow", "foo bar baz");

      submission.setInputData(Map.of("testSubflow", List.of(subflowItem)));
      getPageExpectingSuccess("subflowAddItem/aaa-bbb-ccc");
    }
  }

  @Nested
  public class AddressValidation {

    @Test
    public void addressValidationShouldRunAfterFieldValidation() throws Exception {
      var params = new HashMap<String, List<String>>();
      params.put("validate_validationOn", List.of("true"));
      params.put("validationOnStreetAddress1", List.of("880 N 8th St"));
      params.put("validationOnStreetAddress2", List.of("Apt 2"));
      // City is required
      params.put("validationOnCity", List.of(""));
      params.put("validationOnState", List.of("NM"));
      params.put("validationOnZipCode", List.of("88201"));

      postExpectingFailure("testAddressValidation", params);
      verify(addressValidationService, times(0)).validate(any());
    }

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
      params.put("validate_validationOff", List.of("false"));
      params.put("validationOffStreetAddress1", List.of("110 N 6th St"));
      params.put("validationOffStreetAddress2", List.of("Apt 1"));
      params.put("validationOffCity", List.of("Roswell"));
      params.put("validationOffState", List.of("NM"));
      params.put("validationOffZipCode", List.of("88201"));
      params.put("validate_validationOn", List.of("true"));
      params.put("validationOnStreetAddress1", List.of("880 N 8th St"));
      params.put("validationOnStreetAddress2", List.of("Apt 2"));
      params.put("validationOnCity", List.of("Roswell"));
      params.put("validationOnState", List.of("NM"));
      params.put("validationOnZipCode", List.of("88201"));

      postExpectingSuccess("testAddressValidation", params);

      verify(addressValidationService, times(1)).validate(any());
    }
  }

  @Test
  public void fieldsStillHaveValuesWhenFieldValidationFailsInSubflow() throws Exception {
    var params = new HashMap<String, List<String>>();
    UUID uuid = UUID.randomUUID();
    params.put("firstNameSubflow", List.of("tester"));
    params.put("textInputSubflow", List.of("text input value"));
    params.put("areaInputSubflow", List.of("area input value"));
    // bad value for numberInput
    params.put("numberInputSubflow", List.of(""));
    params.put("checkboxSetSubflow[]", List.of("", "Checkbox-A"));
    params.put("checkboxInputSubflow[]", List.of("", "checkbox-value"));
    params.put("radioInputSubflow", List.of("Radio B"));
    params.put("selectInputSubflow", List.of("Select C"));
    // bad value for moneyInput
    params.put("moneyInputSubflow", List.of("-11"));
    // bad value for phoneInput
    params.put("phoneInputSubflow", List.of("12323"));
    // skipping date, which should cause an error

    String pageName = "subflowAddItem/new";
    postExpectingFailure(pageName, params, "subflowAddItem");
    var page = new FormScreen(getPage("subflowAddItem"));

    assertTrue(page.hasDateInputError());
    assertTrue(page.hasInputError("numberInputSubflow"));
    assertTrue(page.hasInputError("moneyInputSubflow"));
    assertTrue(page.hasInputError("phoneInputSubflow"));
    assertFalse(page.hasInputError("textInputSubflow"));

    // make sure the values are still filled in, in general
    assertEquals("text input value", page.getInputValue("textInputSubflow"));
    assertEquals("area input value", page.getTextAreaValue("areaInputSubflow"));
    assertEquals(List.of("Checkbox-A"), page.getCheckboxSetValues("checkboxSetSubflow"));
    assertEquals(List.of("checkbox-value"), page.getCheckboxSetValues("checkboxInputSubflow"));
    assertEquals("Radio B", page.getRadioValue("radioInputSubflow"));
    assertEquals("Select C", page.getSelectValue("selectInputSubflow"));
  }


}
