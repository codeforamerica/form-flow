package formflow.library;

import formflow.library.config.FlowConfiguration;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.util.List;

public abstract class FormFlowController {

  protected final SubmissionRepositoryService submissionRepositoryService;

  FormFlowController(SubmissionRepositoryService submissionRepositoryService) {
    this.submissionRepositoryService = submissionRepositoryService;
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
}
