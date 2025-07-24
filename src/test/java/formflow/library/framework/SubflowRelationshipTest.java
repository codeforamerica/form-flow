package formflow.library.framework;

import static formflow.library.controllers.ScreenControllerTest.UUID_PATTERN_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.web.servlet.function.RequestPredicates.param;

import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;
import formflow.library.utilities.FormScreen;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-subflow-relationships.yaml"})
public class SubflowRelationshipTest extends AbstractMockMvcTest {

  protected Submission submission;

  @BeforeEach
  public void setup() {
    submission = submissionRepositoryService.save(
            Submission.builder()
                    .flow("testRelatedSubflows")
                    .inputData(new HashMap<>())
                    .urlParams(new HashMap<>())
                    .build()
    );
  }

  @Test
  void shouldOnlyLoopOverFilteredIterations() throws Exception {
    setFlowInfoInSession(session, "testRelatedSubflows", submission.getId());
    // Create first person, not named alex, should be filtered out
    postToUrlExpectingSuccessRedirectPattern(
            "/flow/testRelatedSubflows/housemateInfo/new",
            "/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("householdMemberFirstName", List.of("Not Alex"),
                    "householdMemberLastName", List.of("LastName")));
    Map<String, Object> iterationData = getMostRecentlyCreatedIterationData(session, "testRelatedSubflows", "household");
    
    // Perform get to complete iteration
    mockMvc.perform(get("/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + iterationData.get("uuid")))
            .andExpect(status().is3xxRedirection());
    
    // Post create next person, named Alex, should not be filtered out
    postToUrlExpectingSuccessRedirectPattern(
            "/flow/testRelatedSubflows/housemateInfo/new",
            "/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("householdMemberFirstName", List.of("Alex"),
                    "householdMemberLastName", List.of("LastName")));
    iterationData = getMostRecentlyCreatedIterationData(session, "testRelatedSubflows", "household");
    
    // Perform get to complete iteration
    mockMvc.perform(get("/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + iterationData.get("uuid")))
            .andExpect(status().is3xxRedirection());

    // Assert that the screen header is correct and that only the Alex iteration is present
    FormScreen incomeAmountsScreen = new FormScreen(getPageExpectingSuccess("testRelatedSubflows", "incomeTypes"));
    assertThat(incomeAmountsScreen.getElementById("header").text()).isEqualTo(
            "What sources does Alex LastName receive income from?");
    
    // Assert that the household subflow only contains the Alex iteration
    Submission submissionAfterSubflows = submissionRepositoryService.findById(submission.getId()).get();
    List<HashMap<String, Object>> incomeSubflow = (List<HashMap<String, Object>>) submissionAfterSubflows.getInputData()
            .get("income");
    List<HashMap<String, Object>> householdMembers = (List<HashMap<String, Object>>) submissionAfterSubflows.getInputData()
            .get("household");
    List<HashMap<String, Object>> filteredHouseholdMembers = householdMembers.stream()
            .filter(hm -> hm.get("householdMemberFirstName").equals("Alex"))
            .toList();
    // Only Alex is present in income subflow
    assertThat(incomeSubflow.size()).isEqualTo(1);
    assertThat(incomeSubflow.getFirst().get("householdMemberIncome").equals(filteredHouseholdMembers.get(0).get("uuid"))).isTrue();
  }
  
  @Test
  void shouldLoopOverRepeatForSelectionsWhenRepeatForIsSet() throws Exception {
    setFlowInfoInSession(session, "testRelatedSubflows", submission.getId());
    // Create first household member
    postToUrlExpectingSuccessRedirectPattern(
            "/flow/testRelatedSubflows/housemateInfo/new",
            "/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("householdMemberFirstName", List.of("Alex"),
                    "householdMemberLastName", List.of("LastName")));
    Map<String, Object> iterationData = getMostRecentlyCreatedIterationData(session, "testRelatedSubflows", "household");
    
    // Perform get to complete iteration
    mockMvc.perform(get("/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + iterationData.get("uuid")))
            .andExpect(status().is3xxRedirection());
    
    // Create second household member
    postToUrlExpectingSuccessRedirectPattern(
            "/flow/testRelatedSubflows/housemateInfo/new",
            "/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("householdMemberFirstName", List.of("Alex"),
                    "householdMemberLastName", List.of("Another LastName")));
    iterationData = getMostRecentlyCreatedIterationData(session, "testRelatedSubflows", "household");
    
    // Perform get to complete iteration
    mockMvc.perform(get("/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + iterationData.get("uuid")))
            .andExpect(status().is3xxRedirection());
    FormScreen incomeTypesScreen = new FormScreen(getPageExpectingSuccess("testRelatedSubflows", "incomeTypes"));
    assertThat(incomeTypesScreen.getElementById("header").text()).isEqualTo(
            "What sources does Alex LastName receive income from?");
    
    // Get the UUID of the first household member
    Submission submissionWithHouseholdMembers = submissionRepositoryService.findById(submission.getId()).get();
    List<HashMap<String, Object>> householdSubflow = (List<HashMap<String, Object>>) submissionWithHouseholdMembers.getInputData().get("household");
    HashMap<String, Object> firstHMIteration = householdSubflow.stream()
            .filter(hm -> hm.get("householdMemberLastName").equals("LastName")).toList().getFirst();
    String uuidOfFirstHM = firstHMIteration.get("uuid").toString();
    
    // Get the UUID of the first income iteration which should contain the relation to the first household member
    List<HashMap<String, Object>> incomeSubflow = (List<HashMap<String, Object>>) submissionWithHouseholdMembers.getInputData().get("income");
    String uuidOfIncomeIterationForFirstHM = incomeSubflow.stream().filter(iteration -> iteration.get("householdMemberIncome").equals(uuidOfFirstHM))
            .toList().getFirst().get("uuid").toString();
    // Post to income types with the first household member's UUID
    ResultActions resultActions = postToUrlExpectingSuccessRedirectPattern(
            "/flow/testRelatedSubflows/incomeTypes/" + uuidOfIncomeIterationForFirstHM,
            "/flow/testRelatedSubflows/incomeTypes/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("incomeTypes[]", List.of("incomeJob", "incomeSelf")));
    // Follow the redirect to the next screen
    String redirectURL = resultActions.andReturn().getResponse().getHeader("Location");
    String repeatForIterationURL = mockMvc.perform(get(redirectURL)).andReturn().getResponse().getHeader("Location");
    FormScreen incomeAmountsJobIncome = new FormScreen(mockMvc.perform(get(repeatForIterationURL)));
    // Get the form action URL to know where to POST
    String formActionUrl = incomeAmountsJobIncome.getElementsByTag("form").get(0).getAllElements().attr("action");
    // Assert that the header is correct for the first household members first repeat for iteration (the first income type selection they made)
    assertThat(incomeAmountsJobIncome.getElementById("header").text()).isEqualTo(
            "How much money did Alex LastName receive in the last 12 months from incomeJob?");
    // Post and follow the redirect to get to the next repeat for iteration
    ResultActions repeatForIncomeJobFirstHHMResult = postToUrlExpectingSuccessRedirectPattern(
            formActionUrl,
            "/flow/testRelatedSubflows/incomeAmounts/navigation?uuid=" + UUID_PATTERN_STRING + "&repeatForIterationUuid=" + UUID_PATTERN_STRING,
            Map.of("incomeJobAmount", List.of("100")));

    String nextRedirectURL = repeatForIncomeJobFirstHHMResult.andReturn().getResponse().getHeader("Location");
    String nextRepeatForIterationURL = mockMvc.perform(get(nextRedirectURL)).andReturn().getResponse().getHeader("Location");
    FormScreen incomeSelfScreen = new FormScreen(mockMvc.perform(get(nextRepeatForIterationURL)));
    // Get the form action URL to know where to POST
    String secondHHMFormActionURL = incomeSelfScreen.getElementsByTag("form").get(0).getAllElements().attr("action");
    // Assert that the header is correct for the second repeat for iteration (incomeSelf)
    assertThat(incomeSelfScreen.getElementById("header").text()).isEqualTo(
            "How much money did Alex LastName receive in the last 12 months from incomeSelf?");
    // Post and follow the redirect for the next repeat for iteration
    ResultActions repeatForIncomeSelfFirstHHMResult = postToUrlExpectingSuccessRedirectPattern(
            secondHHMFormActionURL,
            "/flow/testRelatedSubflows/incomeAmounts/navigation?uuid=" + UUID_PATTERN_STRING + "&repeatForIterationUuid=" + UUID_PATTERN_STRING,
            Map.of("incomeSelfAmount", List.of("100")));
    String incomeSelfRedirectUrl = repeatForIncomeSelfFirstHHMResult.andReturn().getResponse().getHeader("Location");
    String repeatForIterationURLSecondHHM = mockMvc.perform(get(incomeSelfRedirectUrl)).andReturn().getResponse().getHeader("Location");
    FormScreen incomeJobScreenSecondHHM = new FormScreen(mockMvc.perform(get(repeatForIterationURLSecondHHM)));
    // Assert that the header is correct for the incomeTypes screen for the second household member
    assertThat(incomeJobScreenSecondHHM.getElementById("header").text()).isEqualTo(
            "What sources does Alex Another LastName receive income from?");
    // Get the UUID of the second household member
    Submission updatedSubmissionWithHouseholdMembers = submissionRepositoryService.findById(submission.getId()).get();
    List<HashMap<String, Object>> updatedHouseholdSubflow = (List<HashMap<String, Object>>) updatedSubmissionWithHouseholdMembers.getInputData().get("household");
    HashMap<String, Object> secondHMIteration = updatedHouseholdSubflow.stream()
            .filter(hm -> hm.get("householdMemberLastName").equals("Another LastName")).toList().getFirst();
    String uuidOfSecondHM = secondHMIteration.get("uuid").toString();

    // Get the UUID of the second income iteration which should contain the relation to the second household member
    List<HashMap<String, Object>> updatedIncomeSubflow = (List<HashMap<String, Object>>) updatedSubmissionWithHouseholdMembers.getInputData().get("income");
    String uuidOfIncomeIterationForSecondHM = updatedIncomeSubflow.stream().filter(iteration -> iteration.get("householdMemberIncome").equals(uuidOfSecondHM))
            .toList().getFirst().get("uuid").toString();
    // Post to income types with the second household member's UUID
    ResultActions secondHMResultActions = postToUrlExpectingSuccessRedirectPattern(
            "/flow/testRelatedSubflows/incomeTypes/" + uuidOfIncomeIterationForSecondHM,
            "/flow/testRelatedSubflows/incomeTypes/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("incomeTypes[]", List.of("incomeSelf")));
    // Follow the redirect to the next screen
    String secondHMRedirectURL = secondHMResultActions.andReturn().getResponse().getHeader("Location");
    String secondHMRepeatForIterationURL = mockMvc.perform(get(secondHMRedirectURL)).andReturn().getResponse().getHeader("Location");
    FormScreen incomeAmountsIncomeSelf = new FormScreen(mockMvc.perform(get(secondHMRepeatForIterationURL)));
    // Get the form action URL to know where to POST
    String secondHMIterationFormActionURL = incomeAmountsIncomeSelf.getElementsByTag("form").get(0).getAllElements().attr("action");
    // Assert that the header is correct for the second household members only repeat for iteration (the only income type selection they made)
    assertThat(incomeAmountsIncomeSelf.getElementById("header").text()).isEqualTo(
            "How much money did Alex Another LastName receive in the last 12 months from incomeSelf?");
    // Post and follow the redirect to get to the next repeat for iteration
    ResultActions repeatForIncomeJobSecondHHMResult = postToUrlExpectingSuccessRedirectPattern(
            secondHMIterationFormActionURL,
            "/flow/testRelatedSubflows/incomeAmounts/navigation?uuid=" + UUID_PATTERN_STRING + "&repeatForIterationUuid=" + UUID_PATTERN_STRING,
            Map.of("incomeSelfAmount", List.of("100")));
    String secondHMFinalRedirectURL = repeatForIncomeJobSecondHHMResult.andReturn().getResponse().getHeader("Location");
    String secondHMFinalURL = mockMvc.perform(get(secondHMFinalRedirectURL)).andReturn().getResponse().getHeader("Location");
    // We are now on the review screen meaning all repeat for iterations have been completed
    assertThat(secondHMFinalURL).isEqualTo("/flow/testRelatedSubflows/annualHouseholdIncome");
  }
}
