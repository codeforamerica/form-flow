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

  @Test
  public void testDeleteConfirmationWithOneIterationAndGoBack() {
    runTestSubflowIterations(1);

    assertThat(testPage.getTitle()).isEqualTo("Review Screen");
    testPage.clickLink("delete");
    assertThat(testPage.getTitle()).isEqualTo("Delete the iteration?");
    testPage.clickButton("No, keep it!");

    assertThat(testPage.getTitle()).isEqualTo("Review Screen");

    assertThat(testPage.findElementsByClass("subflow-edit")).isNotNull();
    testPage.clickLink("delete");

    testPage.clickButton("Yes! Delete it!");
    assertThat(testPage.getTitle()).isEqualTo("Subflow Entry Screen");

    testPage.goBack();
    assertThat(testPage.getTitle()).isEqualTo("Delete the iteration?");
    assertThat(testPage.getHeader()).isEqualTo("Nothing to delete!");

    testPage.clickButton("Let's go back!");
    assertThat(testPage.getTitle()).isEqualTo("Subflow Entry Screen");
  }

  @Test
  public void testDeleteConfirmationWithMultipleIterationsAndGoBack() {
    runTestSubflowIterations(2);

    assertThat(testPage.getTitle()).isEqualTo("Review Screen");
    testPage.clickLink("delete");

    assertThat(testPage.getTitle()).isEqualTo("Delete the iteration?");
    testPage.clickButton("No, keep it!");

    assertThat(testPage.getTitle()).isEqualTo("Review Screen");

    assertThat(testPage.findElementsByClass("subflow-edit")).isNotNull();
    testPage.clickLink("delete");

    testPage.clickButton("Yes! Delete it!");
    assertThat(testPage.getTitle()).isEqualTo("Review Screen");

    testPage.goBack();
    assertThat(testPage.getTitle()).isEqualTo("Delete the iteration?");
    assertThat(testPage.getHeader()).isEqualTo("Nothing to delete!");

    testPage.clickButton("Let's go back!");
    assertThat(testPage.getTitle()).isEqualTo("Review Screen");
  }

  /**
   * Populate iterations with random data.  Use this only when the data itself
   * is unimportant. This will drop you onto the review page for the 'testSubflow' subflow.
   *
   * @param numberToRun
   */
  private void runTestSubflowIterations(int numberToRun) {
    baseUrl = "http://localhost:%s/%s".formatted(localServerPort,
        "flow/testFlow/testEntryScreen");
    driver.navigate().to(baseUrl);

    assertThat(testPage.getTitle()).isEqualTo("Subflow Entry Screen");
    testPage.clickContinue();

    int i = 0;
    while (i < numberToRun) {
      if (i != 0) {
        testPage.clickButton("Add Another");
      }
      assertThat(testPage.getTitle()).isEqualTo("Subflow Page");
      testPage.enter("textInputSubflow", "testFlow: textInput");
      testPage.enter("areaInputSubflow", "testFlow: areaInput");
      testPage.enter("dateSubflowDay", "10");
      testPage.enter("dateSubflowMonth", "10");
      testPage.enter("dateSubflowYear", "2010");
      testPage.enter("numberInputSubflow", "99");
      testPage.clickElementById("checkboxSetSubflow-Checkbox-A-label");
      testPage.clickElementById("checkboxInputSubflow-checkbox-value");
      testPage.clickElementById("radioInputSubflow-Radio A-label");
      testPage.selectFromDropdown("selectInputSubflow", "Select A");
      testPage.enter("moneyInputSubflow", "110");
      testPage.enter("phoneInputSubflow", "(413) 111-1111");

      testPage.clickContinue();

      assertThat(testPage.getTitle()).isEqualTo("Subflow Page2");
      testPage.enter("textInputSubflowPage2", "testFlow: textInput");
      testPage.enter("areaInputSubflowPage2", "testFlow: areaInput");
      testPage.enter("dateSubflowPage2Day", "10");
      testPage.enter("dateSubflowPage2Month", "10");
      testPage.enter("dateSubflowPage2Year", "2010");
      testPage.enter("numberInputSubflowPage2", "10");
      testPage.clickElementById("checkboxSetSubflowPage2-Checkbox-A-label");
      testPage.clickElementById("checkboxInputSubflowPage2-checkbox-value");
      testPage.clickElementById("radioInputSubflowPage2-Radio A-label");
      testPage.selectFromDropdown("selectInputSubflowPage2", "Select A");
      testPage.enter("moneyInputSubflowPage2", "110");
      testPage.enter("phoneInputSubflowPage2", "(413) 111-1111");
      testPage.clickContinue();
      i++;
    }
  }
}
