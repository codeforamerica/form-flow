package formflow.library;

import com.smartystreets.api.exceptions.SmartyException;
import formflow.library.config.ActionManager;
import formflow.library.config.ConditionManager;
import formflow.library.config.FlowConfiguration;
import formflow.library.config.NextScreen;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.config.SubflowConfiguration;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.validation.address.AddressValidationService;
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
@RequestMapping("/flow")
public class ScreenController extends FormFlowController {

  private final ValidationService validationService;
  private final AddressValidationService addressValidationService;
  private final ConditionManager conditionManager;
  private final ActionManager actionManager;
  private final FlowConfigurationManager flowConfigurationManager;
  private final ViewModelFactoryService viewModelFactoryService;

  public ScreenController(
      List<FlowConfiguration> flowConfigurations,
      SubmissionRepositoryService submissionRepositoryService,
      ValidationService validationService,
      AddressValidationService addressValidationService,
      ConditionManager conditionManager,
      ActionManager actionManager,
      ViewModelFactoryService viewModelFactoryService) {
    super(submissionRepositoryService, new FlowConfigurationManager(flowConfigurations));
    this.validationService = validationService;
    this.addressValidationService = addressValidationService;
    this.conditionManager = conditionManager;
    this.actionManager = actionManager;
    this.flowConfigurationManager = new FlowConfigurationManager(flowConfigurations);
    this.viewModelFactoryService = viewModelFactoryService;
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
      HttpSession httpSession,
      HttpServletRequest request
  ) {
    log.info("GET getScreen (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), flow, screen);
    var currentScreen = getScreenOr404(flow, screen);
    Submission submission = submissionRepositoryService.findOrCreate(httpSession);

    if ((submission.getUrlParams() != null) && (!submission.getUrlParams().isEmpty())) {
      submission.mergeUrlParamsWithData(query_params);
    } else {
      submission.setUrlParams(query_params);
    }

    submission.setFlow(flow);
    saveToRepository(submission);
    httpSession.setAttribute("id", submission.getId());

    if (uuid != null) {
      actionManager.handleBeforeDisplayAction(currentScreen, submission, uuid);
    } else {
      actionManager.handleBeforeDisplayAction(currentScreen, submission);
    }

    Map<String, Object> model = viewModelFactoryService.build(flow, screen, httpSession, submission, null);

    HashMap<String, SubflowConfiguration> subflows = flowConfigurationManager.getSubflows(flow);
    if (subflows != null) {
      if (subflows.entrySet().stream().anyMatch(subflowConfig ->
          subflowConfig.getValue().getIterationStartScreen().equals(screen))) {
        model.put("formAction", "/flow/%s/%s/new".formatted(flow, screen));
      } else {
        model.put("formAction", "/flow/%s/%s".formatted(flow, screen));
      }
    }

    if (flowConfigurationManager.isDeleteConfirmationScreen(flow, screen)) {
      ModelMap subflowModel = new ModelMap();
      String subflowName = flowConfigurationManager.getSubflows(flow).entrySet().stream()
          .filter(entry -> entry.getValue().getDeleteConfirmationScreen().equals(screen))
          .toList().get(0).getKey();
      ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
      if (subflow == null || subflow.stream().noneMatch(entry -> entry.get("uuid").equals(uuid))) {
        subflowModel.put("noEntryToDelete", true);
        subflowModel.put("reviewScreen", flowConfigurationManager.getSubflows(flow).get(subflowName).getReviewScreen());
        if (subflow == null) {
          subflowModel.put("subflowIsEmpty", true);
          subflowModel.put("entryScreen", flowConfigurationManager.getSubflows(flow).get(subflowName).getEntryScreen());
        }
        return new ModelAndView("%s/%s".formatted(flow, screen), subflowModel);
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
  @PostMapping({"{flow}/{screen}", "{flow}/{screen}/submit"})
  ModelAndView postScreen(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession,
      HttpServletRequest request
  ) throws SmartyException, IOException, InterruptedException {
    log.info("POST postScreen (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), flow, screen);
    var currentScreen = getScreenOr404(flow, screen);
    Submission submission = submissionRepositoryService.findOrCreate(httpSession);
    FormSubmission formSubmission = new FormSubmission(formData);

    actionManager.handleOnPostAction(currentScreen, formSubmission, submission);

    // Field validation
    var errorMessages = validationService.validate(currentScreen, flow, formSubmission);
    handleErrors(httpSession, errorMessages, formSubmission);

    if (!errorMessages.isEmpty()) {
      return new ModelAndView(String.format("redirect:/flow/%s/%s", flow, screen));
    }

    // Address validation
    addressValidationService.validate(submission, formSubmission);

    // handle submit actions, if requested
    if (request.getRequestURI().toLowerCase().contains("submit")) {
      log.info(String.format("Marking the application (%s) as submitted", submission.getId()));
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
      HttpSession httpSession,
      HttpServletRequest request
  ) throws ResponseStatusException {
    log.info("GET getSubflowScreen (url: {}): flow: {}, screen: {}, uuid: {}", request.getRequestURI().toLowerCase(), flow,
        screen, uuid);
    var currentScreen = getScreenOr404(flow, screen);
    Optional<Submission> maybeSubmission = submissionRepositoryService.findById((UUID) httpSession.getAttribute("id"));
    if (maybeSubmission.isEmpty()) {
      log.error("There is no submission associated with request!");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    Submission submission = maybeSubmission.get();
    actionManager.handleBeforeDisplayAction(currentScreen, submission, uuid);
    Map<String, Object> model = viewModelFactoryService.build(flow, screen, httpSession, submission, null);
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
      HttpSession httpSession,
      HttpServletRequest request
  ) throws ResponseStatusException, SmartyException, IOException, InterruptedException {
    log.info("POST updateOrCreateIteration (url: {}): flow: {}, screen: {}, uuid: {}", request.getRequestURI().toLowerCase(),
        flow, screen, uuid);
    FlowConfiguration flowConfiguration = getFlowConfigOr404(flow);
    ScreenNavigationConfiguration currentScreen = getScreenOr404(flow, screen);
    boolean isNewIteration = uuid.equalsIgnoreCase("new");
    String iterationUuid = isNewIteration ? UUID.randomUUID().toString() : uuid;
    FormSubmission formSubmission = new FormSubmission(formData);
    Submission submission = submissionRepositoryService.findOrCreate(httpSession);
    String subflowName = currentScreen.getSubflow();

    actionManager.handleOnPostAction(currentScreen, formSubmission, submission, iterationUuid);

    if (isNewIteration) {
      // handle start iteration page, if new flow
      HashMap<String, SubflowConfiguration> subflows = flowConfigurationManager.getSubflows(flow);
      subflowName = subflows.entrySet().stream()
          .filter(subflow -> subflow.getValue().getIterationStartScreen().equals(screen))
          .map(Entry::getKey)
          .findFirst()
          .orElse(null);
    }

    var errorMessages = validationService.validate(currentScreen, flow, formSubmission);
    handleErrors(httpSession, errorMessages, formSubmission);
    if (!errorMessages.isEmpty()) {
      if (isNewIteration) {
        return new ModelAndView(String.format("redirect:/flow/%s/%s", flow, screen));
      } else {
        return new ModelAndView(String.format("redirect:/flow/%s/%s/%s", flow, screen, iterationUuid));
      }
    }

    addressValidationService.validate(submission, formSubmission);
    NextScreen nextScreen = currentScreen.getNextScreen(submission, iterationUuid, conditionManager);
    if (httpSession.getAttribute("id") != null) {
      // have we submitted any data to the subflow yet?
      if (!submission.getInputData().containsKey(subflowName)) {
        submission.getInputData().put(subflowName, new ArrayList<Map<String, Object>>());
      }
      boolean endOfIteration = currentScreen.isNextScreenInSubflow(flowConfiguration, nextScreen);
      if (isNewIteration) {
        ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
        formSubmission.getFormData().put("uuid", iterationUuid);
        subflow.add(formSubmission.getFormData());

        // setIterationIsComplete() ends up calling conditions, so we should update the subflow information before we call this
        // to make sure anything conditions check have the complete data.
        formSubmission.getFormData().put("iterationIsComplete", !endOfIteration);
      } else {
        var iterationToEdit = submission.getSubflowEntryByUuid(subflowName, iterationUuid);
        if (iterationToEdit != null) {
          submission.mergeFormDataWithSubflowIterationData(subflowName, iterationToEdit, formSubmission.getFormData());

          formSubmission.getFormData().put("iterationIsComplete", !endOfIteration);
          submission.removeIncompleteIterations(subflowName, iterationUuid);
        }
      }
    } else {
      if (isNewIteration) {
        Map<String, Object> inputData = new HashMap<>();
        ArrayList<Map<String, Object>> subflow = new ArrayList<>();

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
            String.format("Session information for subflow iteration id (%s) not set. Did the session expire?", iterationUuid)
        );
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
      }
    }
    actionManager.handleBeforeSaveAction(currentScreen, submission, iterationUuid);

    saveToRepository(submission, subflowName);
    httpSession.setAttribute("id", submission.getId());
    actionManager.handleAfterSaveAction(currentScreen, submission, iterationUuid);
    boolean partOfSubflow = currentScreen.isNextScreenInSubflow(flowConfiguration, nextScreen);

    return new ModelAndView(currentScreen.getNextScreenUrlPath(partOfSubflow, flow, nextScreen.getName(), uuid));
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
    String deleteConfirmationScreen = getFlowConfigOr404(flow).getSubflows().get(subflow).getDeleteConfirmationScreen();
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
      HttpSession httpSession,
      HttpServletRequest request
  ) throws ResponseStatusException {
    log.info("POST deleteSubflowIteration (url: {}): flow: {}, uuid: {}", request.getRequestURI().toLowerCase(), flow, uuid);
    String subflowEntryScreen = getFlowConfigOr404(flow).getSubflows().get(subflow).getEntryScreen();
    UUID id = (UUID) httpSession.getAttribute("id");
    Optional<Submission> submissionOptional = submissionRepositoryService.findById(id);
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    String reviewScreen = flowConfigurationManager.getSubflows(flow).get(subflow)
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
      HttpSession httpSession,
      HttpServletRequest request
  ) {
    log.info("GET navigation (url: {}): flow: {}, screen: {}", request.getRequestURI().toLowerCase(), flow, screen);
    var currentScreen = getScreenOr404(flow, screen);
    String nextScreen = currentScreen.getNextScreen(submissionRepositoryService.findOrCreate(httpSession), null, conditionManager)
        .getName();
    return new ModelAndView(new RedirectView("/flow/%s/%s".formatted(flow, nextScreen)));
  }
}
