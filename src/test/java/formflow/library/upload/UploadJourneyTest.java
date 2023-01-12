package formflow.library.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/upload.yaml"}, webEnvironment = RANDOM_PORT)
public class UploadJourneyTest extends AbstractBasePageTest {

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    startingPage = "uploadFlow/docUpload";
    super.setUp();
  }

  @Test
  void shouldDoSomeStuff() {
    takeSnapShot("uhmmmm.png");
    assertThat(testPage.getTitle()).isEqualTo("✔ Upload Test");
  }
}
