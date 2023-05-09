package formflow.library.framework;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-conditional-navigation.yaml"})
public class ConditionalNavigationTest extends AbstractMockMvcTest {

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
