package formflow.library.data;


import jakarta.validation.constraints.NotBlank;

public class FlowInputs {

  /**
   * Default constructor.
   */
  public FlowInputs() {
  }

  @NotBlank
  private String _csrf;
}
