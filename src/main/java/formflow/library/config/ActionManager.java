package formflow.library.config;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ActionManager {

  private HashMap<String, Action> actions = new HashMap<>();

  public ActionManager(List<Action> actionsList) {
    actionsList.forEach(action -> this.actions.put(action.getClass().getSimpleName(), action));
  }

  public Action getAction(String name) {
    return actions.get(name);
  }

  public void handleOnPostAction(ScreenNavigationConfiguration currentScreen, FormSubmission formSubmission) {
    String actionName = currentScreen.getOnPostAction();
    if (actionName != null) {
      runAction(actionName, formSubmission);
    }
  }

  public void handleOnPostAction(ScreenNavigationConfiguration currentScreen, FormSubmission formSubmission, String uuid) {
    String actionName = currentScreen.getOnPostAction();
    if (actionName != null) {
      runAction(actionName, formSubmission, uuid);
    }
  }

  public void handleBeforeSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission) {
    String actionName = currentScreen.getBeforeSaveAction();
    if (actionName != null) {
      runAction(actionName, submission);
    }
  }

  public void handleBeforeSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid) {
    String actionName = currentScreen.getBeforeSaveAction();
    if (actionName != null) {
      runAction(actionName, submission, uuid);
    }
  }

  public void handleBeforeDisplayAction(ScreenNavigationConfiguration currentScreen, Submission submission) {
    String actionName = currentScreen.getBeforeDisplayAction();
    if (actionName != null) {
      runAction(actionName, submission);
    }
  }

  public void handleBeforeDisplayAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid) {
    String actionName = currentScreen.getBeforeDisplayAction();
    if (actionName != null) {
      runAction(actionName, submission, uuid);
    }
  }

  public Map<String, List<String>> handleCrossFieldValidationAction(ScreenNavigationConfiguration currentScreen,
      FormSubmission formSubmission) {
    Map<String, List<String>> messageMap = new HashMap<>();
    String actionName = currentScreen.getCrossFieldValidationAction();
    if (actionName != null) {
      messageMap.putAll(runValidationAction(actionName, formSubmission));
    }
    return messageMap;
  }

  private void runAction(String name, Submission submission) {
    runAction(name, () -> getAction(name).run(submission));
  }

  private void runAction(String name, Submission submission, String uuid) {
    runAction(name, () -> getAction(name).run(submission, uuid));
  }

  private void runAction(String name, FormSubmission formSubmission) {
    runAction(name, () -> getAction(name).run(formSubmission));
  }

  private void runAction(String name, FormSubmission formSubmission, String uuid) {
    runAction(name, () -> getAction(name).run(formSubmission, uuid));
  }

  private void runAction(String name, Runnable action) {
    if (action == null) {
      log.error(String.format("Unable to find Action '%s' to run", name));
      return;
    }

    try {
      action.run();
    } catch (Exception e) {
      log.error(String.format("Unable to run Action '%s'", name));
    }
  }

  private Map<String, List<String>> runValidationAction(String name, FormSubmission formSubmission) {
    Map<String, List<String>> errorMessages = new HashMap<>();
    try {
      Map<String, List<String>> messages = getAction(name).runValidation(formSubmission);
      if (messages != null) {
        errorMessages.putAll(messages);
      }
    } catch (Exception e) {
      log.error(String.format("Unable to find Action '%s' to run", name));
    }
    return errorMessages;
  }
}
