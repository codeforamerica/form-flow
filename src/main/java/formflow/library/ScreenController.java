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
import formflow.library.inputs.UnvalidatedField;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.view.RedirectView;

/**
 * A controller to render any screen in flows, including subflows.
 */
@Controller
@EnableAutoConfiguration
@Slf4j
@RequestMapping("/flow")
public class ScreenController extends FormFlowController {

  private final List<FlowConfiguration> flowConfigurations;
  private final ValidationService validationService;

  private final AddressValidationService addressValidationService;

  private final ConditionManager conditionManager;
  private final ActionManager actionManager;

  public ScreenController(
      List<FlowConfiguration> flowConfigurations,
      SubmissionRepositoryService submissionRepositoryService,
      ValidationService validationService,
      AddressValidationService addressValidationService,
      ConditionManager conditionManager,
      ActionManager actionManager) {
    super(submissionRepositoryService);
    this.flowConfigurations = flowConfigurations;
    this.validationService = validationService;
    this.addressValidationService = addressValidationService;
    this.conditionManager = conditionManager;
    this.actionManager = actionManager;
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
  @GetMapping("{flow}/{screen}")
  ModelAndView getScreen(
      @PathVariable String flow,
      @PathVariable String screen,
      @RequestParam(required = false) Map<String, String> query_params,
      @RequestParam(value = "uuid", required = false) String uuid,
      HttpSession httpSession
  ) throws Exception {
    log.info("getScreen: flow: " + flow + ", screen: " + screen);

    var currentScreen = getScreenConfig(flow, screen);
    Submission submission = submissionRepositoryService.findOrCreate(httpSession);
    if ((submission.getUrlParams() != null) && (!submission.getUrlParams().isEmpty())) {
      submission.mergeUrlParamsWithData(query_params);
    } else {
      submission.setUrlParams(query_params);
    }

    submission.setFlow(flow);
    saveToRepository(submission);
    httpSession.setAttribute("id", submission.getId());
    if (currentScreen == null) {
      errorScreen(HttpMethod.GET.toString(), String.format("/flow/%s/%s", flow, screen), HttpStatus.NOT_FOUND);
    } else {
      if (uuid != null) {
        actionManager.handleBeforeDisplayAction(currentScreen, submission, uuid);
      } else {
        actionManager.handleBeforeDisplayAction(currentScreen, submission);
      }
    }

    Map<String, Object> model = createModel(flow, screen, httpSession, submission);

    String formAction = createFormActionString(flow, screen);
    model.put("formAction", formAction);
    if (isDeleteConfirmationScreen(flow, screen)) {
      ModelAndView nothingToDeleteModelAndView = handleDeleteBackBehavior(flow, screen, uuid, submission);
      if (nothingToDeleteModelAndView != null) {
        return nothingToDeleteModelAndView;
      }
    }

    log.info("getScreen: flow: " + flow + ", screen: " + screen);
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
  @PostMapping({"{flow}/{screen}", "{flow}/{screen}/submit"})
  ModelAndView postScreen(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession,
      HttpServletRequest httpServletRequest
  ) throws SmartyException, IOException, InterruptedException {
    log.info("postScreen: flow: " + flow + ", screen: " + screen);
    Submission submission = submissionRepositoryService.findOrCreate(httpSession);
    FormSubmission formSubmission = new FormSubmission(formData);
    var currentScreen = getScreenConfig(flow, screen);

    actionManager.handleOnPostAction(currentScreen, formSubmission);

    // Field validation
    var errorMessages = validationService.validate(currentScreen, flow, formSubmission);
    handleErrors(httpSession, errorMessages, formSubmission);

    if (errorMessages.size() > 0) {
      return new ModelAndView(String.format("redirect:/flow/%s/%s", flow, screen));
    }

    // Address validation
    handleAddressValidation(submission, formSubmission);

    // handle submit actions, if requested
    if (httpServletRequest.getRequestURI().toLowerCase().contains("submit")) {
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
    saveToRepository(submission);
    httpSession.setAttribute("id", submission.getId());
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
  @GetMapping({"{flow}/{screen}/{uuid}", "{flow}/{screen}/{uuid}/edit"})
  ModelAndView getSubflowScreen(
      @PathVariable String flow,
      @PathVariable String screen,
      @PathVariable String uuid,
      HttpSession httpSession
  ) throws Exception {
    Optional<Submission> maybeSubmission = submissionRepositoryService.findById((UUID) httpSession.getAttribute("id"));

    if (maybeSubmission.isEmpty()) {
      // we have issues! We should not get here, really.
      log.error("There is no submission associated with request!");
      errorScreen(HttpMethod.GET.toString(), String.format("/flow/%s/%s/%s", flow, screen, uuid), HttpStatus.BAD_REQUEST);
    }

    Submission submission = maybeSubmission.get();
    var currentScreen = getScreenConfig(flow, screen);
    actionManager.handleBeforeDisplayAction(currentScreen, submission, uuid);
    Map<String, Object> model = createModel(flow, screen, httpSession, submission);
    var currentObject = submission.getSubflowEntryByUuid(currentScreen.getSubflow(), uuid);
    model.put("currentSubflowItem", currentObject);
    // createModel sets fieldData to submission.inputData; override fieldData with the subflow's data
    model.put("fieldData", currentObject);
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
  @PostMapping({"{flow}/{screen}/{uuid}", "{flow}/{screen}/{uuid}/edit"})
  ModelAndView updateOrCreateIteration(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      @PathVariable String uuid,
      HttpSession httpSession
  ) throws Exception {
    log.info("addToIteration: flow: " + flow + ", screen: " + screen + ", uuid: " + uuid);
    boolean isNewIteration = uuid.equalsIgnoreCase("new");
    String iterationUuid = isNewIteration ? UUID.randomUUID().toString() : uuid;
    FormSubmission formSubmission = new FormSubmission(formData);
    Submission submission = submissionRepositoryService.findOrCreate(httpSession);
    ScreenNavigationConfiguration currentScreen = getScreenConfig(flow, screen);
    String subflowName = currentScreen.getSubflow();

    actionManager.handleOnPostAction(currentScreen, formSubmission, iterationUuid);

    if (isNewIteration) {
      // handle start iteration page, if new flow
      HashMap<String, SubflowConfiguration> subflows = getFlowConfigurationByName(flow).getSubflows();
      subflowName = subflows.entrySet().stream()
          .filter(subflow -> subflow.getValue().getIterationStartScreen().equals(screen))
          .map(Entry::getKey)
          .findFirst()
          .orElse(null);
    }

    var errorMessages = validationService.validate(currentScreen, flow, formSubmission);
    handleErrors(httpSession, errorMessages, formSubmission);
    if (errorMessages.size() > 0) {
      if (isNewIteration) {
        return new ModelAndView(String.format("redirect:/flow/%s/%s", flow, screen));
      } else {
        return new ModelAndView(String.format("redirect:/flow/%s/%s/%s", flow, screen, iterationUuid));
      }
    }

    // TODO: validate addresses

    if (httpSession.getAttribute("id") != null) {
      Boolean iterationIsComplete = !isNextScreenInSubflow(flow, httpSession, currentScreen, iterationUuid);
      formSubmission.getFormData().put("iterationIsComplete", iterationIsComplete);
      // have we submitted any data to the subflow yet?
      if (!submission.getInputData().containsKey(subflowName)) {
        submission.getInputData().put(subflowName, new ArrayList<Map<String, Object>>());
      }
      if (isNewIteration) {
        ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
        formSubmission.getFormData().put("uuid", iterationUuid);
        subflow.add(formSubmission.getFormData());
      } else {
        var iterationToEdit = submission.getSubflowEntryByUuid(subflowName, iterationUuid);
        if (iterationToEdit != null) {
          submission.mergeFormDataWithSubflowIterationData(subflowName, iterationToEdit, formSubmission.getFormData());
          submission.removeIncompleteIterations(subflowName, iterationUuid);
        }
      }
    } else {
      if (isNewIteration) {
        submission.setFlow(flow);
        submission.setInputData(formSubmission.getFormData());
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
        errorScreen(HttpMethod.POST.toString(), String.format("/flow/%s/%s/%s", flow, screen, uuid), HttpStatus.BAD_REQUEST);
      }
    }

    actionManager.handleBeforeSaveAction(currentScreen, submission, iterationUuid);
    saveToRepository(submission, subflowName);
    httpSession.setAttribute("id", submission.getId());
    actionManager.handleAfterSaveAction(currentScreen, submission, iterationUuid);

    String nextScreen = getNextScreenName(httpSession, currentScreen, iterationUuid);
    String viewString = isNextScreenInSubflow(flow, httpSession, currentScreen, iterationUuid) ?
        String.format("redirect:/flow/%s/%s/%s", flow, nextScreen, iterationUuid)
        : String.format("redirect:/flow/%s/%s", flow, nextScreen);
    return new ModelAndView(viewString);
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
      HttpSession httpSession
  ) {
    String deleteConfirmationScreen = getFlowConfigurationByName(flow)
        .getSubflows().get(subflow).getDeleteConfirmationScreen();
    UUID id = (UUID) httpSession.getAttribute("id");
    Optional<Submission> submissionOptional = submissionRepositoryService.findById(id);

    if (submissionOptional.isPresent()) {
      Submission submission = submissionOptional.get();
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
      HttpSession httpSession
  ) throws Exception {
    UUID id = (UUID) httpSession.getAttribute("id");
    Optional<Submission> submissionOptional = submissionRepositoryService.findById(id);
    String subflowEntryScreen = getFlowConfigurationByName(flow).getSubflows().get(subflow)
        .getEntryScreen();
    if (submissionOptional.isPresent()) {
      Submission submission = submissionOptional.get();
      var existingInputData = submission.getInputData();
      if (existingInputData.containsKey(subflow)) {
        var subflowArr = (ArrayList<Map<String, Object>>) existingInputData.get(subflow);
        Optional<Map<String, Object>> entryToDelete = subflowArr.stream()
            .filter(entry -> entry.get("uuid").equals(uuid)).findFirst();
        entryToDelete.ifPresent(subflowArr::remove);
        if (!subflowArr.isEmpty()) {
          existingInputData.put(subflow, subflowArr);
          submission.setInputData(existingInputData);
          saveToRepository(submission, subflow);
        } else {
          existingInputData.remove(subflow);
          submission.setInputData(existingInputData);
          saveToRepository(submission, subflow);
          return new ModelAndView("redirect:/flow/%s/%s".formatted(flow, subflowEntryScreen));
        }
      } else {
        return new ModelAndView("redirect:/flow/%s/%s".formatted(flow, subflowEntryScreen));
      }
    } else {
      errorScreen(HttpMethod.POST.toString(), String.format("/flow/%s/%s/%s", flow, subflow, uuid), HttpStatus.BAD_REQUEST);
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
  @GetMapping("{flow}/{screen}/navigation")
  ModelAndView navigation(
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession
  ) throws Exception {
    var currentScreen = getScreenConfig(flow, screen);
    log.info("navigation: flow: " + flow + ", screen: " + screen);
    if (currentScreen == null) {
      errorScreen(HttpMethod.GET.toString(), String.format("/flow/%s/%s", flow, screen), HttpStatus.NOT_FOUND);
    }
    String nextScreen = getNextScreenName(httpSession, currentScreen, null);

    log.info("navigation: flow: " + flow + ", nextScreen: " + nextScreen);
    return new ModelAndView(new RedirectView("/flow/%s/%s".formatted(flow, nextScreen)));
  }

  private static void errorScreen(String method, String url, HttpStatus httpStatus) throws Exception {
    HttpHeaders headers = new HttpHeaders();

    if (httpStatus.isSameCodeAs(HttpStatus.NOT_FOUND)) {
      throw new NoHandlerFoundException(method, url, headers);
    }

    if (httpStatus.isSameCodeAs(HttpStatus.BAD_REQUEST)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
  }

  private String getNextScreenName(HttpSession httpSession,
      ScreenNavigationConfiguration currentScreen, String subflowUuid) {
    NextScreen nextScreen;

    List<NextScreen> nextScreens = getConditionalNextScreen(currentScreen, httpSession, subflowUuid);

    if (isConditionalNavigation(currentScreen) && nextScreens.size() > 0) {
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
    return currentFlowConfiguration.getScreenNavigation(screen);
  }

  private FlowConfiguration getFlowConfigurationByName(String flow) {
    try {
      return flowConfigurations.stream().filter(
          flowConfiguration -> flowConfiguration.getName().equals(flow)
      ).toList().get(0);

    } catch (ArrayIndexOutOfBoundsException e) {
      throw new NoSuchElementException("Could not find flow=" + flow + " in templates");
    }

  }

  private Boolean isConditionalNavigation(ScreenNavigationConfiguration currentScreen) {
    return currentScreen.getNextScreens().stream()
        .anyMatch(nextScreen -> nextScreen.getCondition() != null);
  }

  /**
   * Returns a list of possible next screens, the ones whose conditions pass
   *
   * @param currentScreen screen you're on
   * @param httpSession   session
   * @param subflowUuid   current subflow uuid
   * @return List<NextScreen> list of next screens
   */
  private List<NextScreen> getConditionalNextScreen(ScreenNavigationConfiguration currentScreen,
      HttpSession httpSession, String subflowUuid) {
    return currentScreen.getNextScreens().stream()
        .filter(nextScreen -> conditionManager.conditionExists(nextScreen.getCondition()))
        .filter(nextScreen -> {
          if (currentScreen.getSubflow() != null) {
            return conditionManager.runCondition(nextScreen.getCondition(), submissionRepositoryService.findOrCreate(httpSession),
                subflowUuid);
          } else {
            return conditionManager.runCondition(nextScreen.getCondition(),
                submissionRepositoryService.findOrCreate(httpSession));
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

  private Boolean isNextScreenInSubflow(String flow, HttpSession session, ScreenNavigationConfiguration currentScreen,
      String subflowUuid) {
    String nextScreenName = getNextScreenName(session, currentScreen, subflowUuid);
    return getScreenConfig(flow, nextScreenName).getSubflow() != null;
  }

  private String createFormActionString(String flow, String screen) {
    return isIterationStartScreen(flow, screen) ?
        "/flow/%s/%s/new".formatted(flow, screen) : "/flow/%s/%s".formatted(flow, screen);
  }

  private Map<String, Object> createModel(String flow, String screen, HttpSession httpSession, Submission submission) {
    Map<String, Object> model = new HashMap<>();
    FlowConfiguration flowConfig = getFlowConfigurationByName(flow);
    model.put("flow", flow);
    model.put("screen", screen);
    model.put("conditionManager", conditionManager);

    if (flowConfig.getFlow().get(screen).getSubflow() != null) {
      model.put("subflow", flowConfig.getFlow().get(screen).getSubflow());
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

    // If there are errors, merge form data that was submitted, with already existing inputData
    if (httpSession.getAttribute("formDataSubmission") != null) {
      submission.mergeFormDataWithSubmissionData(
          new FormSubmission((Map<String, Object>) httpSession.getAttribute("formDataSubmission")));
    }

    model.put("submission", submission);
    model.put("inputData", submission.getInputData());
    // default fieldData to "inputData", but it might be replaced with subflow data later on
    model.put("fieldData", submission.getInputData());
    model.put("errorMessages", httpSession.getAttribute("errorMessages"));

    return model;
  }

  private void handleErrors(HttpSession httpSession, Map<String, List<String>> errorMessages,
      FormSubmission formSubmission) {
    if (errorMessages.size() > 0) {
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
