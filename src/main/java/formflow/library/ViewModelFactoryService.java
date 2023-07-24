package formflow.library;

import formflow.library.config.ConditionManager;
import formflow.library.config.FlowConfiguration;
import formflow.library.config.SubflowConfiguration;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import formflow.library.file.FileValidationService;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.stereotype.Service;

@Service
public class ViewModelFactoryService {

  private final FlowConfigurationManager flowConfigurationManager;
  private final ConditionManager conditionManager;
  private final FileValidationService fileValidationService;
  private final Map<String, Object> model = new HashMap<>();

  public ViewModelFactoryService(List<FlowConfiguration> flowConfigurations, ConditionManager conditionManager,
      FileValidationService fileValidationService) {
    this.flowConfigurationManager = new FlowConfigurationManager(flowConfigurations);
    this.conditionManager = conditionManager;
    this.fileValidationService = fileValidationService;
  }

  public Map<String, Object> build(String flow, String screen, HttpSession httpSession, Submission submission, String uuid) {
    FlowConfiguration flowConfig = flowConfigurationManager.get(flow);
    String subflowName = flowConfig.getFlow().get(screen).getSubflow();

    model.put("flow", flow);
    model.put("screen", screen);
    model.put("conditionManager", conditionManager);
    model.put("acceptedFileTypes", fileValidationService.acceptedFileTypes());

    if (subflowName != null) {
      model.put("subflow", subflowName);
    }

    // Put subflow on model if on subflow delete confirmation screen
    HashMap<String, SubflowConfiguration> subflows = flowConfig.getSubflows();
    if (subflows != null) {
      List<String> subflowFromDeleteConfirmationConfig = subflows.entrySet().stream()
          .filter(entry ->
              entry.getValue()
                  .getDeleteConfirmationScreen()
                  .equals(screen))
          .map(Entry::getKey)
          .toList();

      if (!subflowFromDeleteConfirmationConfig.isEmpty()) {
        model.put("subflow", subflowFromDeleteConfirmationConfig.get(0));
      }

      // Add the iteration start page to the model if we are on the review page for a subflow so we have it for the edit button
      subflows.forEach((key, value) -> {
        if (value.getReviewScreen().equals(screen)) {
          model.put("iterationStartScreen", value.getIterationStartScreen());
        }
      });
    }

    // Merge form data that was submitted, with already existing inputData
    // This helps in the case of errors, so all the current data is on the page
    if (httpSession.getAttribute("formDataSubmission") != null) {
      FormSubmission formSubmission = new FormSubmission((Map<String, Object>) httpSession.getAttribute("formDataSubmission"));
      if (subflowName != null && uuid != null && !uuid.isBlank()) {
        submission.mergeFormDataWithSubflowIterationData(subflowName, submission.getSubflowEntryByUuid(subflowName, uuid),
            formSubmission.getFormData());
      } else {
        submission.mergeFormDataWithSubmissionData(formSubmission);
      }
    }

    model.put("submission", submission);
    model.put("inputData", submission.getInputData());
    model.put("errorMessages", httpSession.getAttribute("errorMessages"));
    model.put("fieldData", submission.getInputData());
    if (subflowName != null) {
      if (uuid != null && !uuid.isBlank()) {
        model.put("fieldData", submission.getSubflowEntryByUuid(subflowName, uuid));
      } else if (httpSession.getAttribute("formSubmissionData") != null) {
        // this is a new subflow iteration, we have submission data, so share that
        model.put("fieldData", httpSession.getAttribute("formDataSubmission"));
      }
      // We keep "currentSubflowItem" for backwards compatability at this point
      model.put("currentSubflowItem", model.get("fieldData"));
    }
    return model;
  }
}
