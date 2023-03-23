package formflow.library.config;

import formflow.library.config.submission.Condition;
import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConditionManager {

  private final HashMap<String, Condition> conditions = new HashMap<>();

  public ConditionManager(List<Condition> conditionsList) {
    conditionsList.forEach(condition -> this.conditions.put(condition.getClass().getSimpleName(), condition));
  }

  public Condition getCondition(String name) {
    return conditions.get(name);
  }

  public Boolean conditionExists(String name) {
    return conditions.containsKey(name);
  }

  public Boolean runCondition(String conditionName, Submission submission) {
    Condition condition = getCondition(conditionName);
    if (condition == null) {
      log.warn("Condition not found: " + conditionName);
      // TODO: an exception would be clearer, should we do that?
      return false;
    }
    return condition.run(submission);
  }

  public Boolean runCondition(String conditionName, Submission submission, String uuid) {
    Condition condition = getCondition(conditionName);
    if (condition == null) {
      log.warn("Condition not found: " + conditionName);
      // TODO: an exception would be clearer, should we do that?
      return false;
    }
    return condition.run(submission, uuid);
  }
}
