package formflow.library;

import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS;

import com.smartystreets.api.exceptions.SmartyException;
import formflow.library.address_validation.AddressValidationService;
import formflow.library.address_validation.ValidatedAddress;
import formflow.library.config.ActionManager;
import formflow.library.config.ConditionManager;
import formflow.library.config.FlowConfiguration;
import formflow.library.config.FormFlowConfigurationProperties;
import formflow.library.config.NextScreen;
import formflow.library.config.RepeatFor;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.config.SubflowConfiguration;
import formflow.library.config.SubflowManager;
import formflow.library.config.submission.ShortCodeConfig;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.file.FileValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

/**
 * A controller to render any screen in flows, including subflows.
 */
@Controller
@EnableAutoConfiguration
@Slf4j
@RequestMapping(ScreenController.FLOW)
public class ScreenController extends FormFlowController {

  public static final String FLOW = "/flow";
  public static final String FLOW_SCREEN_PATH = "{flow}/{screen}";
  private final ValidationService validationService;
  private final AddressValidationService addressValidationService;
  private final ConditionManager conditionManager;
  private final ActionManager actionManager;
  private final FileValidationService fileValidationService;
  private final SubmissionRepositoryService submissionRepositoryService;
  private final ShortCodeConfig shortCodeConfig;
  private final SubflowManager subflowManager;

  public ScreenController(
      List<FlowConfiguration> flowConfigurations,
      UserFileRepositoryService userFileRepositoryService,
      SubmissionRepositoryService submissionRepositoryService,
      ValidationService validationService,
      AddressValidationService addressValidationService,
      FormFlowConfigurationProperties formFlowConfigurationProperties,
      ConditionManager conditionManager,
      ActionManager actionManager,
      FileValidationService fileValidationService,
      MessageSource messageSource,
      ShortCodeConfig shortCodeConfig,
      SubflowManager subflowManager
  ) {
    super(submissionRepositoryService, userFileRepositoryService, flowConfigurations, formFlowConfigurationProperties,
        messageSource);
    this.validationService = validationService;
    this.addressValidationService = addressValidationService;
    this.conditionManager = conditionManager;
    this.actionManager = actionManager;
    this.fileValidationService = fileValidationService;
    this.submissionRepositoryService = submissionRepositoryService;
    this.shortCodeConfig = shortCodeConfig;
    this.subflowManager = subflowManager;

      log.info("Screen Controller Created!");
  }

  @AllArgsConstructor
  @Getter
  private class ScreenConfig {
    String flowName;
    ScreenNavigationConfiguration screenNavigationConfiguration;
  }

  /**
   * Chooses which screen template and model data to render.
   *
   * @param requestFlow   The current flow name, not null
   * @param requestScreen The current screen name in the flow, not null
   * @param requestUuid   The uuid of a subflow entry, can be null
   * @param httpSession   The current httpSession, not null
   * @return the screen template with model data
   */
  @GetMapping(FLOW_SCREEN_PATH)
  ModelAndView getScreen(
      @PathVariable(name = "flow") String requestFlow,
      @PathVariable(name = "screen") String requestScreen,
      @RequestParam(required = false) Map<String, String> query_params,
      @RequestParam(value = "uuid", required = false) String requestUuid,
      @RequestHeader(value = "Referer", required = false) String referer,
      RedirectAttributes redirectAttributes,
      HttpSession httpSession,
      HttpServletRequest request,
      Locale locale
  ) {
    log.info("GET getScreen (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), requestFlow, requestScreen);
    // getScreenConfig() will ensure that the screen and flow actually exist
    ScreenConfig screenConfig = getValidatedScreenConfiguration(requestFlow, requestScreen);
    ScreenNavigationConfiguration currentScreen = screenConfig.getScreenNavigationConfiguration();
    String flow = screenConfig.getFlowName();
    String screen = currentScreen.getName();

    Submission submission = findOrCreateSubmission(httpSession, flow);

    String uuid = null;
    if (requestUuid != null && !requestUuid.isBlank()) {
      uuid = getValidatedIterationUuid(submission, flow, currentScreen, requestUuid);
      if (uuid == null) {
        // catch to see if they are trying to go to a delete confirmation screen when the UUID is not present anymore.
        // If so, redirect.
        if (isDeleteConfirmationScreen(flow, screen)) {
          ModelAndView nothingToDeleteModelAndView = handleDeleteBackBehavior(flow, screen, requestUuid, submission);
          if (nothingToDeleteModelAndView != null) {
            return nothingToDeleteModelAndView;
          }
        } else {
          throwNotFoundError(submission.getFlow(), currentScreen.getName(),
              String.format("UUID ('%s') not found in iterations for subflow '%s' in flow '%s', when navigating to '%s'",
                  uuid, currentScreen.getSubflow(), submission.getFlow(), currentScreen.getName()));
        }
      }
    }

    if (shouldRedirectDueToLockedSubmission(screen, submission, flow)) {
      String lockedSubmissionRedirectUrl = getLockedSubmissionRedirectUrl(flow, redirectAttributes, locale);
      return new ModelAndView("redirect:" + lockedSubmissionRedirectUrl);
    }

    if (shouldRedirectToNextScreen(uuid, currentScreen, submission)) {
      String nextViewableScreen = getNextViewableScreen(flow, screen, uuid, submission);
      log.info("%s is not viewable, redirecting to %s".formatted(screen, nextViewableScreen));
      if (uuid == null) {
        return new ModelAndView(String.format("redirect:/flow/%s/%s", flow, nextViewableScreen));
      } else {
        return new ModelAndView(String.format("redirect:/flow/%s/%s/%s", flow, nextViewableScreen, uuid));
      }
    }

    if ((submission.getUrlParams() != null) && (!submission.getUrlParams().isEmpty())) {
      submission.mergeUrlParamsWithData(query_params);
    } else {
      submission.setUrlParams(query_params);
    }

    submission.setFlow(flow);
    submission = saveToRepository(submission);
    setSubmissionInSession(httpSession, submission, flow);

    if (uuid != null) {
      actionManager.handleBeforeDisplayAction(currentScreen, submission, uuid);
    } else {
      actionManager.handleBeforeDisplayAction(currentScreen, submission);
    }

    if (currentScreen.getSubflow() != null &&
            subflowManager.subflowHasRelationship(flow, currentScreen.getSubflow())) {
      subflowManager.addSubflowRelationshipData(currentScreen, flow, submission);
      saveToRepository(submission);
    }

    Map<String, Object> model;
    String formAction;
    try {
      model = createModel(flow, screen, httpSession, submission, null, request, referer);
      formAction = createFormActionString(flow, screen, submission, referer);
    } catch (Exception e) {
      log.warn("There was an error when trying populate the correct data for screen: {} in flow: {}. It's possible the user pressed back in a subflow. Redirecting to subflow review screen.", screen, flow);
      String subflowName = currentScreen.getSubflow();
      String subflowReviewScreen = subflowManager.getSubflowConfiguration(flow, subflowName).getReviewScreen();
      return new ModelAndView("redirect:/flow/" + flow + "/" + subflowReviewScreen);
    }
    model.put("formAction", formAction);

    return new ModelAndView("%s/%s".formatted(flow, screen), model);
  }

  /**
   * Checks if current screen condition is met.
   *
   * @param uuid          The uuid of a subflow entry
   * @param currentScreen The current screen to check
   * @param submission    submission
   * @return True - current screen does not meet the condition; False - otherwise
   */
  private boolean shouldRedirectToNextScreen(String uuid, ScreenNavigationConfiguration currentScreen, Submission submission) {
    if (conditionManager.conditionExists(currentScreen.getCondition())) {
      if (currentScreen.getSubflow() != null) {
        return !conditionManager.runCondition(currentScreen.getCondition(), submission, uuid);
      } else {
        return !conditionManager.runCondition(currentScreen.getCondition(), submission);
      }
    }
    return false;
  }

  /**
   * Processes input data from current screen.
   *
   * <p>
   * If validation of input data passes this will redirect to move the client to the next screen.
   * </p>
   * <p>
   * If validation of input data fails this will redirect the client to the same screen so they can fix the data.
   * </p>
   *
   * @param formData           The input data from current screen, can be null
   * @param flow               The current flow name, not null
   * @param screen             The current screen name in the flow, not null
   * @param httpSession        The HTTP session if it exists, can be null
   * @param request            The HttpServletRequest
   * @param redirectAttributes The attributes used/modified in the case of a redirect happening
   * @param locale             The language the user request we work in
   * @return a redirect to endpoint that gets the next screen in the flow
   */
  @PostMapping(FLOW_SCREEN_PATH)
  ModelAndView postScreenNoSubmit(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Locale locale
  ) throws SmartyException, IOException, InterruptedException {
    return handlePost(formData, flow, screen, httpSession, request, redirectAttributes, locale, false);
  }

  /**
   * Processes input data from current screen and mark the Submission as "submitted" (complete) during the process.
   *
   * <p>
   * If validation of input data passes this will redirect to move the client to the next screen.
   * </p>
   * <p>
   * If validation of input data fails this will redirect the client to the same screen so they can fix the data.
   * </p>
   *
   * @param formData           The input data from current screen, can be null
   * @param flow               The current flow name, not null
   * @param screen             The current screen name in the flow, not null
   * @param httpSession        The HTTP session if it exists, can be null
   * @param request            The HttpServletRequest
   * @param redirectAttributes The attributes used/modified in the case of a redirect happening
   * @param locale             The language the user request we work in
   * @return a redirect to endpoint that gets the next screen in the flow
   */
  @PostMapping(FLOW_SCREEN_PATH + "/submit")
  ModelAndView postScreenSubmit(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Locale locale
  ) throws SmartyException, IOException, InterruptedException {
    return handlePost(formData, flow, screen, httpSession, request, redirectAttributes, locale, true);
  }

  /**
   * Handle the logic for a POST, taking into account which endpoint the request came through. If it came through the "/submit"
   * version of the endpoint, then we will mark the submission as "submitted". Otherwise, the POST logic is exactly the same for
   * both.
   *
   * @param formData           The input data from current screen, can be null
   * @param requestFlow        The current flow name, not null
   * @param requestScreen      The current screen name in the flow, not null
   * @param httpSession        The HTTP session if it exists, can be null
   * @param request            The HttpServletRequest
   * @param redirectAttributes The attributes used/modified in the case of a redirect happening
   * @param locale             The language the user request we work in
   * @param submitSubmission   Boolean indicating whether the Submission should be marked as officially complete ("submitted")
   * @return a redirect to endpoint that gets the next screen in the flow
   */
  private ModelAndView handlePost(
      MultiValueMap<String, String> formData,
      String requestFlow,
      String requestScreen,
      HttpSession httpSession,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Locale locale,
      boolean submitSubmission
  ) throws SmartyException, IOException, InterruptedException {

    log.info("POST postScreen (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), requestFlow, requestScreen);
    // Checks if screen and flow exist. If getScreenConfig runs successfully, then the flow and screen exist.
    ScreenConfig screenConfig = getValidatedScreenConfiguration(requestFlow, requestScreen);
    ScreenNavigationConfiguration currentScreen = screenConfig.getScreenNavigationConfiguration();
    String flow = screenConfig.getFlowName();
    String screen = currentScreen.getName();

    Submission submission = findOrCreateSubmission(httpSession, flow);

    if (shouldRedirectDueToLockedSubmission(screen, submission, flow)) {
      String lockedSubmissionRedirectUrl = getLockedSubmissionRedirectUrl(flow, redirectAttributes, locale);
      return new ModelAndView("redirect:" + lockedSubmissionRedirectUrl);
    }

    FormSubmission formSubmission = new FormSubmission(formData);
    actionManager.handleOnPostAction(currentScreen, formSubmission, submission);

    // Field validation
    var errorMessages = validationService.validate(currentScreen, flow, formSubmission, submission);
    handleErrors(httpSession, errorMessages, formSubmission);

    if (!errorMessages.isEmpty()) {
      return new ModelAndView(String.format("redirect:/flow/%s/%s", flow, screen));
    }

    // Address validation
    handleAddressValidation(submission, formSubmission);

    // if there's already a session
    if (submission.getId() != null) {
      submission.mergeFormDataWithSubmissionData(formSubmission);
    } else {
      submission.setFlow(flow);
      submission.setInputData(formSubmission.getFormData());
    }

    ShortCodeConfig.Config config = shortCodeConfig.getConfig(flow);
    if (config == null) {
      log.debug("No shortcode configuration found for flow {}, no shortcode will be generated.", flow);
    }

    // handle marking the record as submitted, if necessary
    if (submitSubmission) {
      log.info(
          String.format(
              "Marking the application (%s) as submitted",
              submission.getId()
          )
      );
      submission.setSubmittedAt(OffsetDateTime.now());

      if (config != null && config.isCreateShortCodeAtSubmission()) {
        submissionRepositoryService.generateAndSetUniqueShortCode(submission);
      }
    }

    actionManager.handleBeforeSaveAction(currentScreen, submission);
    submission = saveToRepository(submission);

    if (config != null && config.isCreateShortCodeAtCreation()) {
      submissionRepositoryService.generateAndSetUniqueShortCode(submission);
    }

    setSubmissionInSession(httpSession, submission, flow);
    actionManager.handleAfterSaveAction(currentScreen, submission);

    return new ModelAndView(String.format("redirect:/flow/%s/%s/navigation", flow, screen));
  }

  /**
   * Chooses which screen template and model data to render in a subflow.
   *
   * @param requestFlow   The current flow name, not null
   * @param requestScreen The current screen name in the subflow, not null
   * @param requestUuid   The uuid of a subflow entry, not null
   * @param httpSession   The current httpSession, not null
   * @return the screen template with model data
   */
  @GetMapping({FLOW_SCREEN_PATH + "/{uuid}", FLOW_SCREEN_PATH + "/{uuid}/edit"})
  ModelAndView getSubflowScreen(
      @PathVariable(name = "flow") String requestFlow,
      @PathVariable(name = "screen") String requestScreen,
      @PathVariable(name = "uuid") String requestUuid,
      @RequestHeader(value = "Referer", required = false) String referer,
      HttpSession httpSession,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Locale locale
  ) throws ResponseStatusException {
    log.info(
        "GET getSubflowScreen (url: {}): flow: {}, screen: {}, uuid: {}",
        request.getRequestURI().toLowerCase(),
        requestFlow,
        requestScreen,
        requestUuid);
    // Checks if screen and flow exist
    ScreenConfig screenConfig = getValidatedScreenConfiguration(requestFlow, requestScreen);
    ScreenNavigationConfiguration currentScreen = screenConfig.getScreenNavigationConfiguration();
    String flow = screenConfig.getFlowName();
    String screen = currentScreen.getName();

    Submission submission = getSubmissionFromSession(httpSession, flow);
    String uuid = getValidatedIterationUuid(submission, flow, currentScreen, requestUuid);
    if (uuid == null) {
      throwNotFoundError(submission.getFlow(), currentScreen.getName(),
          String.format("UUID ('%s') not found in iterations for subflow '%s' in flow '%s', when navigating to '%s'",
              uuid, currentScreen.getSubflow(), submission.getFlow(), currentScreen.getName()));
    }

    if (shouldRedirectDueToLockedSubmission(screen, submission, flow)) {
      String lockedSubmissionRedirectUrl = getLockedSubmissionRedirectUrl(flow, redirectAttributes, locale);
      return new ModelAndView("redirect:" + lockedSubmissionRedirectUrl);
    }

    String nextViewableScreen = getNextViewableScreen(flow, screen, uuid, submission);


    if (!nextViewableScreen.equals(screen)) {
      return new ModelAndView(String.format("redirect:/flow/%s/%s/%s", flow, nextViewableScreen, uuid));
    }

    if (submission == null) {
      // we have issues! We should not get here, really.
      log.error("There is no submission associated with request!");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    actionManager.handleBeforeDisplayAction(currentScreen, submission, uuid);
    Map<String, Object> model = createModel(flow, screen, httpSession, submission, uuid, request, referer);
    model.put("formAction", String.format("/flow/%s/%s/%s", flow, screen, uuid));
    return new ModelAndView(String.format("%s/%s", flow, screen), model);
  }

  /**
   * Processes input data from a page of a subflow screen. If `new` is supplied for UUID, then it is assumed this is a new
   * iteration of the subflow and a new UUID is created and associated with the iteration's data.
   *
   * <p>
   * If validation of input data passes this will redirect to move the client to the next screen.
   * </p>
   * <p>
   * If validation of input data fails this will redirect the client to the same subflow screen so they can fix the data.
   * </p>
   *
   * @param formData      The input data from current screen, can be null
   * @param requestFlow   The current flow name, not null
   * @param requestScreen The current screen name in the flow, not null
   * @param uuid          Unique id associated with the subflow's data, or `new` if it is a new iteration of the subflow. Not null.
   * @param httpSession   The HTTP session if it exists, not null
   * @return a redirect to next screen
   */
  @PostMapping({FLOW_SCREEN_PATH + "/{uuid}", FLOW_SCREEN_PATH + "/{uuid}/edit"})
  RedirectView postSubflowScreen(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable(name = "flow") String requestFlow,
      @PathVariable(name = "screen") String requestScreen,
      @PathVariable String uuid,
      HttpSession httpSession,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Locale locale
  ) throws ResponseStatusException, SmartyException, IOException, InterruptedException {
    log.info(
        "POST updateOrCreateIteration (url: {}): flow: {}, screen: {}, uuid: {}",
        request.getRequestURI().toLowerCase(),
        requestFlow,
        requestScreen,
        uuid);

    // Checks to see if flow and screen exist; If they do not, then this will throw an error
    ScreenConfig screenConfig = getValidatedScreenConfiguration(requestFlow, requestScreen);
    ScreenNavigationConfiguration currentScreen = screenConfig.getScreenNavigationConfiguration();
    String flow = screenConfig.getFlowName();
    String screen = currentScreen.getName();

    boolean isNewIteration = uuid.equalsIgnoreCase("new");
    String iterationUuid = isNewIteration ? UUID.randomUUID().toString() : uuid;
    FormSubmission formSubmission = new FormSubmission(formData);
    String subflowName = currentScreen.getSubflow();
    Submission submission = findOrCreateSubmission(httpSession, flow);

    if (shouldRedirectDueToLockedSubmission(screen, submission, flow)) {
      String lockedSubmissionRedirectUrl = getLockedSubmissionRedirectUrl(flow, redirectAttributes, locale);
      return new RedirectView(lockedSubmissionRedirectUrl);
    }

    actionManager.handleOnPostAction(currentScreen, formSubmission, submission, iterationUuid);

    var errorMessages = validationService.validate(currentScreen, flow, formSubmission, submission);
    handleErrors(httpSession, errorMessages, formSubmission);
    if (!errorMessages.isEmpty()) {
      if (isNewIteration) {
        return new RedirectView(String.format("/flow/%s/%s", flow, screen));
      } else {
        return new RedirectView(String.format("/flow/%s/%s/%s", flow, screen, iterationUuid));
      }
    }

    handleAddressValidation(submission, formSubmission);

    if (subflowManager.subflowHasRelationship(flow, currentScreen.getSubflow()) && subflowManager.subflowRelationshipIsNested(flow,
            currentScreen.getSubflow())) {
      RepeatFor repeatForConfiguration = subflowManager.getFlowConfiguration(flow).getSubflows().get(currentScreen.getSubflow()).getRelationship()
              .getRepeatFor();
      String inputNameKey = repeatForConfiguration.getInputName();
      if (formSubmission.getFormData().containsKey(inputNameKey)){
        subflowManager.addNestedSubflowRelationshipData(submission, currentScreen.getSubflow(),  iterationUuid, repeatForConfiguration);
      }
    }


    if (submission.getId() != null) {
      // if we are not working with a new submission, make sure to update any existing data
      // have we submitted any data to the subflow yet?
      if (!submission.getInputData().containsKey(subflowName)) {
        submission.getInputData().put(subflowName, new ArrayList<Map<String, Object>>());
      }
      if (isNewIteration) {
        ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData()
            .get(subflowName);
        formSubmission.getFormData().put("uuid", iterationUuid);
        formSubmission.getFormData().putIfAbsent(Submission.ITERATION_IS_COMPLETE_KEY, false);
        subflow.add(formSubmission.getFormData());
      } else {
        var iterationToEdit = submission.getSubflowEntryByUuid(subflowName, iterationUuid);
        if (iterationToEdit != null) {
          submission.mergeFormDataWithSubflowIterationData(subflowName, iterationToEdit, formSubmission.getFormData());
        }
      }
    } else {
      if (isNewIteration) {
        Map<String, Object> inputData = new HashMap<>();
        ArrayList<Map<String, Object>> subflow = new ArrayList<>();
        formSubmission.getFormData().putIfAbsent(Submission.ITERATION_IS_COMPLETE_KEY, false);
        formSubmission.getFormData().put("uuid", iterationUuid);
        subflow.add(formSubmission.getFormData());
        inputData.put(subflowName, subflow);

        submission.setFlow(flow);
        submission.setInputData(inputData);
      } else {
        // We are not in a current session, so this implies we are on the first page
        // of a flow. If it's not a new iteration, then where _are_ we?
        // Maybe the only way to get here is if the session expired on the client in the middle of a subflow
        log.error(
            String.format(
                "Session information for subflow iteration id (%s) not set. Did the session expire?",
                iterationUuid
            )
        );
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
      }
    }

    actionManager.handleBeforeSaveAction(currentScreen, submission, iterationUuid);
    submission = saveToRepository(submission, subflowName);

    if (shortCodeConfig.getConfig(flow) != null && shortCodeConfig.getConfig(flow).isCreateShortCodeAtCreation()) {
      submissionRepositoryService.generateAndSetUniqueShortCode(submission);
    }

    setSubmissionInSession(httpSession, submission, flow);
    actionManager.handleAfterSaveAction(currentScreen, submission, iterationUuid);

    return new RedirectView(String.format("/flow/%s/%s/navigation?uuid=%s", flow, screen, iterationUuid));
  }

  /**
   * Returns a redirect to delete confirmation screen.
   *
   * @param flow        The current flow name, not null
   * @param subflow     The current subflow name, not null
   * @param uuid        Unique id associated with the subflow's data, not null
   * @param httpSession The HTTP session if it exists, not null
   * @return a redirect to delete confirmation screen for a particular uuid's data
   */
  @GetMapping("{flow}/{subflow}/{uuid}/deleteConfirmation")
  ModelAndView deleteConfirmation(
      @PathVariable String flow,
      @PathVariable String subflow,
      @PathVariable String uuid,
      HttpSession httpSession,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Locale locale
  ) {
    log.info("GET deleteConfirmation (url: {}): flow: {}, uuid: {}", request.getRequestURI().toLowerCase(), flow, uuid);
    // Checks to see if flow exists
    String deleteConfirmationScreen = getValidatedFlowConfigurationByName(flow)
        .getSubflows().get(subflow).getDeleteConfirmationScreen();
    Submission submission = getSubmissionFromSession(httpSession, flow);

    if (shouldRedirectDueToLockedSubmission(deleteConfirmationScreen, submission, flow)) {
      String lockedSubmissionRedirectUrl = getLockedSubmissionRedirectUrl(flow, redirectAttributes, locale);
      return new ModelAndView("redirect:" + lockedSubmissionRedirectUrl);
    }

    if (submission != null) {
      Map<String, Object> entryToDelete = submission.getSubflowEntryByUuid(subflow, uuid);

      if (entryToDelete != null) {
        httpSession.setAttribute("entryToDelete", entryToDelete);
      }
    }

    return new ModelAndView(new RedirectView(String.format("/flow/%s/%s?uuid=%s", flow, deleteConfirmationScreen, uuid)));
  }

  /**
   * Deletes a subflow's input data set, based on that set's uuid.
   *
   * @param requestFlow The current flow name, not null
   * @param subflow     The current subflow name, not null
   * @param uuid        Unique id associated with the subflow's data, not null
   * @param httpSession The HTTP session if it exists, not null
   * @return A screen template and model to redirect to either the entry or review page for a flow, depending on if there is more
   * submission data for a subflow.  If no submission data is found an error template is returned.
   */
  @PostMapping("{flow}/{subflow}/{uuid}/delete")
  ModelAndView deleteSubflowIteration(
      @PathVariable(name = "flow") String requestFlow,
      @PathVariable String subflow,
      @PathVariable String uuid,
      HttpSession httpSession,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes,
      Locale locale
  ) throws ResponseStatusException {
    log.info(
        "POST deleteSubflowIteration (url: {}): flow: {}, uuid: {}",
        request.getRequestURI().toLowerCase(),
        requestFlow,
        uuid);
    // Checks to make sure flow exists; if it doesn't an error is thrown
    FlowConfiguration flowConfiguration = getValidatedFlowConfigurationByName(requestFlow);
    String subflowEntryScreen = flowConfiguration.getSubflows().get(subflow)
        .getEntryScreen();
    String flow = flowConfiguration.getName();

    Submission submission = getSubmissionFromSession(httpSession, flow);
    if (submission == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    if (shouldRedirectDueToLockedSubmission(null, submission, flow)) {
      String lockedSubmissionRedirectUrl = getLockedSubmissionRedirectUrl(flow, redirectAttributes, locale);
      return new ModelAndView("redirect:" + lockedSubmissionRedirectUrl);
    }

    var existingInputData = submission.getInputData();
    if (existingInputData.containsKey(subflow)) {
      var subflowArr = (ArrayList<Map<String, Object>>) existingInputData.get(subflow);
      Optional<Map<String, Object>> entryToDelete = subflowArr.stream()
          .filter(entry -> entry.get("uuid").equals(uuid)).findFirst();
      entryToDelete.ifPresent(subflowArr::remove);
      if (!subflowArr.isEmpty()) {
        existingInputData.put(subflow, subflowArr);
        submission.setInputData(existingInputData);
        submission = saveToRepository(submission, subflow);
      } else {
        existingInputData.remove(subflow);
        submission.setInputData(existingInputData);
        submission = saveToRepository(submission, subflow);
        return new ModelAndView("redirect:/flow/%s/%s".formatted(flow, subflowEntryScreen));
      }
    } else {
      return new ModelAndView("redirect:/flow/%s/%s".formatted(flow, subflowEntryScreen));
    }

    String reviewScreen = getValidatedFlowConfigurationByName(flow).getSubflows().get(subflow)
        .getReviewScreen();
    return new ModelAndView("redirect:/flow/%s/%s".formatted(flow, reviewScreen));
  }

  /**
   * Chooses the next screen template and model to render based on what is next in the flow.
   *
   * @param requestFlow   The current flow name, not null
   * @param requestScreen The current screen name in the flow, not null
   * @param httpSession   The current httpSession, not null
   * @return the screen template with model data, returns error page on error
   */
  @GetMapping(FLOW_SCREEN_PATH + "/navigation")
  ModelAndView navigation(
      @PathVariable(name = "flow") String requestFlow,
      @PathVariable(name = "screen") String requestScreen,
      HttpSession httpSession,
      HttpServletRequest request,
      @RequestParam(name = "uuid", required = false) String requestUuid
  ) {
    log.info("GET navigation (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), requestFlow, requestScreen);
    // Checks if the flow and screen exist
    ScreenConfig screenConfig = getValidatedScreenConfiguration(requestFlow, requestScreen);
    ScreenNavigationConfiguration currentScreen = screenConfig.getScreenNavigationConfiguration();
    String flow = screenConfig.getFlowName();
    String screen = currentScreen.getName();

    Submission submission = getSubmissionFromSession(httpSession, flow);
    log.info(
        "Current submission ID is: {} and current Session ID is: {}", submission.getId(), httpSession.getId());
    if (submission == null) {
      throwNotFoundError(flow, screen,
          String.format("Submission not found in session for flow '{}', when navigating to '{}'", flow, screen));
    }

    String uuid = null;
    if (requestUuid != null && !requestUuid.isBlank()) {
      uuid = getValidatedIterationUuid(submission, flow, currentScreen, requestUuid);
      if (uuid == null) {
        throwNotFoundError(submission.getFlow(), currentScreen.getName(),
            String.format("UUID ('%s') not found in iterations for subflow '%s' in flow '%s', when navigating to '%s'",
                uuid, currentScreen.getSubflow(), submission.getFlow(), currentScreen.getName()));
      }
    }

    String nextScreen = getNextViewableScreen(flow, getNextScreenName(submission, currentScreen, uuid), uuid, submission);


    boolean isCurrentScreenLastInSubflow =
            getValidatedScreenConfiguration(flow, nextScreen).getScreenNavigationConfiguration().getSubflow() == null;
    String redirectString;
    if (uuid != null) {
        String currentSubflowName = currentScreen.getSubflow();
        if (isCurrentScreenLastInSubflow) {
            submission.setIterationIsCompleteToTrue(currentSubflowName, uuid);
            submission = saveToRepository(submission);
            redirectString = String.format("/flow/%s/%s", flow, nextScreen);
            // check if we are in the nested and if we are in the nested, then check if we are done with all of the nested or just the individual one.

            // if you are done with inner and outer flow then move to the review screen.
            redirectString = String.format("/flow/%s/%s/%s", flow,
                    subflowManager.getIterationStartScreenForSubflow(flow, currentSubflowName), uuid);

            // we send to the next nestedSubflow based on iteration == false.

            if (subflowManager.subflowHasRelationship(flow, currentSubflowName)) {
                if (!subflowManager.hasFinishedAllSubflowIterations(currentSubflowName, submission)) {
                    // If you are in a subflow with a relationship we want you to keep looping until you loop over every iteration in the related subflow
                    redirectString = String.format("/flow/%s/%s", flow,
                            subflowManager.getIterationStartScreenForSubflow(flow, currentSubflowName));
                }
            }
        } else {
            // Are you in a nested? and if you are not, go to the nest UUID screen.
          String suuid = "sdfksdfkjh";
            redirectString = String.format("/flow/%s/%s/%s/%s", flow, nextScreen, uuid), suuid);
            // reroute to the corrected nested subflow, otherwise ->
            redirectString = String.format("/flow/%s/%s/%s", flow, nextScreen, uuid);
        }
    } else {
        redirectString = String.format("/flow/%s/%s", flow, nextScreen);
    }

    log.info("navigation: flow: " + flow + ", nextScreen: " + nextScreen);
    return new ModelAndView(new RedirectView(redirectString));
  }


  /**
   * Get the current viewable screen that doesn't have a condition or meets its condition.
   *
   * @param flow       the flow containing the screen
   * @param screen     the current screen
   * @param uuid       current iteration uuid
   * @param submission submission
   * @return Next viewable screen if the current one does not satisfy the condition, otherwise the current screen
   */
  private String getNextViewableScreen(String flow, String screen, String uuid, Submission submission) {
    ScreenConfig screenConfig = getValidatedScreenConfiguration(flow, screen);
    ScreenNavigationConfiguration currentScreen = screenConfig.getScreenNavigationConfiguration();

    if (shouldRedirectToNextScreen(uuid, currentScreen, submission)) {
      String nextScreen = getNextScreenName(submission, currentScreen, uuid);
      return getNextViewableScreen(flow, nextScreen, uuid, submission);
    }

    return screen;
  }

  private String getNextScreenName(Submission submission,
                                   ScreenNavigationConfiguration currentScreen,
                                   String subflowUuid) {
    NextScreen nextScreen;

    List<NextScreen> nextScreens = getConditionalNextScreen(currentScreen, submission, subflowUuid);

    if (isConditionalNavigation(currentScreen) && !nextScreens.isEmpty()) {
      nextScreen = nextScreens.getFirst();
    } else {
      nextScreen = getNonConditionalNextScreen(currentScreen);
    }

    log.info("getNextScreenName: currentScreen: " + currentScreen + ", nextScreen: " + nextScreen.getName());
    return nextScreen.getName();
  }

  /**
   * Fetches the navigation configuration for a particular screen in a particular flow after validating both the screen and flow exist.
   *
   * @param flow   the flow containing the screen
   * @param screen the screen that configuration is wanted for
   * @return navigation configuration for the screen
   */
  private ScreenConfig getValidatedScreenConfiguration(String flow, String screen) {
    FlowConfiguration currentFlowConfiguration = getValidatedFlowConfigurationByName(flow);
    ScreenNavigationConfiguration currentScreen = currentFlowConfiguration.getScreenNavigation(screen);
    if (currentScreen == null) {
      throwNotFoundError(flow, screen, "Screen could not be found in flow configuration for flow " + flow + ".");
    }
    return new ScreenConfig(currentFlowConfiguration.getName(), currentScreen);
  }

  private Boolean isConditionalNavigation(ScreenNavigationConfiguration currentScreen) {
    return currentScreen.getNextScreens().stream()
        .anyMatch(nextScreen -> nextScreen.getCondition() != null);
  }

  /**
   * Returns a list of possible next screens, the ones whose conditions pass
   *
   * @param currentScreen screen you're on
   * @param submission    submission
   * @param subflowUuid   current subflow uuid
   * @return List<NextScreen> list of next screens
   */
  private List<NextScreen> getConditionalNextScreen(ScreenNavigationConfiguration currentScreen,
                                                    Submission submission, String subflowUuid) {
    return currentScreen.getNextScreens().stream()
        .filter(nextScreen -> conditionManager.conditionExists(nextScreen.getCondition()))
        .filter(nextScreen -> {
          if (currentScreen.getSubflow() != null) {
            return conditionManager.runCondition(nextScreen.getCondition(), submission, subflowUuid);
          } else {
            return conditionManager.runCondition(nextScreen.getCondition(), submission);
          }
        })
        .toList();
  }

  private NextScreen getNonConditionalNextScreen(ScreenNavigationConfiguration currentScreen) {
    return currentScreen.getNextScreens().stream()
        .filter(nxtScreen -> nxtScreen.getCondition() == null).toList().get(0);
  }

  private Boolean isIterationStartScreen(String flow, String screen) {
    Map<String, SubflowConfiguration> subflows = getValidatedFlowConfigurationByName(flow).getSubflows();
    if (subflows == null) {
      return false;
    }
    return subflows.entrySet().stream().anyMatch(subflowConfig ->
        subflowConfig.getValue().getIterationStartScreen().equals(screen));
  }

  private String createFormActionString(String flow, String screen, Submission submission, String referer) {
    FlowConfiguration flowConfig = getValidatedFlowConfigurationByName(flow);
    ScreenNavigationConfiguration screenConfig = flowConfig.getScreenNavigation(screen);

    if (!isIterationStartScreen(flow, screen)) {
      return String.format("/flow/%s/%s", flow, screen);
    }

    // If we know we are on an iteration start screen we must be in a subflow so which one?
    String subflowName = screenConfig.getSubflow();

    if (!isIterationStartScreen(flow, screen)) {
        return String.format("/flow/%s/%s", flow, screen);
    }

    if (subflowManager.subflowHasRelationship(flow, subflowName)) {
        String subflowUUID = subflowManager.getUuidOfIterationToUpdate(referer, subflowName, submission.getInputData());

        if (subflowUUID == null) {
            throwNotFoundError(flow, screen,
                    String.format(
                            "UUID ('%s') not found in iterations for subflow '%s' in flow '%s', when navigating to '%s'",
                            subflowUUID, subflowName, submission.getFlow(), screen));
        }

        if (subflowUUID != null && subflowManager.subflowRelationshipIsNested(flow, subflowName)) {

            String nestedSubflowName = subflowManager.nestedSubflowDetails(flow, subflowName).getSaveDataAs();
            String nestedSubflowID = subflowManager.getUuidOfIterationToUpdate(referer, nestedSubflowName,
                    submission.getSubflowEntryByUuid(subflowName, subflowUUID));

            if (null != nestedSubflowID) {
                return String.format("/flow/%s/%s/%s/%s/%s", flow, screen, subflowUUID, nestedSubflowName, nestedSubflowID);
            }

        }
        return String.format("/flow/%s/%s/%s", flow, screen, subflowUUID);
    }

    return String.format("/flow/%s/%s/new", flow, screen);
  }


  private Map<String, Object> createModel(String flow, String screen, HttpSession httpSession, Submission submission,
                                          String uuid, HttpServletRequest request, String referer) {
    Map<String, Object> model = new HashMap<>();
    FlowConfiguration flowConfig = getValidatedFlowConfigurationByName(flow);
    String subflowName = flowConfig.getFlow().get(screen).getSubflow();

    model.put("flow", flow);
    model.put("screen", screen);
    model.put("conditionManager", conditionManager);
    model.put("acceptedFileTypes", fileValidationService.acceptedFileTypes());

    if (subflowName != null) {
      model.put("subflow", subflowName);
    }

    // Put subflow on model if on subflow delete confirmation screen
    Map<String, SubflowConfiguration> subflows = getValidatedFlowConfigurationByName(flow).getSubflows();
    if (subflows != null) {
      List<String> subflowFromDeleteConfirmationConfig = subflows.entrySet().stream()
          .filter(entry ->
              screen.equals(entry.getValue().getDeleteConfirmationScreen()))
          .map(Entry::getKey)
          .toList();

      if (!subflowFromDeleteConfirmationConfig.isEmpty()) {
        model.put("subflow", subflowFromDeleteConfirmationConfig.getFirst());
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
      FormSubmission formSubmission = new FormSubmission(
          (Map<String, Object>) httpSession.getAttribute("formDataSubmission"));
      if (subflowName != null && uuid != null && !uuid.isBlank()) {
        // there is existing data to merge with
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
    model.put("userFiles", userFileRepositoryService.findAllBySubmission(submission));
    if (subflowName != null) {
      if (uuid != null && !uuid.isBlank()) {
        model.put("fieldData", submission.getSubflowEntryByUuid(subflowName, uuid));
      } else if (httpSession.getAttribute("formSubmissionData") != null) {
        // this is a new subflow iteration, we have submission data, so share that
        model.put("fieldData", httpSession.getAttribute("formDataSubmission"));
      }
      // We keep "currentSubflowItem" for backwards compatability at this point
      model.put("currentSubflowItem", model.get("fieldData"));
      if (subflowManager.subflowHasRelationship(flow, subflowName)) {
        model.put("relatedSubflow", subflowManager.getRelatedSubflowName(flow, subflowName));
        String uuidOfIterationToUpdate = (uuid != null && !uuid.isBlank()) ?
                uuid : subflowManager.getUuidOfIterationToUpdate(referer, subflowName, submission);
        if (uuidOfIterationToUpdate == null) {
          throwNotFoundError(flow, screen,
              String.format("UUID was null when trying to find iteration for subflow '%s' in flow '%s. It's possible the user hit back in a subflow. Redirecting.'",
                  uuidOfIterationToUpdate, subflowName, submission.getFlow(), screen));
        }
        model.put("relatedSubflowIteration", subflowManager.getRelatedSubflowIteration(flow, subflowName, uuidOfIterationToUpdate, submission));
      }
    }

    if (RequestContextUtils.getInputFlashMap(request) != null) {
      model.put("lockedSubmissionMessage", RequestContextUtils.getInputFlashMap(request).get("lockedSubmissionMessage"));
    }

    model.put("requiredInputs", ValidationService.getRequiredInputs(flow));

    return model;
  }

  private void handleErrors(HttpSession httpSession, Map<String, List<String>> errorMessages,
                            FormSubmission formSubmission) {
    if (!errorMessages.isEmpty()) {
      httpSession.setAttribute("errorMessages", errorMessages);
      httpSession.setAttribute("formDataSubmission", formSubmission.getFormData());
    } else {
      httpSession.removeAttribute("errorMessages");
      httpSession.removeAttribute("formDataSubmission");
    }
  }

  private Boolean isDeleteConfirmationScreen(String flow, String screen) {
    Map<String, SubflowConfiguration> subflows = getValidatedFlowConfigurationByName(flow).getSubflows();
    if (subflows == null) {
      return false;
    }
    return subflows.entrySet().stream()
        .anyMatch(subflow -> screen.equals(subflow.getValue().getDeleteConfirmationScreen()));
  }

  @Nullable
  private ModelAndView handleDeleteBackBehavior(String flow, String screen, String uuid,
                                                Submission submission) {
    ModelMap model = new ModelMap();
    String subflowName = getValidatedFlowConfigurationByName(flow).getSubflows().entrySet().stream()
        .filter(entry -> screen.equals(entry.getValue().getDeleteConfirmationScreen()))
        .toList().getFirst().getKey();
    ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
    if (subflow == null || subflow.stream().noneMatch(entry -> entry.get("uuid").equals(uuid))) {
      model.put("noEntryToDelete", true);
      model.put("reviewScreen", getValidatedFlowConfigurationByName(flow).getSubflows().get(subflowName).getReviewScreen());
      if (subflow == null) {
        model.put("subflowIsEmpty", true);
        model.put("entryScreen", getValidatedFlowConfigurationByName(flow).getSubflows().get(subflowName).getEntryScreen());
      }
      return new ModelAndView("%s/%s".formatted(flow, screen), model);
    }
    return null;
  }

  /**
   * Runs address validation on the form submission data, but only if there is an address present in the form submission that
   * validation is requested for. This also clears out any fields in the submission that are related to the validated version of
   * that were previously set.
   *
   * @param submission     Submission data from the database
   * @param formSubmission Form data from current POST
   */
  private void handleAddressValidation(Submission submission, FormSubmission formSubmission)
      throws SmartyException, IOException, InterruptedException {

    List<String> addressValidationFields = formSubmission.getAddressValidationFields();
    if (!addressValidationFields.isEmpty()) {
      Map<String, ValidatedAddress> validatedAddresses = addressValidationService.validate(formSubmission);
      formSubmission.setValidatedAddress(validatedAddresses);
      // clear lingering address(es) from the submission stored in the database.
      formSubmission.getAddressValidationFields().forEach(item -> {
        String inputName = item.replace(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS, "");
        submission.clearAddressFields(inputName);
      });
    }
  }

  private String getLockedSubmissionRedirectUrl(String flow, RedirectAttributes redirectAttributes, Locale locale) {
    String lockedSubmissionRedirectPage = formFlowConfigurationProperties.getLockedSubmissionRedirect(flow);
    log.info("The Submission for flow {} is locked. Redirecting to locked submission redirect page: {}", flow,
        lockedSubmissionRedirectPage);
    redirectAttributes.addFlashAttribute("lockedSubmissionMessage",
        messageSource.getMessage("general.locked-submission", null, locale));
    return String.format("/flow/%s/%s", flow, lockedSubmissionRedirectPage);
  }

  /**
   * Return the validated UUID in String for, or returns null if not found.
   *
   * @param submission    The submission to search in
   * @param flow          The current flow we are working in
   * @param currentScreen The current screen we are going to
   * @param uuidToVerify  The String UUID to validate
   * @return Validated UUID as String, or returns null if not found
   */
  private String getValidatedIterationUuid(Submission submission, String flow, ScreenNavigationConfiguration currentScreen, String uuidToVerify) {
    FlowConfiguration flowConfiguration = getValidatedFlowConfigurationByName(flow);
    if (flowConfiguration == null) {
      return null;
    }

    Map<String, Object> iteration = null;
    List<String> subflowNameList = new ArrayList<>();

    // see if we have a specific subflow to look in
    if (currentScreen.getSubflow() != null) {
      subflowNameList.add(currentScreen.getSubflow());
    } else {
      // we don't know the subflow, so check them all
      subflowNameList.addAll(flowConfiguration.getSubflows().keySet().stream().toList());
    }

    for (String subflowName : subflowNameList) {
      iteration = submission.getSubflowEntryByUuid(subflowName, uuidToVerify);
      if (iteration != null) {
        break;
      }
    }

    return iteration != null ? (String) iteration.get("uuid") : null;
}

    private Optional<Map<String, Object>> getValidatedNestedIterationUuid(Submission submission, String flow,
            ScreenNavigationConfiguration currentScreen, String uuidToVerify, String saveAsKey, String nestedSubflowUuid) {
        FlowConfiguration flowConfiguration = getValidatedFlowConfigurationByName(flow);
        if (flowConfiguration == null) {
            return null;
        }

        Map<String, Object> iteration = null;
        List<String> subflowNameList = new ArrayList<>();

        // see if we have a specific subflow to look in
        if (currentScreen.getSubflow() != null) {
            subflowNameList.add(currentScreen.getSubflow());
        } else {
            // we don't know the subflow, so check them all
            subflowNameList.addAll(flowConfiguration.getSubflows().keySet().stream().toList());
        }

        for (String subflowName : subflowNameList) {
            iteration = submission.getSubflowEntryByUuid(subflowName, uuidToVerify);

            if (iteration.containsKey(saveAsKey)) {
                List<Map<String, Object>> nestedSubflowData = (List<Map<String, Object>>) iteration.get(saveAsKey);

                List<Map<String, Object>> matchingNestedSubflows = nestedSubflowData.stream()
                        .filter(subflow -> subflow.get("uuid").equals(nestedSubflowUuid)).collect(Collectors.toList());

                if (matchingNestedSubflows.isEmpty()) {
                    iteration = null;
                } else {
                    if (matchingNestedSubflows.size() > 1) {
                        log.info(
                                "This nested subflow {} id {} has multiple records for the same subflow. Displaying the first one",
                                saveAsKey, nestedSubflowUuid);
                    }

                    iteration = matchingNestedSubflows.get(0);
                }


            }
            if (iteration != null) {
                break;
            }
        }

        return iteration != null ? Optional.of(iteration) : Optional.empty();
    }
}
