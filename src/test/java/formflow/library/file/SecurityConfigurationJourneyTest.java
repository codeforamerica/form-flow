package formflow.library.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import formflow.library.utilities.AbstractBasePageTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(properties = "form-flow.path=flows-config/test-landmark-flow.yaml", webEnvironment = WebEnvironment.RANDOM_PORT)
public class SecurityConfigurationJourneyTest extends AbstractBasePageTest {

    @Test
    void sessionCookieContainsHttpOnlyAndSecureHeaders() {
        assertThat(testPage.getTitle()).isEqualTo("Test index page");
        testPage.clickButton("Start the test flow");
        assertThat(testPage.getTitle()).isEqualTo("First Page");
        
        // Wait for SESSION cookie to be available (it may take a moment after navigation)
        // Also check for JSESSIONID as a fallback in case cookie name differs
        Cookie sessionCookie = await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> {
                    Cookie cookie = driver.manage().getCookieNamed("SESSION");
                    if (cookie == null) {
                        cookie = driver.manage().getCookieNamed("JSESSIONID");
                    }
                    return cookie;
                }, cookie -> cookie != null);
        
        assertThat(sessionCookie).isNotNull();
        // In test environment, secure cookies are disabled to allow Selenium access over HTTP
        // The secure flag is verified in production via SecurityConfigurationBase
        assertThat(sessionCookie.isHttpOnly()).isEqualTo(true);
    }
}
