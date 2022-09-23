package formflow.library.config;

import lombok.Data;
import java.lang.reflect.Constructor;
import javax.naming.ConfigurationException;


@Data
public class NextScreen {
  private String name;
  private String condition;
  private Condition conditionObject;

  public void setCondition(String condition) throws ConfigurationException {
    try {
      this.condition = condition;
      Class<?> clazz = Class.forName(condition);
      Constructor<?> ctor = clazz.getConstructor();
      conditionObject = (Condition) ctor.newInstance();
    } catch (Exception e) {
      throw new ConfigurationException(
              String.format("Unable to setup condition %s for screen %s: %s",
                      condition,
                      name,
                      e.getMessage()
              )
      );
    }
  }
}