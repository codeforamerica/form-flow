package formflow.library.framework;

import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static formflow.library.controllers.ScreenControllerTest.UUID_PATTERN_STRING;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-conditional-navigation.yaml"})
public class ConditionalNavigationTest extends AbstractMockMvcTest {

  @BeforeEach
  public void setup() {
    submission = Submission.builder()
            .id(null) // Let Hibernate assign the ID
            .urlParams(new HashMap<>())
            .inputData(new HashMap<>())
            .flow("testFlow")
            .build();

    // Persist the submission to ensure it's managed
    submission = submissionRepositoryService.save(submission);
  }

  @Test
  void shouldGoToPageWhoseConditionIsSatisfied() throws Exception {
    setFlowInfoInSession(session, "testFlow", submission.getId());
    continueExpectingNextPageTitle("testFlow", "first", "Second Page");
  }

  @Test
  void shouldNotGoToPageWhoseConditionIsNotSatisfied() throws Exception {
    setFlowInfoInSession(session, "testFlow", submission.getId());
    continueExpectingNextPageTitle("testFlow", "second", "Last Page");
  }

  @Test
  void shouldGoToTheFirstPageInNextPagesIfThereAreTwoPagesWithNoConditions() throws Exception {
    setFlowInfoInSession(session, "testFlow", submission.getId());
    continueExpectingNextPageTitle("testFlow", "third", "First Page");
  }

  @Test
  void shouldGoToOtherScreenIfOneHasNonExistentCondition() throws Exception {
    setFlowInfoInSession(session, "testFlow", submission.getId());
    continueExpectingNextPageTitle("testFlow", "other", "Last Page");
  }

  @Test
  void shouldGoToNextConditionalSubflowPage() throws Exception {
    setFlowInfoInSession(session, "testFlow", submission.getId());
    postToUrlExpectingSuccessRedirectPattern(
        "/flow/testFlow/fourth/new",
        "/flow/testFlow/fourth/navigation?uuid=" + UUID_PATTERN_STRING,
        new HashMap<>());
    Map<String, Object> iterationData = getMostRecentlyCreatedIterationData(session, "testFlow", "testSubflow");
    assertThat(followRedirectsForUrl("/flow/testFlow/fourth/navigation?uuid=" + iterationData.get("uuid")))
        .isEqualTo("/flow/testFlow/last");
  }

  @Test
  void shouldSkipScreensUntilScreenConditionIsTrue() throws Exception {
    setFlowInfoInSession(session, "conditionsTestFlow", submission.getId());
    String actualNextPage = getUrlExpectingSuccessRedirectPattern("/flow/conditionsTestFlow/skipFirst");
    assertThat(actualNextPage).isEqualTo("/flow/conditionsTestFlow/viewThird");
  }

  @Test
  void shouldSkipScreensUntilScreenAndNextConditionsAreTrue() throws Exception {
    setFlowInfoInSession(session, "conditionsTestFlow", submission.getId());
    String actualNextPage = getUrlExpectingSuccessRedirectPattern("/flow/conditionsTestFlow/sixth");
    assertThat(actualNextPage).isEqualTo("/flow/conditionsTestFlow/other");
  }

  @Test
  void shouldSkipSubflowScreensUntilConditionIsTrue() throws Exception {
    setFlowInfoInSession(session, "conditionsTestFlow", submission.getId());
    postToUrlExpectingSuccessRedirectPattern(
        "/flow/conditionsTestFlow/fourth/new",
        "/flow/conditionsTestFlow/fourth/navigation?uuid=" + UUID_PATTERN_STRING,
        new HashMap<>());
    Map<String, Object> iterationData = getMostRecentlyCreatedIterationData(session, "conditionsTestFlow", "testSubflow");
    assertThat(followRedirectsForUrl("/flow/conditionsTestFlow/fourth/navigation?uuid=" + iterationData.get("uuid")))
        .isEqualTo("/flow/conditionsTestFlow/other");
  }
}
