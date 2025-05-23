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
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public abstract class FormFlowController {

  /**
   * A parent controller class for form-flow controllers
   */

  protected final SubmissionRepositoryService submissionRepositoryService;

  protected final UserFileRepositoryService userFileRepositoryService;

  protected final List<FlowConfiguration> flowConfigurations;

  protected final FormFlowConfigurationProperties formFlowConfigurationProperties;

  protected final MessageSource messageSource;

  public static final String SUBMISSION_MAP_NAME = "submissionMap";

  FormFlowController(SubmissionRepositoryService submissionRepositoryService, UserFileRepositoryService userFileRepositoryService,
                     List<FlowConfiguration> flowConfigurations, FormFlowConfigurationProperties formFlowConfigurationProperties,
                     MessageSource messageSource) {
    this.submissionRepositoryService = submissionRepositoryService;
    this.userFileRepositoryService = userFileRepositoryService;
    this.flowConfigurations = flowConfigurations;
    this.formFlowConfigurationProperties = formFlowConfigurationProperties;
    this.messageSource = messageSource;
  }

  /**
   * Saves a {@link Submission}
   *
   * @param submission {@link Submission} saved through the submission repository service.
   * @return The saved {@link Submission} object
   */
  protected Submission saveToRepository(Submission submission) {
    return saveToRepository(submission, null);
  }

  /**
   * Saves a {@link Submission}
   *
   * @param submission  a {@link Submission} saved through the submission repository service
   * @param subflowName {@link String} of a subflow name or null
   * @return The saved {@link Submission} object
   */
  protected Submission saveToRepository(Submission submission, String subflowName) {
    submissionRepositoryService.removeFlowCSRF(submission);
    if (subflowName != null && !subflowName.isBlank()) {
      submissionRepositoryService.removeSubflowCSRF(submission, subflowName);
    }
    return submissionRepositoryService.save(submission);
  }

  /**
   * Gets the {@link FlowConfiguration} object for a given flow after validating that the flow exists.
   *
   * @param flow {@link String} of a flow name.
   * @return Returns a {@link FlowConfiguration} object.
   * @throws ResponseStatusException when FlowConfigurations are not found.
   */
  protected FlowConfiguration getValidatedFlowConfigurationByName(String flow) {
    List<FlowConfiguration> flowConfigurationList = flowConfigurations.stream().filter(
        flowConfiguration -> flowConfiguration.getName().equals(flow)).toList();

    if (flowConfigurationList.isEmpty()) {
      throwNotFoundError(flow, null, String.format("Could not find flow %s in your applications flow configuration file.", flow));
    }

    return flowConfigurationList.get(0);
  }

  /**
   * Checks if there are any flows matching the flow name passed in.
   *
   * @param flow {@link String} of a flow name.
   * @return {@link Boolean} {@code True} if the flow is found in the {@link FlowConfiguration}. <br>{@link Boolean} {@code False} if the flow is not found in the {@link FlowConfiguration}.
   */
  protected Boolean doesFlowExist(String flow) {
    return flowConfigurations.stream().anyMatch(
        flowConfiguration -> flowConfiguration.getName().equals(flow)
    );
  }

  /**
   * Throws a {@link ResponseStatusException} when called that includes the status, {@code HttpStatus.NOT_FOUND}, and an error message.
   *
   * @param flow    {@link String} of the flow name.
   * @param screen  Screen name of a flow
   * @param message Message about the request issue
   * @throws ResponseStatusException Throws a {@link ResponseStatusException} when called.
   */
  protected static void throwNotFoundError(String flow, String screen, String message) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        String.format("There was a problem with the request (flow: %s, screen: %s): %s",
            flow, screen, message));
  }

  /**
   * If the submission information exists in the {@link HttpSession}, find it in the db. If a submission is not found, create a new one.
   *
   * @param httpSession The {@link HttpSession}to look in for information
   * @param flow        The name of the flow to retrieve data about
   * @return The {@link Submission} object from the database or a new {@link Submission} object if one was not found
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
   * Returns the {@link UUID} of the {@link Submission} associated with the given flow.
   *
   * @param session The {@link HttpSession} the user is in
   * @param flow    The flow to look up the submission ID for
   * @return The submission id if it exists for the given flow, else null
   * @throws ResponseStatusException if {@code throwNotFoundError()} is called.
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
   * This method will return a Submission that was referenced the {@link HttpSession} for a particular flow, if one exists. If the session
   * or now Submission does not exist, a null will be returned.
   *
   * @param session the {@link HttpSession} data will be looked for in
   * @param flow    the current flow to retrieve the {@link Submission} for
   * @return {@link Submission} for the flow, if one exists, else null
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

  /**
   * A method that will store the Submission ID, based on flow, in the {@link HttpSession} provided.
   *
   * @param session    The {@link HttpSession} to store information in
   * @param submission The {@link Submission} whose information will be stored
   * @param flow       A {@link String} containing the name of the flow to store the Submission data for
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
   * Determines if the user should be redirected due to a locked submission.
   *
   * @param screen     The current screen name.
   * @param submission The submission object.
   * @param flowName   The name of the flow to check.
   * @return true if the user should be redirected, false otherwise.
   */
  public boolean shouldRedirectDueToLockedSubmission(String screen, Submission submission, String flowName) {
    FlowConfiguration flowConfig = getValidatedFlowConfigurationByName(flowName);
    boolean submissionIsLocked = this.formFlowConfigurationProperties.isSubmissionLockedForFlow(flowName);

    if (submissionIsLocked) {
      boolean isSubmissionSubmitted = submission.getSubmittedAt() != null;
      boolean isCurrentScreenAfterSubmit = flowConfig.getLandmarks().getAfterSubmitPages().contains(screen);
      return isSubmissionSubmitted && !isCurrentScreenAfterSubmit;
    }
    return false;
  }
}
