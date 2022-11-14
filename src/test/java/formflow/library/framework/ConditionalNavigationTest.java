package formflow.library.framework;

import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-conditional-navigation.yaml"})
public class ConditionalNavigationTest extends AbstractMockMvcTest {

  @Test
  void shouldGoToPageWhoseConditionIsSatisfied() throws Exception {
    postExpectingNextPageTitle("first", "Second Page");
  }

  @Test
  void shouldNotGoToPageWhoseConditionIsNotSatisfied() throws Exception {
    postExpectingNextPageTitle("second", "Last Page");
  }

  @Test
  void shouldGoToTheFirstPageInNextPagesIfThereAreTwoPagesWithNoConditions() throws Exception {
    continueExpectingNextPageTitle("other", "First Page");
  }
}
