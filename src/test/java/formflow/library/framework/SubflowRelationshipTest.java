package formflow.library.framework;

import static formflow.library.controllers.ScreenControllerTest.UUID_PATTERN_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;
import formflow.library.utilities.FormScreen;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-subflow-relationships.yaml"})
public class SubflowRelationshipTest extends AbstractMockMvcTest {

  private static final String FLOW_NAME = "testRelatedSubflows";
  private static final String HOUSEMATE_INFO_NEW_URL = "/flow/testRelatedSubflows/housemateInfo/new";
  private static final String HOUSEMATE_INFO_NAV_URL = "/flow/testRelatedSubflows/housemateInfo/navigation?uuid=";
  private static final String INCOME_TYPES_URL = "/flow/testRelatedSubflows/incomeTypes/";
  private static final String INCOME_AMOUNTS_URL = "/flow/testRelatedSubflows/incomeAmounts/";

  protected Submission submission;

  @BeforeEach
  public void setup() {
    submission = submissionRepositoryService.save(
            Submission.builder()
                    .flow(FLOW_NAME)
                    .inputData(new HashMap<>())
                    .urlParams(new HashMap<>())
                    .build()
    );
    setFlowInfoInSession(session, FLOW_NAME, submission.getId());
  }

  @Test
  void shouldOnlyLoopOverFilteredIterations() throws Exception {
    // Create first person (filtered out) and second person (included)
    createHouseholdMember("Not Alex", "LastName");
    createHouseholdMember("Alex", "LastName");

    // Verify only Alex appears in income types screen
    FormScreen incomeTypesScreen = new FormScreen(getPageExpectingSuccess(FLOW_NAME, "incomeTypes"));
    assertThat(incomeTypesScreen.getElementById("header").text())
            .isEqualTo("What sources does Alex LastName receive income from?");

    // Verify only Alex is present in income subflow
    Submission submissionAfterSubflows = submissionRepositoryService.findById(submission.getId()).get();
    List<HashMap<String, Object>> incomeSubflow = getSubflowData(submissionAfterSubflows, "income");
    List<HashMap<String, Object>> householdMembers = getSubflowData(submissionAfterSubflows, "household");

    List<HashMap<String, Object>> alexMembers = householdMembers.stream()
            .filter(hm -> "Alex".equals(hm.get("householdMemberFirstName")))
            .toList();

    assertThat(incomeSubflow).hasSize(1);
    assertThat(incomeSubflow.getFirst().get("householdMemberIncome"))
            .isEqualTo(alexMembers.getFirst().get("uuid"));
  }

  @Test
  void shouldLoopOverRepeatForSelectionsWhenRepeatForIsSet() throws Exception {
    // Create two household members
    createHouseholdMember("Alex", "LastName");
    createHouseholdMember("Alex", "Another LastName");

    // Navigate to income types screen to trigger income subflow creation
    FormScreen incomeTypesScreen = new FormScreen(getPageExpectingSuccess(FLOW_NAME, "incomeTypes"));
    assertThat(incomeTypesScreen.getElementById("header").text())
            .isEqualTo("What sources does Alex LastName receive income from?");

    // Process income types for first household member Alex LastName
    String firstHMUuid = getHouseholdMemberUuid("LastName");
    String firstIncomeUuid = getIncomeIterationUuid(firstHMUuid);

    // Submit income types for first member and follow navigation
    ResultActions firstMemberIncomeTypesResult = processIncomeTypesForMember(firstIncomeUuid, List.of("incomeJob", "incomeSelf"));
    String firstRepeatForUrl = followRedirectsToRepeatFor(firstMemberIncomeTypesResult);

    // Process repeat-for iterations for first member
    Map<String, String> firstMemberIcomeAmounts = new LinkedHashMap<>();
    firstMemberIcomeAmounts.put("incomeJob", "100");
    firstMemberIcomeAmounts.put("incomeSelf", "200");
    String nextUrl = processRepeatForIterations(firstRepeatForUrl, "Alex LastName",
            firstMemberIcomeAmounts);

    // Should now be on income types for second household member Alex Another LastName
    FormScreen secondMemberIncomeTypesScreen = new FormScreen(mockMvc.perform(get(nextUrl)));
    assertThat(secondMemberIncomeTypesScreen.getElementById("header").text())
            .isEqualTo("What sources does Alex Another LastName receive income from?");

    // Process income types for second household member Alex Another LastName
    String secondHMUuid = getHouseholdMemberUuid("Another LastName");
    String secondIncomeUuid = getIncomeIterationUuid(secondHMUuid);

    ResultActions secondMemberIncomeTypesResult = processIncomeTypesForMember(secondIncomeUuid, List.of("incomeSelf"));
    String secondRepeatForUrl = followRedirectsToRepeatFor(secondMemberIncomeTypesResult);

    // Process repeat-for iteration for second member
    Map<String, String> secondMemberIncomeAmounts = new LinkedHashMap<>();
    secondMemberIncomeAmounts.put("incomeSelf", "100");
    String finalUrl = processRepeatForIterations(secondRepeatForUrl, "Alex Another LastName",
            secondMemberIncomeAmounts);

    // Verify we reach the review screen
    assertThat(finalUrl).isEqualTo("/flow/testRelatedSubflows/annualHouseholdIncome");
  }

  private void createHouseholdMember(String firstName, String lastName) throws Exception {
    postToUrlExpectingSuccessRedirectPattern(
            HOUSEMATE_INFO_NEW_URL,
            HOUSEMATE_INFO_NAV_URL + UUID_PATTERN_STRING,
            Map.of("householdMemberFirstName", List.of(firstName),
                    "householdMemberLastName", List.of(lastName)));

    Map<String, Object> iterationData = getMostRecentlyCreatedIterationData(session, FLOW_NAME, "household");
    mockMvc.perform(get(HOUSEMATE_INFO_NAV_URL + iterationData.get("uuid")))
            .andExpect(status().is3xxRedirection());
  }

  private String getHouseholdMemberUuid(String lastName) {
    Submission currentSubmission = submissionRepositoryService.findById(submission.getId()).get();
    List<HashMap<String, Object>> householdSubflow = getSubflowData(currentSubmission, "household");

    return householdSubflow.stream()
            .filter(hm -> lastName.equals(hm.get("householdMemberLastName")))
            .findFirst()
            .map(hm -> hm.get("uuid").toString())
            .orElseThrow(() -> new RuntimeException("Household member not found: " + lastName));
  }

  private String getIncomeIterationUuid(String householdMemberUuid) {
    Submission currentSubmission = submissionRepositoryService.findById(submission.getId()).get();
    List<HashMap<String, Object>> incomeSubflow = getSubflowData(currentSubmission, "income");

    return incomeSubflow.stream()
            .filter(iteration -> householdMemberUuid.equals(iteration.get("householdMemberIncome")))
            .findFirst()
            .map(iteration -> iteration.get("uuid").toString())
            .orElseThrow(() -> new RuntimeException("Income iteration not found for household member: " + householdMemberUuid));
  }

  private ResultActions processIncomeTypesForMember(String incomeUuid, List<String> incomeTypes) throws Exception {
    return postToUrlExpectingSuccessRedirectPattern(
            INCOME_TYPES_URL + incomeUuid,
            "/flow/testRelatedSubflows/incomeTypes/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("incomeTypes[]", incomeTypes));
  }

  private String followRedirectsToRepeatFor(ResultActions resultActions) throws Exception {
    String redirectUrl = resultActions.andReturn().getResponse().getHeader("Location");
    String repeatForIterationUrl = mockMvc.perform(get(redirectUrl)).andReturn().getResponse().getHeader("Location");
    return repeatForIterationUrl;
  }

  private String processRepeatForIterations(String startingUrl, String memberName,
          Map<String, String> incomeAmounts) throws Exception {
    String currentUrl = startingUrl;

    for (Map.Entry<String, String> entry : incomeAmounts.entrySet()) {
      String incomeType = entry.getKey();
      String amount = entry.getValue();

      FormScreen incomeAmountScreen = new FormScreen(mockMvc.perform(get(currentUrl)));
      String formActionUrl = incomeAmountScreen.getElementsByTag("form").get(0).getAllElements().attr("action");

      // Verify header shows correct member and income type
      String expectedHeader = String.format("How much money did %s receive in the last 12 months from %s?",
              memberName, incomeType);
      assertThat(incomeAmountScreen.getElementById("header").text()).isEqualTo(expectedHeader);

      // Submit amount and get next URL
      ResultActions result = postToUrlExpectingSuccessRedirectPattern(
              formActionUrl,
              INCOME_AMOUNTS_URL + "navigation?uuid=" + UUID_PATTERN_STRING + "&repeatForIterationUuid=" + UUID_PATTERN_STRING,
              Map.of(incomeType + "Amount", List.of(amount)));

      String redirectUrl = result.andReturn().getResponse().getHeader("Location");
      currentUrl = mockMvc.perform(get(redirectUrl)).andReturn().getResponse().getHeader("Location");
    }

    return currentUrl;
  }

  @SuppressWarnings("unchecked")
  private List<HashMap<String, Object>> getSubflowData(Submission submission, String subflowName) {
    Object subflowData = submission.getInputData().get(subflowName);
    return (List<HashMap<String, Object>>) subflowData;
  }
}