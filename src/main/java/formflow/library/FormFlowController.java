package formflow.library;

import formflow.library.config.FlowConfiguration;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class FormFlowController {

  protected final SubmissionRepositoryService submissionRepositoryService;
  protected final FlowConfigurationManager flowConfigurationManager;

  FormFlowController(SubmissionRepositoryService submissionRepositoryService, FlowConfigurationManager flowConfigurationManager) {
    this.submissionRepositoryService = submissionRepositoryService;
    this.flowConfigurationManager = flowConfigurationManager;
  }

  protected void saveToRepository(Submission submission) {
    submissionRepositoryService.removeFlowCSRF(submission);
    submissionRepositoryService.save(submission);
  }

  protected void saveToRepository(Submission submission, String subflowName) {
    submissionRepositoryService.removeFlowCSRF(submission);
    submissionRepositoryService.removeSubflowCSRF(submission, subflowName);
    submissionRepositoryService.save(submission);
  }

  protected void handleErrors(HttpSession httpSession, Map<String, List<String>> errorMessages,
      FormSubmission formSubmission) {
    if (!errorMessages.isEmpty()) {
      httpSession.setAttribute("errorMessages", errorMessages);
      httpSession.setAttribute("formDataSubmission", formSubmission.getFormData());
    } else {
      httpSession.removeAttribute("errorMessages");
      httpSession.removeAttribute("formDataSubmission");
    }
  }

  protected FlowConfiguration getFlowConfigOr404(String flow) {
    try {
      return flowConfigurationManager.get(flow);
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "There was a problem with the request: Could not find flow with name %s in your application's flow configuration.".formatted(
              flow));
    }
  }

  protected ScreenNavigationConfiguration getScreenOr404(String flow, String screen) {
    try {
      return getFlowConfigOr404(flow).getScreen(screen);
    } catch (NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "There was a problem with the request: Could not find screen with name %s in flow %s.".formatted(
              screen, flow));
    }
  }
}
