package formflow.library.upload;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-upload-flow.yaml"}, webEnvironment = RANDOM_PORT)
public class UploadUnitTests extends AbstractBasePageTest {

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    startingPage = "uploadFlow/docUploadUnit";
    super.setUp();
  }

  @Test
  void runQunitTests() {
    takeSnapShot("src/test/resources/qunit-results.png");
    await().until(
        () -> driver.findElements(By.id("qunit-testresult-display")).get(0).getAttribute("innerHTML")
            .contains("tests completed in"));
    assertThat(testPage.getElementText("qunit-testresult-display")).doesNotContain("global failure");
    assertThat(testPage.getElementText("qunit-testresult-display")).contains("0 failed");
  }
}
