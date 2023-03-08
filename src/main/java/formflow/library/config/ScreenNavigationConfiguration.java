package formflow.library.config;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.ConfigurationException;
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
  //TODO: Implement callback
  private String callback;

  private Action onPostActionObject;
  private Action crossFieldValidationActionObject;
  private Action beforeSaveActionObject;
  private Action beforeDisplayActionObject;

  public void setOnPostAction(String actionName) throws ConfigurationException {
    log.info("onPostAction is set to: " + actionName);
    onPostActionObject = loadAction(actionName);
  }

  public void setCrossFieldValidationAction(String actionName) throws ConfigurationException {
    log.info("crossFieldValidationActionName is set to: " + actionName);
    crossFieldValidationActionObject = loadAction(actionName);
  }

  public void setBeforeSaveAction(String actionName) throws ConfigurationException {
    log.info("beforeSave is set to: " + actionName);
    beforeSaveActionObject = loadAction(actionName);
  }

  public void setBeforeDisplayAction(String actionName) throws ConfigurationException {
    log.info("beforeDisplayAction is set to: " + actionName);
    beforeDisplayActionObject = loadAction(actionName);
  }

  private Action loadAction(String actionName) throws ConfigurationException {
    try {
      log.info("Loading action: " + actionName);
      Class<?> clazz = Class.forName(actionName);
      Constructor<?> ctor = clazz.getConstructor();
      return (Action) ctor.newInstance();
    } catch (Exception e) {
      throw new ConfigurationException(String.format("Unable to setup action %s: %s", actionName, e.getMessage()));
    }
  }

  public void handleBeforeSaveAction(Submission submission) {
    if (beforeSaveActionObject != null) {
      beforeSaveActionObject.run(submission);
    }
  }

  public void handleBeforeSaveAction(Submission submission, String uuid) {
    if (beforeSaveActionObject != null) {
      beforeSaveActionObject.run(submission, uuid);
    }
  }

  // consider: should we enforce that the action doesn't add or subtract from the submission
  // doing so could mess up down stream usage of the data
  public void handleBeforeDisplayAction(Submission submission, String uuid) {
    if (beforeDisplayActionObject != null) {
      if (uuid == null || uuid.isEmpty()) {
        beforeDisplayActionObject.run(submission);
      } else {
        beforeDisplayActionObject.run(submission, uuid);
      }
    }
  }

  // we can limit this to not adding fields here.
  public void handleOnPostAction(FormSubmission formSubmission) {
    if (onPostActionObject != null) {
      onPostActionObject.run(formSubmission);
    }
  }

  public Map<String, List<String>> handleCrossFieldValidationAction(FormSubmission formSubmission) {
    Map<String, List<String>> messageMap = new HashMap<>();
    if (crossFieldValidationActionObject != null) {
      messageMap = crossFieldValidationActionObject.runValidation(formSubmission);
    }
    return messageMap;
  }

  public void handleBeforeDisplayAction(FormSubmission formSubmission) {
    if (beforeDisplayActionObject != null) {
      beforeDisplayActionObject.run(formSubmission);
    }
  }
}
