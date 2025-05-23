package formflow.library.config;

import lombok.Data;

/**
 * Class which contains configuration for a sub flow.
 */
@Data
public class SubflowConfiguration {

  SubflowRelationship relationship;
  String entryScreen;
  String iterationStartScreen;
  String reviewScreen;
  String deleteConfirmationScreen;
}
