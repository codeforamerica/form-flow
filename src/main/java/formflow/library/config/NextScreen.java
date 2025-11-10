package formflow.library.config;


import lombok.Data;


/**
 * NextScreen represents what the next screen in a flow is and any conditions that need to be tested before one can go to that
 * screen.
 */
@Data
public class NextScreen {

  /**
   * Default constructor.
   */
  public NextScreen() {
  }

  private String name;
  private String condition;
}