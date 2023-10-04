package formflow.library.controllers;

import static formflow.library.FormFlowController.SUBMISSION_MAP_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
public class ScreenControllerTest extends AbstractMockMvcTest {

  @MockBean
  private AddressValidationService addressValidationService;

  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;

  public final String uuidPatternString = "{uuid:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}";

  private Map<String, Object> sessionAttributes;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).urlParams(new HashMap<>()).inputData(new HashMap<>()).build();
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    when(submissionRepositoryService.save(any())).thenReturn(submission);

    sessionAttributes = Map.of(
        SUBMISSION_MAP_NAME,
        new HashMap<String, Object>(
            Map.of("testFlow", submission.getId())
        )
    );

    super.setUp();
  }


  @ParameterizedTest
  @CsvSource({
      "GET, /flow/{flow}/{screen}, flowThatDoesNotExist, screen",
      "GET, /flow/{flow}/{screen}, testFlow, screenThatDoesNotExist",
      "POST, /flow/{flow}/{screen}, flowThatDoesNotExist, screen",
      "POST, /flow/{flow}/{screen}, testFlow, screenThatDoesNotExist",
      "POST, /flow/{flow}/{screen}/submit, flowThatDoesNotExist, screen",
      "POST, /flow/{flow}/{screen}/submit, testFlow, screenThatDoesNotExist",
      "GET, /flow/{flow}/{screen}/thisIsAUUIDForASubflow, flowThatDoesNotExist, screen",
      "GET, /flow/{flow}/{screen}/thisIsAUUIDForASubflow, testFlow, screenThatDoesNotExist",
      "POST, /flow/{flow}/{screen}/thisIsAUUIDForASubflow, flowThatDoesNotExist, screen",
      "POST, /flow/{flow}/{screen}/thisIsAUUIDForASubflow, testFlow, screenThatDoesNotExist",
      "GET, /flow/{flow}/{subflowName}/thisIsAUUIDForASubflow/deleteConfirmation, flowThatDoesNotExist, testSubflow",
      "POST, /flow/{flow}/{subflowName}/thisIsAUUIDForASubflow/delete, flowThatDoesNotExist, testSubflow",
      "GET, /flow/{flow}/{screen}/navigation, flowThatDoesNotExist, screen",
      "GET, /flow/{flow}/{screen}/navigation, testFlow, screenThatDoesNotExist"
  })
  void endpointShouldReturn404IfFlowOrScreenDoesNotExist(String method, String path, String flow, String screen)
      throws Exception {
    switch (method) {
      case "GET" -> mockMvc.perform(get(path, flow, screen)).andExpect(status().isNotFound());
      case "POST" -> mockMvc.perform(post(path, flow, screen)).andExpect(status().isNotFound());
    }
  }

  @Nested
  public class UrlParameterPersistence {

    @Test
    public void passedUrlParametersShouldBeSaved() throws Exception {
      Map<String, String> queryParams = new HashMap<>();
      queryParams.put("lang", "en");
      getWithQueryParam("test", "lang", "en", sessionAttributes);
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
      getPageExpectingSuccess("subflowAddItem/aaa-bbb-ccc", sessionAttributes);
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
  public void fieldsStillHaveValuesWhenFieldValidationFailsInSubflowNewIteration() throws Exception {
    var params = new HashMap<String, List<String>>();
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
    postExpectingFailure(pageName, params, "subflowAddItem", sessionAttributes);
    var page = new FormScreen(getPage("subflowAddItem", sessionAttributes));

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


  @Test
  public void fieldsStillHaveValuesWhenFieldValidationFailsInSubflowPage2() throws Exception {
    var paramsPage1 = new HashMap<String, List<String>>();
    paramsPage1.put("firstNameSubflow", List.of("tester"));
    paramsPage1.put("textInputSubflow", List.of("text input value"));
    paramsPage1.put("areaInputSubflow", List.of("area input value"));
    paramsPage1.put("numberInputSubflow", List.of("10"));
    paramsPage1.put("checkboxSetSubflow[]", List.of("", "Checkbox-A"));
    paramsPage1.put("checkboxInputSubflow[]", List.of("", "checkbox-value"));
    paramsPage1.put("radioInputSubflow", List.of("Radio B"));
    paramsPage1.put("selectInputSubflow", List.of("Select C"));
    paramsPage1.put("moneyInputSubflow", List.of("11"));
    paramsPage1.put("phoneInputSubflow", List.of("(413) 123-1234"));
    paramsPage1.put("dateSubflowDay", List.of("12"));
    paramsPage1.put("dateSubflowMonth", List.of("12"));
    paramsPage1.put("dateSubflowYear", List.of("2012"));

    String pageName = "/flow/testFlow/subflowAddItem/new";
    ResultActions resultActions = postToUrlExpectingSuccessRedirectPattern(pageName,
        "/flow/testFlow/subflowAddItemPage2/" + uuidPatternString,
        paramsPage1, sessionAttributes);

    var paramsPage2 = new HashMap<String, List<String>>();
    paramsPage2.put("firstNameSubflowPage2", List.of("tester"));
    paramsPage2.put("textInputSubflowPage2", List.of("text input value"));
    paramsPage2.put("areaInputSubflowPage2", List.of("area input value"));
    paramsPage2.put("numberInputSubflowPage2", List.of("")); // bad data
    paramsPage2.put("checkboxSetSubflowPage2[]", List.of("", "Checkbox-A"));
    paramsPage2.put("checkboxInputSubflowPage2[]", List.of("", "checkbox-value"));
    paramsPage2.put("radioInputSubflowPage2", List.of("Radio B"));
    paramsPage2.put("selectInputSubflowPage2", List.of("Select C"));
    paramsPage2.put("moneyInputSubflowPage2", List.of("-11"));  // bad data
    paramsPage2.put("phoneInputSubflowPage2", List.of("(413) 123-1234"));
    paramsPage2.put("dateSubflowPage2Day", List.of("35"));
    paramsPage2.put("dateSubflowPage2Month", List.of("12"));
    paramsPage2.put("dateSubflowPage2Year", List.of("2012"));

    String redirectedUrl = resultActions.andReturn().getResponse().getRedirectedUrl();
    int lastSlash = redirectedUrl.lastIndexOf('/');
    String pageNamePage2 = "subflowAddItemPage2/" + redirectedUrl.substring(lastSlash + 1);
    postExpectingFailure(pageNamePage2, paramsPage2, pageNamePage2, sessionAttributes);

    var page2 = new FormScreen(getPage("subflowAddItemPage2/" + redirectedUrl.substring(lastSlash), sessionAttributes));
    assertTrue(page2.hasDateInputError());
    assertTrue(page2.hasInputError("numberInputSubflowPage2"));
    assertTrue(page2.hasInputError("moneyInputSubflowPage2"));
    assertFalse(page2.hasInputError("phoneInputSubflow"));

    assertEquals("text input value", page2.getInputValue("textInputSubflowPage2"));
    assertEquals("area input value", page2.getTextAreaValue("areaInputSubflowPage2"));
    assertEquals(List.of("Checkbox-A"), page2.getCheckboxSetValues("checkboxSetSubflowPage2"));
    assertEquals(List.of("checkbox-value"), page2.getCheckboxSetValues("checkboxInputSubflowPage2"));
    assertEquals("Radio B", page2.getRadioValue("radioInputSubflowPage2"));
    assertEquals("Select C", page2.getSelectValue("selectInputSubflowPage2"));
  }
}
