package formflow.library.config;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Manages methods for handling actions.  Action Manager methods allow for application logic to be run at specific points during
 * GET and POST processing.
 */
@Slf4j
@Component
public class ActionManager {

  private final HashMap<String, Action> actions = new HashMap<>();

  /**
   * Class Constructor generating a hashmap of action names and their corresponding action objects.
   *
   * @param actionsList - a list of action objects.
   */
  public ActionManager(List<Action> actionsList) {
    actionsList.forEach(action -> this.actions.put(action.getClass().getSimpleName(), action));
  }

  /**
   * Get an action by name, usually to run the action.
   *
   * @param name The name of the action
   * @return The action from the action manager
   */
  public Action getAction(String name) {
    return actions.get(name);
  }

  /**
   * <code>handleOnPostAction()</code> invokes a method in the ScreenController. Runs before validation. The
   * handleOnPostAction method is called on all screens except for screens in a subflow. Runs an action if a screen has one
   * defined.
   *
   * @param currentScreen  The screen that we are currently saving data from.
   * @param formSubmission The current form submission
   * @param submission     The submission object after changes to the current screen have been saved to the repository
   */
  public void handleOnPostAction(ScreenNavigationConfiguration currentScreen, FormSubmission formSubmission,
      Submission submission) {
    String actionName = currentScreen.getOnPostAction();
    if (actionName != null) {
      runAction(actionName, formSubmission, submission);
    }
  }

  /**
   * <code>handleOnPostAction()</code> invokes a method in the ScreenController. Runs before validation. The
   * handleOnPostAction method is called on only screens in a subflow. Runs an action if a screen has one defined.
   *
   * @param currentScreen  The screen that we are currently saving data from.
   * @param formSubmission The current form submission
   * @param submission     The submission object after changes to the current screen have been saved to the repository
   * @param uuid           The uuid of the current subflow.
   */
  public void handleOnPostAction(ScreenNavigationConfiguration currentScreen, FormSubmission formSubmission,
          Submission submission, String uuid) {
    String actionName = currentScreen.getOnPostAction();
    if (actionName != null) {
      runAction(actionName, formSubmission, submission, uuid);
    }
  }

  /**
   * <code>handleOnPostAction()</code> invokes a method in the ScreenController. Runs before validation. The
   * handleOnPostAction method is called on only screens in a subflow. Runs an action if a screen has one defined.
   *
   * @param currentScreen  The screen that we are currently saving data from.
   * @param formSubmission The current form submission
   * @param submission     The submission object after changes to the current screen have been saved to the repository
   * @param uuid           The uuid of the current subflow.
   * @param repeatsForUuid The uuid of the repeatsFor subflow under the current subflow
   */
  public void handleOnPostAction(ScreenNavigationConfiguration currentScreen, FormSubmission formSubmission,
          Submission submission, String uuid, String repeatsForUuid) {
    String actionName = currentScreen.getOnPostAction();
    if (actionName != null) {
      runAction(actionName, formSubmission, submission, uuid, repeatsForUuid);
    }
  }

  /**
   * <code>handleBeforeSaveAction()</code> invokes a method in the ScreenController. Runs after validation and before
   * saving. The handleBeforeSaveAction method is called on all screens except for screens in a subflow. Runs an action if a
   * screen has one defined.
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   */
  public void handleBeforeSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission) {
    String actionName = currentScreen.getBeforeSaveAction();
    if (actionName != null) {
      runAction(actionName, submission);
    }
  }

  /**
   * <code>handleBeforeSaveAction()</code> invokes a method in the ScreenController. Runs after validation and before
   * saving. The handleBeforeSaveAction method is called on only screens in a subflow. Runs an action if a screen has one
   * defined.
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   * @param uuid          The uuid of the current subflow.
   */
  public void handleBeforeSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid) {
    String actionName = currentScreen.getBeforeSaveAction();
    if (actionName != null) {
      runAction(actionName, submission, uuid);
    }
  }

  /**
   * <code>handleBeforeSaveAction()</code> invokes a method in the ScreenController. Runs after validation and before
   * saving. The handleBeforeSaveAction method is called on only screens in a subflow. Runs an action if a screen has one
   * defined.
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   * @param uuid          The uuid of the current subflow.
   * @param repeatsForUuid The uuid of the repeatsFor subflow under the current subflow
   */
  public void handleBeforeSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid, String repeatsForUuid) {
    String actionName = currentScreen.getBeforeSaveAction();
    if (actionName != null) {
      runAction(actionName, submission, uuid, repeatsForUuid);
    }
  }

  /**
   * <p>
   * <code>handleAfterSaveAction()</code> invokes a method after a submission has been saved in the ScreenController.  The
   * handleAfterSaveAction method is called on all screens except for screens in a subflow.
   * </p>
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   */
  public void handleAfterSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission) {
    String actionName = currentScreen.getAfterSaveAction();
    if (actionName != null) {
      runAction(actionName, submission);
    }
  }

  /**
   * <p>
   * This <code>handleAfterSaveAction()</code> invokes a method after a <b>subflow</b> submission has been saved.
   * </p>
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   * @param uuid          The uuid of the current subflow.
   */
  public void handleAfterSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid) {
    String actionName = currentScreen.getAfterSaveAction();
    if (actionName != null) {
      runAction(actionName, submission, uuid);
    }
  }

  /**
   * <p>
   * This <code>handleAfterSaveAction()</code> invokes a method after a <b>subflow</b> submission has been saved.
   * </p>
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   * @param uuid          The uuid of the current subflow.
   * @param repeatsForUuid The uuid of the repeatsFor subflow under the current subflow
   */
  public void handleAfterSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid, String repeatsForUuid) {
    String actionName = currentScreen.getAfterSaveAction();
    if (actionName != null) {
      runAction(actionName, submission, uuid, repeatsForUuid);
    }
  }

  /**
   * <code>handleBeforeDisplayAction()</code> invokes a method in the ScreenController. Runs after getting data from the database
   * and before the view template is displayed. The handleBeforeDisplayAction method is called on all screens except for screens
   * in a subflow. Runs an action if a screen has one defined.
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   */
  public void handleBeforeDisplayAction(ScreenNavigationConfiguration currentScreen, Submission submission) {
    String actionName = currentScreen.getBeforeDisplayAction();
    if (actionName != null) {
      runAction(actionName, submission);
    }
  }

  /**
   * <code>handleBeforeDisplayAction()</code> invokes a method in the ScreenController. Runs after getting data from the database
   * and before the view template is displayed. The handleBeforeDisplayAction method is called on only screens in a subflow. Runs
   * an action if a screen has one defined.
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   * @param uuid          The uuid of the current subflow.
   */
  public void handleBeforeDisplayAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid) {
    String actionName = currentScreen.getBeforeDisplayAction();
    if (actionName != null) {
      runAction(actionName, submission, uuid);
    }
  }


  /**
   * <code>handleBeforeDisplayAction()</code> invokes a method in the ScreenController. Runs after getting data from the database
   * and before the view template is displayed. The handleBeforeDisplayAction method is called on only screens in a subflow. Runs
   * an action if a screen has one defined.
   *
   * @param currentScreen The screen that we are currently saving data from.
   * @param submission    The submission object after changes to the current screen have been saved to the repository
   * @param uuid          The uuid of the current subflow.
   * @param repeatsForUuid The uuid of the repeatsFor subflow under the current subflow
   */
  public void handleBeforeDisplayAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid, String repeatsForUuid) {
    String actionName = currentScreen.getBeforeDisplayAction();
    if (actionName != null) {
      runAction(actionName, submission, uuid, repeatsForUuid);
    }
  }
  /**
   * <code>handleCrossFieldValidationAction()</code> invokes a method in the ScreenController. Runs after field validation and
   * before saving to the database. The handleCrossFieldValidationAction method is called on all screens. Runs an action if a
   * screen has one defined.
   *
   * @param currentScreen  The screen that we are currently saving data from.
   * @param formSubmission The current form submission
   * @param submission     The submission object before changes to the current screen have been saved to the repository
   * @return A map of validation results
   */
  public Map<String, List<String>> handleCrossFieldValidationAction(ScreenNavigationConfiguration currentScreen,
      FormSubmission formSubmission, Submission submission) {
    Map<String, List<String>> messageMap = new HashMap<>();
    String actionName = currentScreen.getCrossFieldValidationAction();
    if (actionName != null) {
      messageMap.putAll(runValidationAction(actionName, formSubmission, submission));
    }
    return messageMap;
  }

  private void runAction(String name, Submission submission) {
    runAction(name, () -> getAction(name).run(submission));
  }

  private void runAction(String name, Submission submission, String uuid, String repeatsForUuid) {
    runAction(name, () -> getAction(name).run(submission, uuid, repeatsForUuid));
  }

  private void runAction(String name, Submission submission, String uuid) {
    runAction(name, () -> getAction(name).run(submission, uuid));
  }

  private void runAction(String name, FormSubmission formSubmission, Submission submission) {
    runAction(name, () -> getAction(name).run(formSubmission, submission));
  }

  private void runAction(String name, FormSubmission formSubmission, Submission submission, String uuid) {
    runAction(name, () -> getAction(name).run(formSubmission, submission, uuid));
  }

  private void runAction(String name, FormSubmission formSubmission, Submission submission, String uuid, String repeatsForUuid) {
    runAction(name, () -> getAction(name).run(formSubmission, submission, uuid, repeatsForUuid));
  }

  private void runAction(String name, Runnable action) {
    if (action == null) {
      log.error(String.format("Unable to find Action '%s' to run", name));
      return;
    }

    try {
      action.run();
    } catch (Exception e) {
      log.error(String.format("Unable to run Action '%s', %s", name, e));
    }
  }

  private Map<String, List<String>> runValidationAction(String name, FormSubmission formSubmission, Submission submission) {
    Map<String, List<String>> errorMessages = new HashMap<>();
    try {
      Map<String, List<String>> messages = getAction(name).runValidation(formSubmission, submission);
      if (messages != null) {
        errorMessages.putAll(messages);
      }
    } catch (Exception e) {
      log.error(String.format("Unable to find Action '%s' to run", name));
    }
    return errorMessages;
  }
}
