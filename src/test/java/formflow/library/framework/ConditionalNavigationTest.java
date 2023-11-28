package formflow.library.framework;

import static formflow.library.controllers.ScreenControllerTest.UUID_PATTERN_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-conditional-navigation.yaml"})
public class ConditionalNavigationTest extends AbstractMockMvcTest {

  @BeforeEach
  public void setup() {
    submission = Submission.builder()
        .id(UUID.randomUUID())
        .urlParams(new HashMap<>())
        .inputData(new HashMap<>())
        .flow("testFlow")
        .build();
    when(submissionRepositoryService.findById(submission.getId()))
        .thenReturn(Optional.of(submission));
    setFlowInfoInSession(session, "testFlow", submission.getId());
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
    postToUrlExpectingSuccessRedirectPattern(
        "/flow/testFlow/fourth/new",
        "/flow/testFlow/fourth/navigation?uuid=" + UUID_PATTERN_STRING,
        new HashMap<>());
    Map<String, Object> iterationData = getMostRecentlyCreatedIterationData(session, "testFlow", "testSubflow");
    assertThat(followRedirectsForUrl("/flow/testFlow/fourth/navigation?uuid=" + iterationData.get("uuid")))
        .isEqualTo("/flow/testFlow/last");
  }
}
