package formflow.library.config;

import lombok.Data;

@Data
public class NextScreen {
  private String name;
  private Condition condition;
}
