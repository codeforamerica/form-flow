package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-landmark-flow.yaml"}, webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {"form-flow.session-continuity-interceptor.enabled=true"})
public class DataInterceptorJourneyTest extends AbstractBasePageTest {

    @Test
    void interceptorShouldRedirectToLandingPageIfSessionIsNull() {
        assertThat(testPage.getTitle()).isEqualTo("Test index page");
        testPage.clickButton("Start the test flow");
        // firstScreen
        testPage.enter("firstName", "Testy");
        testPage.clickContinue();
        // nonFormPage - wait for cookie to be available
        String currentSessionCookie = await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> getCurrentSessionCookie(), cookie -> cookie != null);
        assertThat(currentSessionCookie).isNotNull();
        deleteSessionCookie();
        assertThat(getCurrentSessionCookie()).isNull();
        testPage.clickContinue();
        assertThat(testPage.getTitle()).isEqualTo("Test index page");
    }

    protected String getCurrentSessionCookie() {
        Cookie cookie = driver.manage().getCookieNamed("SESSION");
        if (cookie == null) {
            // Try JSESSIONID as fallback
            cookie = driver.manage().getCookieNamed("JSESSIONID");
        }
        return cookie != null ? cookie.getValue() : null;
    }

    protected void deleteSessionCookie() {
        driver.manage().deleteCookieNamed("SESSION");
        // Also try deleting JSESSIONID if it exists
        Cookie jsessionCookie = driver.manage().getCookieNamed("JSESSIONID");
        if (jsessionCookie != null) {
            driver.manage().deleteCookieNamed("JSESSIONID");
        }
    }
}
