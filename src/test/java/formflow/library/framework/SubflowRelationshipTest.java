package formflow.library.framework;

import static formflow.library.controllers.ScreenControllerTest.UUID_PATTERN_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;
import formflow.library.utilities.FormScreen;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
    postToUrlExpectingSuccessRedirectPattern(
            "/flow/testRelatedSubflows/housemateInfo/new",
            "/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("householdMemberFirstName", List.of("Not Alex"),
                    "householdMemberLastName", List.of("LastName")));
    Map<String, Object> iterationData = getMostRecentlyCreatedIterationData(session, "testRelatedSubflows", "household");
    mockMvc.perform(get("/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + iterationData.get("uuid")))
            .andExpect(status().is3xxRedirection());
    postToUrlExpectingSuccessRedirectPattern(
            "/flow/testRelatedSubflows/housemateInfo/new",
            "/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + UUID_PATTERN_STRING,
            Map.of("householdMemberFirstName", List.of("Alex"),
                    "householdMemberLastName", List.of("LastName")));
    iterationData = getMostRecentlyCreatedIterationData(session, "testRelatedSubflows", "household");
    mockMvc.perform(get("/flow/testRelatedSubflows/housemateInfo/navigation?uuid=" + iterationData.get("uuid")))
            .andExpect(status().is3xxRedirection());
    FormScreen incomeAmountsScreen = new FormScreen(getPageExpectingSuccess("testRelatedSubflows", "incomeTypes"));
    assertThat(incomeAmountsScreen.getElementById("header").text()).isEqualTo(
            "What sources does Alex LastName receive income from?");
    Submission submissionAfterSubflows = submissionRepositoryService.findById(submission.getId()).get();
    List<HashMap<String, Object>> incomeSubflow = (List<HashMap<String, Object>>) submissionAfterSubflows.getInputData()
            .get("income");
    List<HashMap<String, Object>> householdMembers = (List<HashMap<String, Object>>) submissionAfterSubflows.getInputData()
            .get("household");
    List<HashMap<String, Object>> filteredHouseholdMembers = householdMembers.stream()
            .filter(hm -> hm.get("householdMemberFirstName").equals("Alex"))
            .toList();
    assertThat(
            incomeSubflow.getFirst().get("householdMemberIncome").equals(filteredHouseholdMembers.get(0).get("uuid"))).isTrue();
  }
}
