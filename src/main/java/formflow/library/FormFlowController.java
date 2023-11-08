package formflow.library;

import formflow.library.config.FlowConfiguration;
import formflow.library.config.FormFlowConfigurationProperties;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFileRepositoryService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public abstract class FormFlowController {

  protected final SubmissionRepositoryService submissionRepositoryService;

  protected final UserFileRepositoryService userFileRepositoryService;

  protected final List<FlowConfiguration> flowConfigurations;
  
  protected final FormFlowConfigurationProperties formFlowConfigurationProperties;

  public static final String SUBMISSION_MAP_NAME = "submissionMap";

  FormFlowController(SubmissionRepositoryService submissionRepositoryService, UserFileRepositoryService userFileRepositoryService,
      List<FlowConfiguration> flowConfigurations, FormFlowConfigurationProperties formFlowConfigurationProperties) {
    this.submissionRepositoryService = submissionRepositoryService;
    this.userFileRepositoryService = userFileRepositoryService;
    this.flowConfigurations = flowConfigurations;
    this.formFlowConfigurationProperties = formFlowConfigurationProperties;
  }

  protected Submission saveToRepository(Submission submission) {
    return saveToRepository(submission, null);
  }

  protected Submission saveToRepository(Submission submission, String subflowName) {
    submissionRepositoryService.removeFlowCSRF(submission);
    if (subflowName != null && !subflowName.isBlank()) {
      submissionRepositoryService.removeSubflowCSRF(submission, subflowName);
    }
    return submissionRepositoryService.save(submission);
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

  protected static void throwNotFoundError(String flow, String screen, String message) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        String.format("There was a problem with the request (flow: %s, screen: %s): %s",
            flow, screen, message));

  }

  /**
   * If the submission information exists in the HttpSession, find it in the db. If a submission is not found, create a new one.
   *
   * @param httpSession The HttpSession to look in for information
   * @param flow        The name of the flow to retrieve data about
   * @return Submission A Submission object from the database or a new one if one was not found
   */
  public Submission findOrCreateSubmission(HttpSession httpSession, String flow) {
    Submission submission = null;
    try {
      submission = getSubmissionFromSession(httpSession, flow);
    } catch (ResponseStatusException ignored) {
      // it's okay if it doesn't exist already
    }

    if (submission == null) {
      log.info("Submission not found in session for flow '{}', creating one.", flow);
      submission = new Submission();
    }
    return submission;
  }

  /**
   * Returns the UUID of the Submission associated with the given flow.
   *
   * @param session The HttpSession the user is in
   * @param flow    The flow to look up the submission ID for
   * @return The submission id if it exists for the given flow, else null
   */
  public static UUID getSubmissionIdForFlow(HttpSession session, String flow) {
    if (session == null) {
      throwNotFoundError(flow, null, String.format("Session is null, unable to retrieve submission id for flow '%s'.", flow));
    }

    Map<String, UUID> submissionMap = (Map) session.getAttribute(SUBMISSION_MAP_NAME);
    if (submissionMap == null) {
      throwNotFoundError(flow, null, String.format("There was no submission map present in the session for flow '%s'.", flow));
    }

    return submissionMap.get(flow);
  }

  /**
   * This method will return a Submission that was referenced the HttpSession for a particular flow, if one exists. If the session
   * or now Submission exists, a null will be returned.
   *
   * @param session the HttpSession data will be looked for in
   * @param flow    the current flow to retrieve the Submission for
   * @return Submission for the flow, if one exists, else null
   */
  protected Submission getSubmissionFromSession(HttpSession session, String flow) {
    if (session == null) {
      throwNotFoundError(flow, null, String.format("Session is null, unable to retrieve submission for flow '%s'.", flow));
    }

    Map<String, UUID> submissionMap = (Map) session.getAttribute(SUBMISSION_MAP_NAME);
    if (submissionMap == null) {
      throwNotFoundError(flow, null, String.format("There was no submission map present in the session for flow '%s'.", flow));
    }

    UUID id = submissionMap.get(flow);
    if (id != null) {
      Optional<Submission> maybeSubmission = submissionRepositoryService.findById(id);
      if (maybeSubmission.isPresent()) {
        return maybeSubmission.get();
      }
      throwNotFoundError(flow, null, String.format("No submission was found in the database with id '%s'.", id));
    }

    throwNotFoundError(flow, null, String.format("No ID was present in the session map for flow '%s'", flow));
    return null;
  }

  /*
   * A method that will store the Submission ID, based on flow, in the HttpSession provided.
   *
   * @param session The HttpSession to store information in
   * @param submission The Submission whose information will be stored
   * @param flow A string containing the name of the flow to store the Submission data for
   */
  protected void setSubmissionInSession(HttpSession session, Submission submission, String flow) {
    if (session == null) {
      log.error(
          "Unable to put the submission ID ('{}') into the session for the flow '{}'. Session is null.",
          submission != null ? submission.getId() : null,
          flow);
      return;
    }

    Map<String, UUID> submissionMap = (Map) session.getAttribute(SUBMISSION_MAP_NAME);
    UUID id = submission != null ? submission.getId() : null;

    if (submissionMap == null) {
      submissionMap = new HashMap<>();
    }

    submissionMap.put(flow, id);
    session.setAttribute(SUBMISSION_MAP_NAME, submissionMap);
  }

  /**
   * Checks if the Submission for the flow with flowName should be locked, preventing further updates after it has been submitted.
   * @param flowName the name of the flow to check.
   * @param submission the Submission to check.
   * @return true if the Submission is configured to be locked for a given flow and the Submission's submittedAt value is not null,
   * false otherwise.
   */
  public boolean shouldRedirectDueToLockedSubmission(String flowName, Submission submission) {
    return this.formFlowConfigurationProperties.isSubmissionLockedForFlow(flowName);
  }
}
