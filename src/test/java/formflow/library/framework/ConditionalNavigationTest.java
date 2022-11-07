package formflow.library.framework;

import static org.mockito.Mockito.when;

import formflow.library.config.submission.Condition;
import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-conditional-navigation.yaml"})
public class ConditionalNavigationTest extends AbstractMockMvcTest {

  private Submission submission = new Submission();
  private Condition mockedCondition = Mockito.mock(Condition.class);

  @Test
  void shouldGoToPageWhoseConditionIsSatisfied() throws Exception {
    when(mockedCondition.run(submission)).thenReturn(true);
    postExpectingNextPageTitle("first", "firstName", "Testy McTesterson", "Second Page");
  }

  @Test
  void shouldNotGoToPageWhoseConditionIsNotSatisfied() throws Exception {
    when(mockedCondition.run(submission)).thenReturn(false);
    postExpectingNextPageTitle("first", "firstName", "Not Testy McTesterson", "Other Page");
  }

  @Test
  void shouldGoToTheFirstPageInNextPagesIfThereAreTwoPagesWithNoConditions() throws Exception {
    continueExpectingNextPageTitle("second", "Other Page");
  }
}
