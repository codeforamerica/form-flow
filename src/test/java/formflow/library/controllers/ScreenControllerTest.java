package formflow.library.controllers;

import static formflow.library.FormFlowController.SUBMISSION_MAP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
public class ScreenControllerTest extends AbstractMockMvcTest {

  @MockBean
  private AddressValidationService addressValidationService;

  public static final String UUID_PATTERN_STRING = "{uuid:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}}";

  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).urlParams(new HashMap<>()).inputData(new HashMap<>()).build();
    // this setups flow info in the session to get passed along later on.
    setFlowInfoInSession(session, "testFlow", submission.getId());

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
      when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
      mockMvc.perform(get(getUrlForPageName("test")).queryParam("lang", "en").session(session))
              .andExpect(status().isOk());
      assert (submission.getUrlParams().equals(Map.of("lang", "en")));
    }
  }

  @Nested
  public class SubflowTests {

    @Test
    public void modelIncludesCurrentSubflowItem() throws Exception {
      when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
      HashMap<String, String> subflowItem = new HashMap<>();
      subflowItem.put("uuid", "aaa-bbb-ccc");
      subflowItem.put("firstNameSubflow", "foo bar baz");

      submission.setInputData(Map.of("testSubflow", List.of(subflowItem)));
      getPageExpectingSuccess("subflowAddItem/aaa-bbb-ccc");
    }
    
    @Test
    public void shouldUpdateIterationIsCompleteOnSubflowsWhereLastScreenIsAGetRequest() throws Exception {
      setFlowInfoInSession(session, "testSubflowLogic", submission.getId());
      mockMvc.perform(post("/flow/testSubflowLogic/subflowAddItem/new")
          .session(session)
          .params(new LinkedMultiValueMap<>(Map.of(
              "textInput", List.of("textInputValue"),
              "numberInput", List.of("10"))))
      );
      UUID testSubflowLogicUUID = ((Map<String, UUID>) session.getAttribute(SUBMISSION_MAP_NAME)).get("testSubflowLogic");

      Submission submissionBeforeSubflowIsCompleted = submissionRepositoryService.findById(testSubflowLogicUUID).get();
      List<Map<String, Object>> iterationsBeforeSubflowIsCompleted = (List<Map<String, Object>>) submissionBeforeSubflowIsCompleted.getInputData().get("subflowWithGetAtEnd");
      String uuidString = (String) iterationsBeforeSubflowIsCompleted.get(0).get("uuid");
      mockMvc.perform(get("/flow/testSubflowLogic/otherGetScreen/" + uuidString).session(session))
          .andExpect(status().isOk());
      mockMvc.perform(get("/flow/testSubflowLogic/otherGetScreen/navigation?uuid=" + uuidString).session(session))
          .andExpect(status().is3xxRedirection());

      Submission submissionAfterSubflowIsCompleted = submissionRepositoryService.findById(testSubflowLogicUUID).get();
      List<Map<String, Object>> subflowIterationsAfterSubflowIsCompleted = (List<Map<String, Object>>) submissionAfterSubflowIsCompleted.getInputData().get("subflowWithGetAtEnd");
      assertTrue((Boolean) subflowIterationsAfterSubflowIsCompleted.get(0).get("iterationIsComplete"));
    }

    @Test
    public void shouldNotUpdateIterationIsCompleteBeforeSubflowHasFinished() throws Exception {
      setFlowInfoInSession(session, "testSubflowLogic", submission.getId());
      mockMvc.perform(post("/flow/testSubflowLogic/subflowAddItem/new")
          .session(session)
          .params(new LinkedMultiValueMap<>(Map.of(
              "textInput", List.of("textInputValue"),
              "numberInput", List.of("10"))))
      );
      UUID testSubflowLogicUUID = ((Map<String, UUID>) session.getAttribute(SUBMISSION_MAP_NAME)).get("testSubflowLogic");
      Submission submissionBeforeSubflowIsCompleted = submissionRepositoryService.findById(testSubflowLogicUUID).get();
      List<Map<String, Object>> iterationsBeforeSubfowIsCompleted = (List<Map<String, Object>>) submissionBeforeSubflowIsCompleted.getInputData().get("subflowWithGetAtEnd");
      String uuidString = (String) iterationsBeforeSubfowIsCompleted.get(0).get("uuid");
      mockMvc.perform(get("/flow/testSubflowLogic/getScreen/" + uuidString).session(session))
          .andExpect(status().isOk());
      mockMvc.perform(get("/flow/testSubflowLogic/getScreen/navigation?uuid=" + uuidString).session(session))
          .andExpect(status().is3xxRedirection());

      Submission submissionBetweenGetScreens = submissionRepositoryService.findById(testSubflowLogicUUID).get();
      List<Map<String, Object>> subflowIterationsBetweenGetScreens = (List<Map<String, Object>>) submissionBetweenGetScreens.getInputData().get("subflowWithGetAtEnd");
      assertThat((Boolean) subflowIterationsBetweenGetScreens.get(0).get("iterationIsComplete")).isFalse();
      
      mockMvc.perform(get("/flow/testSubflowLogic/otherGetScreen/" + uuidString).session(session))
          .andExpect(status().isOk());
      mockMvc.perform(get("/flow/testSubflowLogic/otherGetScreen/navigation?uuid=" + uuidString).session(session))
          .andExpect(status().is3xxRedirection());
      Submission submissionAfterSubflowIsCompleted = submissionRepositoryService.findById(testSubflowLogicUUID).get();
      List<Map<String, Object>> subflowIterationsAfterSubflowIsCompleted = (List<Map<String, Object>>) submissionAfterSubflowIsCompleted.getInputData().get("subflowWithGetAtEnd");
      assertThat((Boolean) subflowIterationsAfterSubflowIsCompleted.get(0).get("iterationIsComplete")).isTrue();
    }
    
    @Test
    public void shouldSetIterationIsCompleteWhenLastScreenInSubflowIsAPost() throws Exception {
      setFlowInfoInSession(session, "otherTestFlow", submission.getId());
      mockMvc.perform(post("/flow/otherTestFlow/subflowAddItem/new")
          .session(session)
          .params(new LinkedMultiValueMap<>(Map.of(
              "textInput", List.of("textInputValue"),
              "numberInput", List.of("10"))))
      );
      Map<String, Object> iterationAfterFirstSubflowScreeen = getMostRecentlyCreatedIterationData(session, "otherTestFlow", "testSubflow");

      String iterationUuid = (String) iterationAfterFirstSubflowScreeen.get("uuid");
      assertThat((Boolean) iterationAfterFirstSubflowScreeen.get("iterationIsComplete")).isFalse();

      String navigationUrl = "/flow/otherTestFlow/subflowAddItemPage2/navigation?uuid=" + iterationUuid;
      postToUrlExpectingSuccess("/flow/otherTestFlow/subflowAddItemPage2", navigationUrl, new HashMap<>(), iterationUuid);
      assertThat(followRedirectsForUrl(navigationUrl)).isEqualTo("/flow/otherTestFlow/test");

      Map<String, Object> iterationAfterSecondSubflowScreeen = getMostRecentlyCreatedIterationData(session, "otherTestFlow", "testSubflow");
      assertThat((Boolean) iterationAfterSecondSubflowScreeen.get("iterationIsComplete")).isTrue();
    }

    private record Result(UUID testSubflowLogicUUID, List<Map<String, Object>> iterationsAfterFirstPost, String uuidString) {
    }

    @Test
    public void shouldHandleSubflowsWithAGetAndThenAPost() throws Exception {
      setFlowInfoInSession(session, "yetAnotherTestFlow", submission.getId());
      mockMvc.perform(post("/flow/yetAnotherTestFlow/subflowAddItem/new")
          .session(session)
          .params(new LinkedMultiValueMap<>(Map.of(
              "textInput", List.of("textInputValue"),
              "numberInput", List.of("10"))))
      );
      UUID testSubflowLogicUUID = ((Map<String, UUID>) session.getAttribute(SUBMISSION_MAP_NAME)).get("yetAnotherTestFlow");
      Submission submissionAfterFirstPost = submissionRepositoryService.findById(testSubflowLogicUUID).get();
      List<Map<String, Object>> iterationsAfterFirstPost = (List<Map<String, Object>>) submissionAfterFirstPost.getInputData().get("subflowWithAGetAndThenAPost");
      String uuidString = (String) iterationsAfterFirstPost.get(0).get("uuid");

      mockMvc.perform(get("/flow/yetAnotherTestFlow/getScreen/navigation?uuid=" + uuidString).session(session))
          .andExpect(status().is3xxRedirection());

      String navigationUrl = "/flow/yetAnotherTestFlow/subflowAddItemPage2/navigation?uuid=" + uuidString;
      postToUrlExpectingSuccess("/flow/yetAnotherTestFlow/subflowAddItemPage2", navigationUrl,
              Map.of(), uuidString);
      assertThat(followRedirectsForUrl(navigationUrl)).isEqualTo("/flow/yetAnotherTestFlow/testReviewScreen");
      Submission submissionAfterSecondPost = submissionRepositoryService.findById(testSubflowLogicUUID).get();
      List<Map<String, Object>> iterationsAfterSecondPost = (List<Map<String, Object>>) submissionAfterSecondPost.getInputData().get("subflowWithAGetAndThenAPost");
      assertThat((Boolean) iterationsAfterSecondPost.get(0).get("iterationIsComplete")).isTrue();
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

  @Nested
  public class MultiFlowTests {
    // tests that related to testing out changing flows in the middle of a flow to ensure
    // that no data is lost

    @Test
    public void multipleFlowsResultInMultipleSubmissionsNoDataLost() throws Exception {
      // session does not have to know about the flows yet, as the flows will be
      // added once the post occurs
      mockMvc.perform(post("/flow/testFlow/inputs")
          .session(session)
          .params(new LinkedMultiValueMap<>(Map.of(
              "textInput", List.of("firstFlowTextInputValue"),
              "numberInput", List.of("10"))))
      );

      mockMvc.perform(post("/flow/otherTestFlow/inputs")
          .session(session)
          .params(new LinkedMultiValueMap<>(Map.of(
              "textInput", List.of("secondFlowTextInputValue"),
              "numberInput", List.of("20"),
              "phoneInput", List.of("(555) 123-1234"))))
      );

      Map<String, UUID> submissionMap = (Map) session.getAttribute(SUBMISSION_MAP_NAME);

      assertThat(submissionMap.containsKey("testFlow")).isTrue();
      assertThat(submissionMap.containsKey("otherTestFlow")).isTrue();
      assertThat(submissionMap.size()).isEqualTo(2);

      Optional<Submission> testFlowSubmission = submissionRepositoryService.findById(submissionMap.get("testFlow"));
      Optional<Submission> otherTestFlowSubmission = submissionRepositoryService.findById(submissionMap.get("otherTestFlow"));
      assertThat(testFlowSubmission.isPresent()).isTrue();
      assertThat(otherTestFlowSubmission.isPresent()).isTrue();
      assertThat(testFlowSubmission.get().getInputData().size()).isEqualTo(2);
      assertThat(otherTestFlowSubmission.get().getInputData().size()).isEqualTo(3);

      assertThat(testFlowSubmission.get().getInputData().get("textInput")).isEqualTo("firstFlowTextInputValue");
      assertThat(testFlowSubmission.get().getInputData().get("numberInput")).isEqualTo("10");
      assertThat(testFlowSubmission.get().getInputData().get("phoneInput")).isEqualTo(null);
      assertThat(otherTestFlowSubmission.get().getInputData().get("textInput")).isEqualTo("secondFlowTextInputValue");
      assertThat(otherTestFlowSubmission.get().getInputData().get("numberInput")).isEqualTo("20");
      assertThat(otherTestFlowSubmission.get().getInputData().get("phoneInput")).isEqualTo("(555) 123-1234");
    }

    @Test
    public void multipleFlowsInSubflowsNoDataLost() throws Exception {
      // session doesn't have to know about the two different flows yet
      // as they will get put in session during the posts
      mockMvc.perform(post("/flow/testFlow/subflowAddItem/new")
          .session(session)
          .params(new LinkedMultiValueMap<>(Map.of(
              "firstNameSubflow", List.of("Subflow testFlow Name"),
              "textInputSubflow", List.of("Subflow testFlow Text Input"))))
      );

      mockMvc.perform(post("/flow/otherTestFlow/subflowAddItem/new")
          .session(session)
          .params(new LinkedMultiValueMap<>(Map.of(
              "numberInputSubflow", List.of("23"),
              "moneyInputSubflow", List.of("10.00"),
              "phoneInputSubflow", List.of("(413) 123-4567"))))
      );

      Map<String, UUID> submissionMap = (Map) session.getAttribute(SUBMISSION_MAP_NAME);

      assertThat(submissionMap.containsKey("testFlow")).isTrue();
      assertThat(submissionMap.containsKey("otherTestFlow")).isTrue();
      assertThat(submissionMap.size()).isEqualTo(2);

      Optional<Submission> testFlowSubmission = submissionRepositoryService.findById(submissionMap.get("testFlow"));
      Optional<Submission> otherTestFlowSubmission = submissionRepositoryService.findById(submissionMap.get("otherTestFlow"));
      assertThat(testFlowSubmission.isPresent()).isTrue();
      assertThat(testFlowSubmission.get().getInputData().containsKey("testSubflow")).isTrue();
      assertThat(otherTestFlowSubmission.isPresent()).isTrue();
      assertThat(otherTestFlowSubmission.get().getInputData().containsKey("testSubflow")).isTrue();

      List<Object> testFlowInputData = (List<Object>) (testFlowSubmission.get().getInputData()).get("testSubflow");
      List<Object> otherTestFlowInputData = (List<Object>) (otherTestFlowSubmission.get().getInputData()).get("testSubflow");
      Map<String, Object> testFlowIteration = (Map<String, Object>) testFlowInputData.get(0);
      Map<String, Object> otherTestFlowIteration = (Map<String, Object>) otherTestFlowInputData.get(0);

      assertThat(testFlowInputData.size()).isEqualTo(1);
      assertThat(otherTestFlowInputData.size()).isEqualTo(1);
      assertThat(testFlowIteration.size()).isEqualTo(4);
      assertThat(otherTestFlowIteration.size()).isEqualTo(5);

      assertThat(testFlowIteration.get("firstNameSubflow")).isEqualTo("Subflow testFlow Name");
      assertThat(testFlowIteration.get("textInputSubflow")).isEqualTo("Subflow testFlow Text Input");
      assertThat(otherTestFlowIteration.get("numberInputSubflow")).isEqualTo("23");
      assertThat(otherTestFlowIteration.get("moneyInputSubflow")).isEqualTo("10.00");
      assertThat(otherTestFlowIteration.get("phoneInputSubflow")).isEqualTo("(413) 123-4567");
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

    ResultActions resultActions = postToUrlExpectingSuccessRedirectPattern(
            "/flow/testFlow/subflowAddItem/new",
            "/flow/testFlow/subflowAddItem/navigation?uuid=" + UUID_PATTERN_STRING,
            paramsPage1);
    String redirectedUrl = resultActions.andReturn().getResponse().getRedirectedUrl();
    String iterationUuid = redirectedUrl.substring(redirectedUrl.lastIndexOf('=') + 1);
    assertThat(followRedirectsForUrl("/flow/testFlow/subflowAddItem/navigation?uuid=" + iterationUuid))
            .isEqualTo("/flow/testFlow/subflowAddItemPage2/" + iterationUuid);

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

    String pageNamePage2 = "subflowAddItemPage2/" + iterationUuid;
    postExpectingFailure(pageNamePage2, paramsPage2, pageNamePage2);

    var page2 = new FormScreen(getPage("subflowAddItemPage2/" + iterationUuid));
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
