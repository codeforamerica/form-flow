package formflow.library;

import formflow.library.config.FlowConfiguration;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.util.List;
import java.util.NoSuchElementException;

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
    try {
      return flowConfigurations.stream().filter(
          flowConfiguration -> flowConfiguration.getName().equals(flow)
      ).toList().get(0);

    } catch (ArrayIndexOutOfBoundsException e) {
      throw new NoSuchElementException("Could not find flow=" + flow + " in templates");
    }
  }
}
