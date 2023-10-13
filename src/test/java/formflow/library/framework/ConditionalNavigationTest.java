package formflow.library.framework;

import static formflow.library.FormFlowController.SUBMISSION_MAP_NAME;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-conditional-navigation.yaml"})
public class ConditionalNavigationTest extends AbstractMockMvcTest {

  @SpyBean
  private SubmissionRepositoryService submissionRepositoryService;

  @BeforeEach
  public void setup() {
    submission = Submission.builder().id(UUID.randomUUID()).urlParams(new HashMap<>()).inputData(new HashMap<>()).build();
    when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
    setFlowInfoInSession(session,
        "testFlow", submission.getId()
    );
  }

  @Test
  void shouldGoToPageWhoseConditionIsSatisfied() throws Exception {
    continueExpectingNextPageTitle("first", "Second Page");
  }

  @Test
  void shouldNotGoToPageWhoseConditionIsNotSatisfied() throws Exception {
    continueExpectingNextPageTitle("second", "Last Page");
  }

  @Test
  void shouldGoToTheFirstPageInNextPagesIfThereAreTwoPagesWithNoConditions() throws Exception {
    continueExpectingNextPageTitle("third", "First Page");
  }

  @Test
  void shouldGoToOtherScreenIfOneHasNonExistentCondition() throws Exception {
    continueExpectingNextPageTitle("other", "Last Page");
  }

  @Test
  void shouldGoToNextConditionalSubflowPage() throws Exception {
    ResultActions resultActions = postWithoutData("fourth/new");
    resultActions.andExpect(redirectedUrl("/flow/testFlow/last"));
  }
}
