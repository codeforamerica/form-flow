package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-landmark-flow.yaml"}, webEnvironment = RANDOM_PORT)
public class DataInterceptorJourneyTest extends AbstractBasePageTest {
  @Test
  void interceptorShouldRedirectToLandingPageIfSessionIsNull() {
    assertThat(testPage.getTitle()).isEqualTo("Test index page");
    testPage.clickButton("Start the test flow");
    // firstScreen
    testPage.enter("firstName", "Testy");
    testPage.clickContinue();
    // nonFormPage
    String currentSessionCookie = getCurrentSessionCookie();
    assertThat(currentSessionCookie).isNotNull();
    deleteSessionCookie();
    assertThat(getCurrentSessionCookie()).isNull();
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("Test index page");
  }

  protected String getCurrentSessionCookie() {
    if (driver.manage().getCookieNamed("SESSION") == null){
      return null;
    }
    return driver.manage().getCookieNamed("SESSION").getValue();
  }

  protected void deleteSessionCookie(){
    System.out.println("SessionId before deletion: " + getCurrentSessionCookie());
    driver.manage().deleteCookieNamed("SESSION");
    System.out.println("SessionId after deletion: " + getCurrentSessionCookie());
    Set<Cookie> cookies = driver.manage().getCookies();
    cookies.forEach(cookie -> System.out.println(cookie.getName() + ": " + cookie.getValue()));
  }
}
