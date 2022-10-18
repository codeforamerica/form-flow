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
import java.util.stream.Collectors;

@Data
public class TemplateManager {

    private HashMap<String, Condition> conditions;
    private HashMap<String, Action> actions;

    public TemplateManager(String conditionsClassPath, String actionsClassPath) {
        this.conditions = new HashMap<>();
        this.actions = new HashMap<>();

        System.out.println(String.format("In template manager and loading: \n\tConditions: %s\n\tActions: %s)",
                conditionsClassPath, actionsClassPath));

        try {
            // get all the conditions located in conditions, load them
            List<ClassPath.ClassInfo> classes =
                    ClassPath.from(ClassLoader.getSystemClassLoader())
                            .getAllClasses()
                            .stream()
                            .filter(clazz -> clazz.getPackageName().equalsIgnoreCase(conditionsClassPath))
                            .collect(Collectors.toList());

            classes.forEach(clazz -> {
                try {
                    Constructor<?> ctor = Class.forName(clazz.getName()).getConstructor();
                    Condition condition = (Condition) ctor.newInstance();
                    System.out.println("Loading Condition: " + clazz.getSimpleName());
                    this.conditions.put(clazz.getSimpleName(), condition);
                } catch (Exception e) {
                    System.out.println(
                            String.format("Encountered %s exception when creating %s Condition: %s",
                                    e.getClass(), clazz.getName(), e.getMessage())
                    );
                }
            });
        } catch (IOException e) {
            System.out.println("Caught IOException while loading Conditions: " + e.getMessage());
        }

        try {
            // get all the actions located in the actions class path, load them
            List<ClassPath.ClassInfo> classes =
                    ClassPath.from(ClassLoader.getSystemClassLoader())
                            .getAllClasses()
                            .stream()
                            .filter(clazz -> clazz.getPackageName().equalsIgnoreCase(actionsClassPath))
                            .collect(Collectors.toList());

            classes.forEach(clazz -> {
                try {
                    Constructor<?> ctor = Class.forName(clazz.getName()).getConstructor();
                    Action action = (Action) ctor.newInstance();
                    System.out.println("Loading Action: " + clazz.getSimpleName());
                    this.actions.put(clazz.getSimpleName(), action);
                } catch (Exception e) {
                    System.out.println(
                            String.format("Encountered %s exception when creating %s Action: %s",
                                    e.getClass(), clazz.getName(), e.getMessage())
                    );
                }
            });
        } catch (IOException e) {
            System.out.println("Caught IOException while loading Action: " + e.getMessage());
        }
    }

    public Boolean runCondition(String conditionName, Submission submission) {
        System.out.println("Running condition " + conditionName);
        Condition condition = conditions.get(conditionName);
        if (condition == null) {
            System.out.println("Condition not found: " + conditionName);
            // TODO throw error?
            return null;
        }
        return condition.run(submission);
    }

    public Boolean runCondition(String conditionName, Submission submission, String uuid) {
        System.out.println("Running condition " + conditionName + " data: " + uuid);
         Condition condition = conditions.get(conditionName);
        if (condition == null) {
            System.out.println("Condition not found: " + conditionName);
            // TODO throw error?
            return null;
        }
        return condition.run(submission, uuid);
    }

}
