package formflow.library.upload;

import formflow.library.utilities.AbstractBasePageTest;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-inputs.yaml"})

public class UploadJourneyTest extends AbstractBasePageTest {

  @Test
  void shouldDoSomeStuff() {
    takeSnapShot("test.png");
  }
}
