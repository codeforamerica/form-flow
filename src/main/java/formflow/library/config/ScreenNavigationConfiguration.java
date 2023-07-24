package formflow.library.config;

import formflow.library.data.Submission;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Screen navigation configuration class used to store navigation information about a specific screen.
 */
@Data
@Slf4j
public class ScreenNavigationConfiguration {

  private List<NextScreen> nextScreens = Collections.emptyList();
  private String subflow;
  private String onPostAction;
  private String crossFieldValidationAction;
  private String beforeSaveAction;
  private String beforeDisplayAction;
  private String afterSaveAction;

  public NextScreen getNextScreen(Submission submission, String subflowUuid, ConditionManager conditionManager) {
    NextScreen nextScreen;
    List<NextScreen> nextScreens = getConditionalNextScreen(submission, subflowUuid, conditionManager);

    if (isConditionalNavigation() && !nextScreens.isEmpty()) {
      nextScreen = nextScreens.get(0);
    } else {
      // TODO this needs to throw an error if there are more than 1 next screen that don't have a condition or more than one evaluate to true
      nextScreen = getNonConditionalNextScreen();
    }

    log.info("getNextScreenName: currentScreen:" + this + ", nextScreen: " + nextScreen.getName());
    // TODO throw a better error if the next screen doesn't exist (incorrect name / name is not in flow config)
    return nextScreen;
  }

  public boolean isNextScreenInSubflow(FlowConfiguration flowConfiguration, NextScreen nextScreen) {
    return flowConfiguration.getScreen(nextScreen.getName()).getSubflow() != null;
  }

  private List<NextScreen> getConditionalNextScreen(Submission submission, String subflowUuid,
      ConditionManager conditionManager) {
    return getNextScreens().stream()
        .filter(nextScreen -> conditionManager.conditionExists(nextScreen.getCondition()))
        .filter(nextScreen -> {
          if (getSubflow() != null) {
            return conditionManager.runCondition(nextScreen.getCondition(), submission, subflowUuid);
          } else {
            return conditionManager.runCondition(nextScreen.getCondition(), submission);
          }
        })
        .toList();
  }

  private Boolean isConditionalNavigation() {
    return getNextScreens().stream().anyMatch(nextScreen -> nextScreen.getCondition() != null);
  }

  private NextScreen getNonConditionalNextScreen() {
    return getNextScreens().stream()
        .filter(nxtScreen -> nxtScreen.getCondition() == null).toList().get(0);
  }

  public String getNextScreenUrlPathIfSubflow(String flow, String nextScreen, String iterationUuid) {
    return String.format("redirect:/flow/%s/%s/%s", flow, nextScreen, iterationUuid);
  }

  public String getNextScreenUrlPathIfNotSubflow(String flow, String nextScreen) {
    return String.format("redirect:/flow/%s/%s", flow, nextScreen);
  }

  public String getNextScreenUrlPath(boolean partOfSubflow, String flow, String nextScreen, String uuid) {
    return partOfSubflow ?
        getNextScreenUrlPathIfSubflow(flow, nextScreen, uuid)
        : getNextScreenUrlPathIfNotSubflow(flow, nextScreen);
  }

}
