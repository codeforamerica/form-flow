package formflow.library.config;

import lombok.Data;

@Data
public class SubflowConfiguration {
  String entryScreen;
  String iterationStartScreen;
  String reviewScreen;
  String deleteConfirmationScreen;
}
