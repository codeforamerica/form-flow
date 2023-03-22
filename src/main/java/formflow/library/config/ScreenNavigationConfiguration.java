package formflow.library.config;

import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * Screen navigation configuration class used to store navigation information about a specific screen.
 */
@Data
public class ScreenNavigationConfiguration {

  private List<NextScreen> nextScreens = Collections.emptyList();
  private String subflow;

  private String onPostAction;
  private String crossFieldValidationAction;
  private String beforeSaveAction;
  private String beforeDisplayAction;
}
