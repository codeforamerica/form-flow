package formflow.library;

import com.smartystreets.api.exceptions.SmartyException;
import formflow.library.address_validation.AddressValidationService;
import formflow.library.address_validation.ValidatedAddress;
import formflow.library.config.ActionManager;
import formflow.library.config.ConditionManager;
import formflow.library.config.FlowConfiguration;
import formflow.library.config.NextScreen;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.config.SubflowConfiguration;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.file.FileValidationService;
import formflow.library.inputs.UnvalidatedField;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
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

  public ScreenController(
      List<FlowConfiguration> flowConfigurations,
      UserFileRepositoryService userFileRepositoryService,
      SubmissionRepositoryService submissionRepositoryService,
      ValidationService validationService,
      AddressValidationService addressValidationService,
      ConditionManager conditionManager,
      ActionManager actionManager,
      FileValidationService fileValidationService) {
    super(submissionRepositoryService, userFileRepositoryService, flowConfigurations);
    this.validationService = validationService;
    this.addressValidationService = addressValidationService;
    this.conditionManager = conditionManager;
    this.actionManager = actionManager;
    this.fileValidationService = fileValidationService;
    log.info("Screen Controller Created!");
  }

  /**
   * Chooses which screen template and model data to render.
   *
   * @param flow        The current flow name, not null
   * @param screen      The current screen name in the flow, not null
   * @param uuid        The uuid of a subflow entry, can be null
   * @param httpSession The current httpSession, not null
   * @return the screen template with model data
   */
  @GetMapping(FLOW_SCREEN_PATH)
  ModelAndView getScreen(
      @PathVariable String flow,
      @PathVariable String screen,
      @RequestParam(required = false) Map<String, String> query_params,
      @RequestParam(value = "uuid", required = false) String uuid,
      HttpSession httpSession,
      HttpServletRequest request
  ) {
    log.info("GET getScreen (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), flow, screen);
    // this will ensure that the screen and flow actually exist
    ScreenNavigationConfiguration currentScreen = getScreenConfig(flow, screen);
    Submission submission = findOrCreateSubmission(httpSession, flow);

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

    Map<String, Object> model = createModel(flow, screen, httpSession, submission, null);

    String formAction = createFormActionString(flow, screen);
    model.put("formAction", formAction);
    if (isDeleteConfirmationScreen(flow, screen)) {
      ModelAndView nothingToDeleteModelAndView = handleDeleteBackBehavior(flow, screen, uuid, submission);
      if (nothingToDeleteModelAndView != null) {
        return nothingToDeleteModelAndView;
      }
    }

    return new ModelAndView("%s/%s".formatted(flow, screen), model);
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
   * @param formData    The input data from current screen, can be null
   * @param flow        The current flow name, not null
   * @param screen      The current screen name in the flow, not null
   * @param httpSession The HTTP session if it exists, can be null
   * @return a redirect to endpoint that gets the next screen in the flow
   */
  @PostMapping({FLOW_SCREEN_PATH, FLOW_SCREEN_PATH + "/submit"})
  ModelAndView postScreen(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession,
      HttpServletRequest request
  ) throws SmartyException, IOException, InterruptedException {
    log.info("POST postScreen (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), flow, screen);
    // Checks if screen and flow exist
    var currentScreen = getScreenConfig(flow, screen);
    Submission submission = findOrCreateSubmission(httpSession, flow);
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

    // handle submit actions, if requested
    if (request.getRequestURI().toLowerCase().contains("submit")) {
      log.info(
          String.format(
              "Marking the application (%s) as submitted",
              submission.getId()
          )
      );
      submission.setSubmittedAt(DateTime.now().toDate());
    }

    // if there's already a session
    if (submission.getId() != null) {
      submission.mergeFormDataWithSubmissionData(formSubmission);
    } else {
      submission.setFlow(flow);
      submission.setInputData(formSubmission.getFormData());
    }

    actionManager.handleBeforeSaveAction(currentScreen, submission);
    submission = saveToRepository(submission);
    setSubmissionInSession(httpSession, submission, flow);
    actionManager.handleAfterSaveAction(currentScreen, submission);

    return new ModelAndView(String.format("redirect:/flow/%s/%s/navigation", flow, screen));
  }

  /**
   * Chooses which screen template and model data to render in a subflow.
   *
   * @param flow        The current flow name, not null
   * @param screen      The current screen name in the subflow, not null
   * @param uuid        The uuid of a subflow entry, not null
   * @param httpSession The current httpSession, not null
   * @return the screen template with model data
   */
  @GetMapping({FLOW_SCREEN_PATH + "/{uuid}", FLOW_SCREEN_PATH + "/{uuid}/edit"})
  ModelAndView getSubflowScreen(
      @PathVariable String flow,
      @PathVariable String screen,
      @PathVariable String uuid,
      HttpSession httpSession,
      HttpServletRequest request
  ) throws ResponseStatusException {
    log.info(
        "GET getSubflowScreen (url: {}): flow: {}, screen: {}, uuid: {}",
        request.getRequestURI().toLowerCase(),
        flow,
        screen,
        uuid);
    // Checks if screen and flow exist
    var currentScreen = getScreenConfig(flow, screen);
    Submission submission = getSubmissionFromSession(httpSession, flow);

    if (submission == null) {
      // we have issues! We should not get here, really.
      log.error("There is no submission associated with request!");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    actionManager.handleBeforeDisplayAction(currentScreen, submission, uuid);
    Map<String, Object> model = createModel(flow, screen, httpSession, submission, uuid);
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
   * @param formData    The input data from current screen, can be null
   * @param flow        The current flow name, not null
   * @param screen      The current screen name in the flow, not null
   * @param uuid        Unique id associated with the subflow's data, or `new` if it is a new iteration of the subflow. Not null.
   * @param httpSession The HTTP session if it exists, not null
   * @return a redirect to next screen
   */
  @PostMapping({FLOW_SCREEN_PATH + "/{uuid}", FLOW_SCREEN_PATH + "/{uuid}/edit"})
  RedirectView postSubflowScreen(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      @PathVariable String uuid,
      HttpSession httpSession,
      HttpServletRequest request
  ) throws ResponseStatusException, SmartyException, IOException, InterruptedException {
    log.info(
        "POST updateOrCreateIteration (url: {}): flow: {}, screen: {}, uuid: {}",
        request.getRequestURI().toLowerCase(),
        flow,
        screen,
        uuid);
    // Checks to see if flow and screen exist
    ScreenNavigationConfiguration currentScreen = getScreenConfig(flow, screen);
    boolean isNewIteration = uuid.equalsIgnoreCase("new");
    String iterationUuid = isNewIteration ? UUID.randomUUID().toString() : uuid;
    FormSubmission formSubmission = new FormSubmission(formData);
    String subflowName = currentScreen.getSubflow();
    Submission submission = findOrCreateSubmission(httpSession, flow);
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

    if (submission.getId() != null) {
      // if we are not working with a new submission, make sure to update any existing data
      // have we submitted any data to the subflow yet?
      if (!submission.getInputData().containsKey(subflowName)) {
        submission.getInputData().put(subflowName, new ArrayList<Map<String, Object>>());
      }
      if (isNewIteration) {
        ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
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
      HttpServletRequest request
  ) {
    log.info("GET deleteConfirmation (url: {}): flow: {}, uuid: {}", request.getRequestURI().toLowerCase(), flow, uuid);
    // Checks to see if flow exists
    String deleteConfirmationScreen = getFlowConfigurationByName(flow)
        .getSubflows().get(subflow).getDeleteConfirmationScreen();
    Submission submission = getSubmissionFromSession(httpSession, flow);

    if (submission != null) {
      var existingInputData = submission.getInputData();
      var subflowArr = (ArrayList<Map<String, Object>>) existingInputData.get(subflow);
      var entryToDelete = subflowArr.stream().filter(entry -> entry.get("uuid").equals(uuid)).findFirst();
      entryToDelete.ifPresent(entry -> httpSession.setAttribute("entryToDelete", entry));
    }

    return new ModelAndView(new RedirectView(String.format("/flow/%s/" + deleteConfirmationScreen + "?uuid=" + uuid, flow)));
  }

  /**
   * Deletes a subflow's input data set, based on that set's uuid.
   *
   * @param flow        The current flow name, not null
   * @param subflow     The current subflow name, not null
   * @param uuid        Unique id associated with the subflow's data, not null
   * @param httpSession The HTTP session if it exists, not null
   * @return A screen template and model to redirect to either the entry or review page for a flow, depending on if there is more
   * submission data for a subflow.  If no submission data is found an error template is returned.
   */
  @PostMapping("{flow}/{subflow}/{uuid}/delete")
  ModelAndView deleteSubflowIteration(
      @PathVariable String flow,
      @PathVariable String subflow,
      @PathVariable String uuid,
      HttpSession httpSession,
      HttpServletRequest request
  ) throws ResponseStatusException {
    log.info(
        "POST deleteSubflowIteration (url: {}): flow: {}, uuid: {}",
        request.getRequestURI().toLowerCase(),
        flow,
        uuid);
    // Checks to make sure flow exists
    String subflowEntryScreen = getFlowConfigurationByName(flow).getSubflows().get(subflow)
        .getEntryScreen();
    Submission submission = getSubmissionFromSession(httpSession, flow);
    if (submission == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
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

    String reviewScreen = getFlowConfigurationByName(flow).getSubflows().get(subflow)
        .getReviewScreen();
    return new ModelAndView(String.format("redirect:/flow/%s/" + reviewScreen, flow));
  }

  /**
   * Chooses the next screen template and model to render based on what is next in the flow.
   *
   * @param flow        The current flow name, not null
   * @param screen      The current screen name in the flow, not null
   * @param httpSession The current httpSession, not null
   * @return the screen template with model data, returns error page on error
   */
  @GetMapping(FLOW_SCREEN_PATH + "/navigation")
  ModelAndView navigation(
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession,
      HttpServletRequest request,
      @RequestParam(required = false) String uuid
  ) {
    log.info("GET navigation (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), flow, screen);
    log.info(
        "Current submission ID is: " + httpSession.getAttribute("id") + " and current Session ID is: " + httpSession.getId());
    // Checks if the screen and flow exist
    var currentScreen = getScreenConfig(flow, screen);
    Submission submission = getSubmissionFromSession(httpSession, flow);
    if (submission == null) {
      throwNotFoundError(flow, screen,
          String.format("Submission not found in session for flow '{}', when navigating to '{}'", flow, screen));
    }
    String nextScreen = getNextScreenName(submission, currentScreen, uuid);

    boolean isCurrentScreenLastInSubflow = getScreenConfig(flow, nextScreen).getSubflow() == null;
    String redirectString;
    if (uuid != null && isCurrentScreenLastInSubflow) {
      submission.setIterationIsCompleteToTrue(currentScreen.getSubflow(), uuid);
      submission = saveToRepository(submission);
      redirectString = String.format("/flow/%s/%s", flow, nextScreen);
    } else {
      redirectString = String.format("/flow/%s/%s/%s", flow, nextScreen, uuid);
    }
    log.info("navigation: flow: " + flow + ", nextScreen: " + nextScreen);
    return new ModelAndView(new RedirectView(redirectString));
  }

  private String getNextScreenName(Submission submission,
      ScreenNavigationConfiguration currentScreen, String subflowUuid) {
    NextScreen nextScreen;

    List<NextScreen> nextScreens = getConditionalNextScreen(currentScreen, submission, subflowUuid);

    if (isConditionalNavigation(currentScreen) && !nextScreens.isEmpty()) {
      nextScreen = nextScreens.get(0);
    } else {
      // TODO this needs to throw an error if there are more than 1 next screen that don't have a condition or more than one evaluate to true
      nextScreen = getNonConditionalNextScreen(currentScreen);
    }

    log.info("getNextScreenName: currentScreen:" + currentScreen + ", nextScreen: " + nextScreen.getName());
    // TODO throw a better error if the next screen doesn't exist (incorrect name / name is not in flow config)
    return nextScreen.getName();
  }

  /**
   * Fetches the navigation configuration for a particular screen in a particular flow.
   *
   * @param flow   the flow containing the screen
   * @param screen the screen that configuration is wanted for
   * @return navigation configuration for the screen
   */
  private ScreenNavigationConfiguration getScreenConfig(String flow, String screen) {
    FlowConfiguration currentFlowConfiguration = getFlowConfigurationByName(flow);
    ScreenNavigationConfiguration currentScreen = currentFlowConfiguration.getScreenNavigation(screen);
    if (currentScreen == null) {
      throwNotFoundError(flow, screen, "Screen could not be found in flow configuration for flow " + flow + ".");
    }
    return currentFlowConfiguration.getScreenNavigation(screen);
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
    HashMap<String, SubflowConfiguration> subflows = getFlowConfigurationByName(flow).getSubflows();
    if (subflows == null) {
      return false;
    }
    return subflows.entrySet().stream().anyMatch(subflowConfig ->
        subflowConfig.getValue().getIterationStartScreen().equals(screen));
  }

  private String createFormActionString(String flow, String screen) {
    return isIterationStartScreen(flow, screen) ?
        "/flow/%s/%s/new".formatted(flow, screen) : "/flow/%s/%s".formatted(flow, screen);
  }

  private Map<String, Object> createModel(String flow, String screen, HttpSession httpSession, Submission submission,
      String uuid) {
    Map<String, Object> model = new HashMap<>();
    FlowConfiguration flowConfig = getFlowConfigurationByName(flow);
    String subflowName = flowConfig.getFlow().get(screen).getSubflow();

    model.put("flow", flow);
    model.put("screen", screen);
    model.put("conditionManager", conditionManager);
    model.put("acceptedFileTypes", fileValidationService.acceptedFileTypes());

    if (subflowName != null) {
      model.put("subflow", subflowName);
    }

    // Put subflow on model if on subflow delete confirmation screen
    HashMap<String, SubflowConfiguration> subflows = getFlowConfigurationByName(flow).getSubflows();
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
    }

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
    HashMap<String, SubflowConfiguration> subflows = getFlowConfigurationByName(flow).getSubflows();
    if (subflows == null) {
      return false;
    }
    return subflows.entrySet().stream()
        .anyMatch(subflow -> subflow.getValue().getDeleteConfirmationScreen().equals(screen));
  }

  @Nullable
  private ModelAndView handleDeleteBackBehavior(String flow, String screen, String uuid,
      Submission submission) {
    ModelMap model = new ModelMap();
    String subflowName = getFlowConfigurationByName(
        flow).getSubflows().entrySet().stream()
        .filter(entry -> entry.getValue().getDeleteConfirmationScreen().equals(screen))
        .toList().get(0).getKey();
    ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
    if (subflow == null || subflow.stream().noneMatch(entry -> entry.get("uuid").equals(uuid))) {
      model.put("noEntryToDelete", true);
      model.put("reviewScreen", getFlowConfigurationByName(flow).getSubflows().get(subflowName).getReviewScreen());
      if (subflow == null) {
        model.put("subflowIsEmpty", true);
        model.put("entryScreen", getFlowConfigurationByName(flow).getSubflows().get(subflowName).getEntryScreen());
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
        String inputName = item.replace(UnvalidatedField.VALIDATE_ADDRESS, "");
        submission.clearAddressFields(inputName);
      });
    }
  }
}
