package formflow.library.config;

import com.google.common.reflect.ClassPath;
import formflow.library.config.submission.Action;
import formflow.library.config.submission.Condition;
import formflow.library.data.Submission;
import lombok.Data;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TemplateManager {

  private HashMap<String, Condition> conditions = new HashMap<>();
  private HashMap<String, Action> actions = new HashMap<>();

  public TemplateManager(String conditionsClassPath, String actionsClassPath) {

    log.info("In template manager and loading: \n\tConditions: {}\n\tActions: {}%n",
        conditionsClassPath, actionsClassPath);

    if (conditionsClassPath != null && !conditionsClassPath.isEmpty()) {
      try {
        // get all the conditions located in conditions, load them
        List<ClassPath.ClassInfo> classes =
            ClassPath.from(ClassLoader.getSystemClassLoader())
                .getAllClasses()
                .stream()
                .filter(clazz -> clazz.getPackageName().toLowerCase().contains(conditionsClassPath.toLowerCase()))
                .toList();

        classes.forEach(clazz -> {
          try {
            log.info("Loading Condition: " + clazz.getSimpleName());
            Constructor<?> ctor = Class.forName(clazz.getName().replace("BOOT-INF.classes.", "")).getConstructor();
            Condition condition = (Condition) ctor.newInstance();
            this.conditions.put(clazz.getSimpleName(), condition);
          } catch (Exception e) {
            log.info("Encountered {} exception when creating {} Condition: {}%n",
                e.getClass(), clazz.getName(), e.getMessage());
          }
        });

        if (classes.size() == 0) {
          System.out.print("No conditions classes \uD83D\uDE1E%n");
        }
      } catch (IOException e) {
        log.info("Caught IOException while loading Conditions: " + e.getMessage());
      }
    }

    if (actionsClassPath != null && !actionsClassPath.isEmpty()) {
      try {
        // get all the conditions located in conditions, load them
        List<ClassPath.ClassInfo> classes =
            ClassPath.from(ClassLoader.getSystemClassLoader())
                .getAllClasses()
                .stream()
                .filter(clazz -> clazz.getPackageName().toLowerCase().contains(actionsClassPath.toLowerCase()))
                .toList();

        classes.forEach(clazz -> {
          try {
            log.info("Loading Action: " + clazz.getSimpleName());
            Constructor<?> ctor = Class.forName(clazz.getName().replace("BOOT-INF.classes.", "")).getConstructor();
            Action action = (Action) ctor.newInstance();
            this.actions.put(clazz.getSimpleName(), action);
          } catch (Exception e) {
            log.info("Encountered {} exception when creating {} Action: {}%n",
                e.getClass(), clazz.getName(), e.getMessage());
          }
        });

        if (classes.size() == 0) {
          System.out.print("No action classes \uD83D\uDE1E%n");
        }
      } catch (IOException e) {
        log.info("Caught IOException while loading Actions: " + e.getMessage());
      }
    }
  }

  public Boolean runCondition(String conditionName, Submission submission) {
    log.info("Running condition " + conditionName);
    Condition condition = conditions.get(conditionName);
    if (condition == null) {
      log.info("Condition not found: " + conditionName);
      // TODO throw error?
      return null;
    }
    return condition.run(submission);
  }

  public Boolean runCondition(String conditionName, Submission submission, String uuid) {
    log.info("Running condition " + conditionName + " data: " + uuid);
    Condition condition = conditions.get(conditionName);
    if (condition == null) {
      log.info("Condition not found: " + conditionName);
      // TODO throw error?
      return null;
    }
    return condition.run(submission, uuid);
  }
}
