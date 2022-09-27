package formflow.library.config;

import lombok.Data;
import java.lang.reflect.Constructor;
import javax.naming.ConfigurationException;


/**
 * NextScreen represents what the next screen in a flow is and any conditions that need to be
 * tested before one can go to that screen.
 */
@Data
public class NextScreen {
  private String name;
  private String condition;
  private Condition conditionObject;

  /**
   * Sets the condition name that should be associated with this (next) screen.
   *
   * This will use the name to find the proper condition class and create the condition object to be
   * used to test this condition.
   *
   * @param condition string name of the condition
   * @throws ConfigurationException
   */
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