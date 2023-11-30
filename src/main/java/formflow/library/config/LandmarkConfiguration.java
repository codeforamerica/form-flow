package formflow.library.config;

import java.util.ArrayList;
import lombok.Data;

/**
 * Class which contains configuration for the landmark screen(s) in a flow.
 */
@Data
public class LandmarkConfiguration {
  String firstScreen;
  ArrayList<String> afterSubmitPages;
}
