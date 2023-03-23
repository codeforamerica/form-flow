package formflow.library.config;

import formflow.library.config.submission.Condition;
import lombok.Data;

import java.lang.reflect.Constructor;
import javax.naming.ConfigurationException;


/**
 * NextScreen represents what the next screen in a flow is and any conditions that need to be tested before one can go to that
 * screen.
 */
@Data
public class NextScreen {

  private String name;
  private String condition;
}