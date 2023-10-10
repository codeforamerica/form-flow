package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"},
    webEnvironment = RANDOM_PORT)
public class ScreenControllerJourneyTest extends AbstractBasePageTest {

  private final String firstFlow = "testFlow";

  private final String secondFlow = "otherTestFlow";

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    startingPage = "flow/" + firstFlow + "/inputs";
    super.setUp();
  }

  @Test
  public void multiFlowJourneyTestDataPersists() {
    // "testFlow" flow
    assertThat(testPage.getTitle()).isEqualTo("Inputs Screen");
    testPage.enter("textInput", "testFlow: textInput");
    testPage.enter("areaInput", "testFlow: areaInput");
    testPage.enter("dateDay", "10");
    testPage.enter("dateMonth", "10");
    testPage.enter("dateYear", "2010");
    testPage.enter("moneyInput", "110");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("Test");

    // switch to other flow "testOtherFlow"
    baseUrl = "http://localhost:%s/%s".formatted(localServerPort,
        "flow/" + secondFlow + "/inputs");
    driver.navigate().to(baseUrl);

    assertThat(testPage.getTitle()).isEqualTo("Inputs Screen");

    // stop check that no values are set, as we are in other flow
    assertThat(testPage.getElementText("textInput")).isEmpty();
    assertThat(testPage.getElementText("areaInput")).isEmpty();
    assertThat(testPage.getInputValue("dateDay")).isEmpty();

    // enter some data for otherTestFlow
    testPage.enter("textInput", "otherTestFlow: textInput");
    testPage.enter("areaInput", "otherTestFlow: areaInput");
    testPage.enter("dateDay", "11");
    testPage.enter("dateMonth", "11");
    testPage.enter("dateYear", "2011");
    testPage.enter("moneyInput", "111");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("Test");

    baseUrl = "http://localhost:%s/%s".formatted(localServerPort,
        "flow/" + firstFlow + "/inputs");
    driver.navigate().to(baseUrl);

    assertThat(testPage.getTitle()).isEqualTo("Inputs Screen");

    assertThat(testPage.getInputValue("textInput")).isEqualTo("testFlow: textInput");
    assertThat(testPage.getElementText("areaInput")).isEqualTo("testFlow: areaInput");
    assertThat(testPage.getInputValue("dateDay")).isEqualTo("10");
    assertThat(testPage.getInputValue("dateMonth")).isEqualTo("10");
    assertThat(testPage.getInputValue("dateYear")).isEqualTo("2010");
    assertThat(testPage.getInputValue("moneyInput")).isEqualTo("110");

    baseUrl = "http://localhost:%s/%s".formatted(localServerPort,
        "flow/" + secondFlow + "/inputs");
    driver.navigate().to(baseUrl);

    assertThat(testPage.getTitle()).isEqualTo("Inputs Screen");

    assertThat(testPage.getInputValue("textInput")).isEqualTo("otherTestFlow: textInput");
    assertThat(testPage.getElementText("areaInput")).isEqualTo("otherTestFlow: areaInput");
    assertThat(testPage.getInputValue("dateDay")).isEqualTo("11");
    assertThat(testPage.getInputValue("dateMonth")).isEqualTo("11");
    assertThat(testPage.getInputValue("dateYear")).isEqualTo("2011");
    assertThat(testPage.getInputValue("moneyInput")).isEqualTo("111");
  }
}
