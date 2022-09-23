package formflow.library.config;

import formflow.library.data.Submission;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

@Data
@Component
public class ConditionHandler {
  Class<ConditionDefinitions> conditions = ConditionDefinitions.class;
  Submission submission;

  public Boolean handleCondition(String conditionName)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Boolean result = false;
    try {
      result = (Boolean) conditions.getMethod(conditionName, Submission.class).invoke(conditions, submission);
    } catch (NoSuchMethodException e) {
      System.out.println("No such method could be found in the ConditionDefinitions class.");
    } catch (InvocationTargetException | IllegalAccessException e) {
      e.printStackTrace();
    }
    return result;
  }
}
