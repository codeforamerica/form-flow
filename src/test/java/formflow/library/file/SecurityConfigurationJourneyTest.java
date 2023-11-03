package formflow.library.file;

import static org.assertj.core.api.Assertions.assertThat;
import formflow.library.utilities.AbstractBasePageTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(properties = "form-flow.path=flows-config/test-landmark-flow.yaml", webEnvironment = WebEnvironment.RANDOM_PORT)
public class SecurityConfigurationJourneyTest extends AbstractBasePageTest {

  @Test
  void sessionCookieContainsHttpOnlyAndSecureHeaders(){
    assertThat(testPage.getTitle()).isEqualTo("Test index page");
    testPage.clickButton("Start the test flow");
    assertThat(testPage.getTitle()).isEqualTo("First Page");
    assertThat(driver.manage().getCookieNamed("SESSION").isSecure()).isEqualTo(true);
    assertThat(driver.manage().getCookieNamed("SESSION").isHttpOnly()).isEqualTo(true);
  }
}
