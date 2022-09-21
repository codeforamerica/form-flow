package formflow.library.config;

import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class ScreenNavigationConfiguration {
  private List<NextScreen> nextScreens = Collections.emptyList();
  private String subflow;
  //TODO: Implement callback
  private String callback;
  private String beforeSave;
}
