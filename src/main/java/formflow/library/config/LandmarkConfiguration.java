package formflow.library.config;

import lombok.Data;

/**
 * Class which contains configuration for the landmark screen(s) in a flow.
 */
@Data
public class LandmarkConfiguration {
  String firstScreen;
  String disabledFlowRedirect;
}
