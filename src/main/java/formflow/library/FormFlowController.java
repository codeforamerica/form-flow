package formflow.library;

import formflow.library.config.FlowConfiguration;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class FormFlowController {

  protected final SubmissionRepositoryService submissionRepositoryService;

  protected final List<FlowConfiguration> flowConfigurations;

  FormFlowController(SubmissionRepositoryService submissionRepositoryService, List<FlowConfiguration> flowConfigurations) {
    this.submissionRepositoryService = submissionRepositoryService;
    this.flowConfigurations = flowConfigurations;
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

  protected FlowConfiguration getFlowConfigurationByName(String flow) {
      List<FlowConfiguration> flowConfigurationList = flowConfigurations.stream().filter(
          flowConfiguration -> flowConfiguration.getName().equals(flow)).toList();
      
      if (flowConfigurationList.isEmpty()) {
        throwNotFoundError(flow, null, String.format("Could not find flow %s in your applications flow configuration file.", flow));
      }
      
      return flowConfigurationList.get(0);
  }

  protected Boolean doesFlowExist(String flow) {
    return flowConfigurations.stream().anyMatch(
        flowConfiguration -> flowConfiguration.getName().equals(flow)
    );
  }

  protected static void throwNotFoundError(String flow,String screen, String message) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("There was a problem with the request (flow: %s, screen: %s): %s",
        flow, screen, message));

  }
}
