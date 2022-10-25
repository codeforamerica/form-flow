package formflow.library;

import formflow.library.config.FlowConfiguration;
import formflow.library.config.NextScreen;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.config.SubflowConfiguration;
import formflow.library.config.TemplateManager;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.upload.FileRepository;
import formflow.library.upload.S3FileRepository;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * A controller to render any screen in flows, including subflows.
 */
@Controller
@EnableAutoConfiguration
@Slf4j
public class ScreenController {

  private final List<FlowConfiguration> flowConfigurations;
  private final SubmissionRepositoryService submissionRepositoryService;
  private final ValidationService validationService;

  private final FileRepository fileRepository;


  public ScreenController(
      List<FlowConfiguration> flowConfigurations,
      SubmissionRepositoryService submissionRepositoryService,
      ValidationService validationService,
      FileRepository fileRepository) {
    this.flowConfigurations = flowConfigurations;
    this.submissionRepositoryService = submissionRepositoryService;
    this.validationService = validationService;
    this.fileRepository = fileRepository;

    log.info("Screen Controller Created!");
    this.flowConfigurations.forEach(f -> {
      log.info("Creating TemplateManager for flow: " + f.getName());
      TemplateManager tm = new TemplateManager(f.getConditionsPath(), f.getActionsPath());
      f.setTemplateManager(tm);
    });
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
      @RequestParam(value = "uuid", required = false) String uuid,
      HttpSession httpSession
  ) {
    log.info(String.format("%s/%s 🚀", flow, screen));
    log.info("getScreen: flow: " + flow + ", screen: " + screen);
    var currentScreen = getScreenConfig(flow, screen);
    var submission = getSubmission(httpSession);
    if (currentScreen == null) {
      return new ModelAndView("redirect:/error");
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
  @PostMapping("{flow}/{screen}")
  ModelAndView postScreen(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession
  ) {
    log.info("postScreen: flow: " + flow + ", screen: " + screen);
    var formDataSubmission = removeEmptyValuesAndFlatten(formData);
    var submission = getSubmission(httpSession);
    var errorMessages = validationService.validate(flow, formDataSubmission);
    handleErrors(httpSession, errorMessages, formDataSubmission);

    if (errorMessages.size() > 0) {
      return new ModelAndView(String.format("redirect:/%s/%s", flow, screen));
    }

    // if there's already a session
    if (submission.getId() != null) {
      submission.mergeFormDataWithSubmissionData(formDataSubmission);
      saveToRepository(submission);
    } else {
      submission.setFlow(flow);
      submission.setInputData(formDataSubmission);
      saveToRepository(submission);
      httpSession.setAttribute("id", submission.getId());
    }

    return new ModelAndView(String.format("redirect:/%s/%s/navigation", flow, screen));
  }

  /**
   * Processes input data from the first page of a subflow screen.
   *
   * <p>
   * If validation of input data passes this will redirect to move the client to the next screen.
   * </p>
   * <p>
   * If validation of input data fails this will redirect the client to the same subflow screen so they can fix the data.
   * </p>
   * <p>
   * A newly created UUID will be stored with the entire subflow data as unique id to reference the data by.
   * </p>
   *
   * @param formData    The input data from current screen, can be null
   * @param flow        The current flow name, not null
   * @param screen      The current screen name in the flow, not null
   * @param httpSession The HTTP session if it exists, can be null
   * @return a redirect to next screen
   */
  @PostMapping("{flow}/{screen}/new")
  ModelAndView postNewSubflow(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      HttpSession httpSession
  ) {
//    Copy from OG /post request
    log.info("postScreen: flow: " + flow + ", screen: " + screen);
    var formDataSubmission = removeEmptyValuesAndFlatten(formData);
    var submission = getSubmission(httpSession);
    var currentScreen = getScreenConfig(flow, screen);
    HashMap<String, SubflowConfiguration> subflows = getFlowConfigurationByName(flow).getSubflows();
    String subflowName = subflows.entrySet().stream().filter(subflow ->
            subflow.getValue().getIterationStartScreen().equals(screen))
        .map(Entry::getKey).findFirst().orElse(null);
    var errorMessages = validationService.validate(flow, formDataSubmission);
    handleErrors(httpSession, errorMessages, formDataSubmission);
    if (errorMessages.size() > 0) {
      return new ModelAndView(String.format("redirect:/%s/%s", flow, screen));
    }

    UUID uuid = UUID.randomUUID();
    formDataSubmission.put("uuid", uuid);

    // if there's already a session
    if (submission.getId() != null) {
      if (!submission.getInputData().containsKey(subflowName)) {
        submission.getInputData().put(subflowName, new ArrayList<Map<String, Object>>());
      }
      ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
      Boolean iterationIsComplete = !isNextScreenInSubflow(flow, httpSession, currentScreen);
      formDataSubmission.put("iterationIsComplete", iterationIsComplete);
      subflow.add(formDataSubmission);
      saveToRepository(submission, subflowName);
    } else {
      submission.setFlow(flow);
      // TODO: create the subflow here and add formDataSubmission to that
      submission.setInputData(formDataSubmission);
      saveToRepository(submission, subflowName);
      httpSession.setAttribute("id", submission.getId());
    }
    String nextScreen = getNextScreenName(httpSession, currentScreen);
    String viewString = isNextScreenInSubflow(flow, httpSession, currentScreen) ?
        String.format("redirect:/%s/%s/%s", flow, nextScreen, uuid) : String.format("redirect:/%s/%s", flow, nextScreen);
    return new ModelAndView(viewString);
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
  // 😭 If we could use a method: <string>.replaceFirst("\\{([^}]*)}", "flow:(?!assets).*")
  // We have to put the regex inline because spring boot must have a compile-time available string
  @GetMapping("{flow:(?!assets).*}/{screen}/{uuid}")
  ModelAndView getSubflowScreen(
      @PathVariable String flow,
      @PathVariable String screen,
      @PathVariable String uuid,
      HttpSession httpSession
  ) {
    Submission submission = getSubmission(httpSession);
    Map<String, Object> model = createModel(flow, screen, httpSession, submission);
    model.put("formAction", String.format("/%s/%s/%s", flow, screen, uuid));
    return new ModelAndView(String.format("%s/%s", flow, screen), model);
  }

  /**
   * Processes input data from a page of a subflow screen that is not the first page of the subflow. The data from the first page
   * of a subflow is processed by {@link #postNewSubflow}, every subsequent page of the subflow is processed by this method.
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
   * @param uuid        Unique id associated with the subflow's data, not null
   * @param httpSession The HTTP session if it exists, not null
   * @return a redirect to next screen
   * @throws InvocationTargetException
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   */
  @PostMapping("{flow:(?!assets).*}/{screen}/{uuid}")
  ModelAndView addToIteration(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      @PathVariable String uuid,
      HttpSession httpSession
  ) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    log.info("addToIteration: flow: " + flow + ", screen: " + screen + ", uuid: " + uuid);
    Long id = (Long) httpSession.getAttribute("id");
    Optional<Submission> submissionOptional = submissionRepositoryService.findById(id);
    Map<String, Object> formDataSubmission = removeEmptyValuesAndFlatten(formData);
    ScreenNavigationConfiguration currentScreen = getScreenConfig(flow, screen);
    String subflowName = currentScreen.getSubflow();
    var errorMessages = validationService.validate(flow, formDataSubmission);
    handleErrors(httpSession, errorMessages, formDataSubmission);
    if (errorMessages.size() > 0) {
      return new ModelAndView(String.format("redirect:/%s/%s/%s", flow, screen, uuid));
    }

    if (submissionOptional.isPresent()) {
      Submission submission = submissionOptional.get();
      var iterationToEdit = submission.getSubflowEntryByUuid(subflowName, uuid);
      if (iterationToEdit != null) {
        Boolean iterationIsComplete = !isNextScreenInSubflow(flow, httpSession, currentScreen);
        formDataSubmission.put("iterationIsComplete", iterationIsComplete);
        submission.mergeFormDataWithSubflowIterationData(subflowName, iterationToEdit, formDataSubmission);
        submission.removeIncompleteIterations(subflowName, uuid);
        handleBeforeSaveAction(currentScreen, submission, uuid);
        saveToRepository(submission, subflowName);
      }
    } else {
      return new ModelAndView("error", HttpStatus.BAD_REQUEST);
    }
    String nextScreen = getNextScreenName(httpSession, currentScreen);
    String viewString = isNextScreenInSubflow(flow, httpSession, currentScreen) ?
        String.format("redirect:/%s/%s/%s", flow, nextScreen, uuid) : String.format("redirect:/%s/%s", flow, nextScreen);
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
  RedirectView deleteConfirmation(
      @PathVariable String flow,
      @PathVariable String subflow,
      @PathVariable String uuid,
      HttpSession httpSession
  ) {
    String deleteConfirmationScreen = getFlowConfigurationByName(flow)
        .getSubflows().get(subflow).getDeleteConfirmationScreen();
    Long id = (Long) httpSession.getAttribute("id");
    if (id == null) {
      // we should throw an error here?
    }
    Optional<Submission> submissionOptional = submissionRepositoryService.findById(id);

    if (submissionOptional.isPresent()) {
      Submission submission = submissionOptional.get();
      var existingInputData = submission.getInputData();
      var subflowArr = (ArrayList<Map<String, Object>>) existingInputData.get(subflow);
      var entryToDelete = subflowArr.stream().filter(entry -> entry.get("uuid").equals(uuid)).findFirst();
      entryToDelete.ifPresent(entry -> httpSession.setAttribute("entryToDelete", entry));
    }

    return new RedirectView(String.format("/%s/" + deleteConfirmationScreen + "?uuid=" + uuid, flow));
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
  ) {
    Long id = (Long) httpSession.getAttribute("id");
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
          return new ModelAndView("redirect:/%s/%s".formatted(flow, subflowEntryScreen));
        }
      } else {
        return new ModelAndView("redirect:/%s/%s".formatted(flow, subflowEntryScreen));
      }
    } else {
      return new ModelAndView("error", HttpStatus.BAD_REQUEST);
    }
    String reviewScreen = getFlowConfigurationByName(flow).getSubflows().get(subflow)
        .getReviewScreen();
    return new ModelAndView(String.format("redirect:/%s/" + reviewScreen, flow));
  }

  /**
   * Returns the template and model for a subflow's screen, filled in with previously supplied values.
   *
   * @param flow        The current flow name, not null
   * @param screen      The current screen name in the flow, not null
   * @param uuid        Unique id associated with the subflow's data, not null
   * @param httpSession The HTTP session if it exists, not null
   * @return the screen template with model data, or error page if data not found
   */
  @GetMapping("{flow}/{screen}/{uuid}/edit")
  ModelAndView getEditScreen(
      @PathVariable String flow,
      @PathVariable String screen,
      @PathVariable String uuid,
      HttpSession httpSession
  ) {
    ScreenNavigationConfiguration currentScreenConfig = getScreenConfig(flow, screen);
    String subflow = currentScreenConfig.getSubflow();
    Long id = (Long) httpSession.getAttribute("id");
    Map<String, Object> model;

    Optional<Submission> submissionOptional = submissionRepositoryService.findById(id);
    if (submissionOptional.isPresent()) {
      Submission submission = submissionOptional.get();
      model = createModel(flow, screen, httpSession, submission);

      var existingInputData = submission.getInputData();
      var subflowArr = (ArrayList<Map<String, Object>>) existingInputData.get(subflow);
      var entryToEdit = subflowArr.stream()
          .filter(entry -> entry.get("uuid").equals(uuid)).findFirst();
      entryToEdit.ifPresent(stringObjectMap -> model.put("inputData", stringObjectMap));
      model.put("formAction", String.format("/%s/%s/%s/edit", flow, screen, uuid));
    } else {
      return new ModelAndView("error", HttpStatus.BAD_REQUEST);
    }

    return new ModelAndView(String.format("%s/%s", flow, screen), model);
  }

  /**
   * Processes input data from a page of a subflow screen.
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
   * @param uuid        Unique id associated with the subflow's data, not null
   * @param httpSession The HTTP session if it exists, not null
   * @return a redirect to next screen
   * @throws InvocationTargetException
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   */
  @PostMapping("{flow}/{screen}/{uuid}/edit")
  ModelAndView edit(
      @RequestParam(required = false) MultiValueMap<String, String> formData,
      @PathVariable String flow,
      @PathVariable String screen,
      @PathVariable String uuid,
      HttpSession httpSession
  ) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    ScreenNavigationConfiguration currentScreen = getScreenConfig(flow, screen);
    String subflowName = currentScreen.getSubflow();
    Long id = (Long) httpSession.getAttribute("id");
    Optional<Submission> submissionOptional = submissionRepositoryService.findById(id);
    Map<String, Object> formDataSubmission = removeEmptyValuesAndFlatten(formData);
    var errorMessages = validationService.validate(flow, formDataSubmission);
    handleErrors(httpSession, errorMessages, formDataSubmission);
    if (errorMessages.size() > 0) {
      return new ModelAndView(String.format("redirect:/%s/%s/%s/edit", flow, screen, uuid));
    }

    if (submissionOptional.isPresent()) {
      Submission submission = submissionOptional.get();
      var iterationToEdit = submission.getSubflowEntryByUuid(subflowName, uuid);
      if (iterationToEdit != null) {
        submission.mergeFormDataWithSubflowIterationData(subflowName, iterationToEdit, formDataSubmission);
        handleBeforeSaveAction(currentScreen, submission, uuid);
        saveToRepository(submission, subflowName);
      }
    } else {
      return new ModelAndView("error", HttpStatus.BAD_REQUEST);
    }
    String nextScreen = getNextScreenName(httpSession, currentScreen);
    String viewString = isNextScreenInSubflow(flow, httpSession, currentScreen) ?
        String.format("redirect:/%s/%s/%s/edit", flow, nextScreen, uuid) : String.format("redirect:/%s/%s", flow, nextScreen);
    return new ModelAndView(viewString);
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
  RedirectView navigation(
      @PathVariable String flow,
      @PathVariable String screen,
      @RequestParam(required = false, defaultValue = "0") Integer option,
      HttpSession httpSession
  ) {
    var currentScreen = getScreenConfig(flow, screen);
    log.info("navigation: flow: " + flow + ", screen: " + screen);
    if (currentScreen == null) {
      return new RedirectView("/error");
    }
    String nextScreen = getNextScreenName(httpSession, currentScreen);

    log.info("navigation: flow: " + flow + ", nextScreen: " + nextScreen);
    return new RedirectView("/%s/%s".formatted(flow, nextScreen));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @PostMapping("/file-upload")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
      @RequestParam("type") String type) throws IOException, InterruptedException {
    try {
      log.info("You are in file upload endpoint");
      log.info("The file name is " + file.getOriginalFilename());
      fileRepository.upload(file);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      log.error("Error Occurred while uploading File " + e.getLocalizedMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String getNextScreenName(HttpSession httpSession,
      ScreenNavigationConfiguration currentScreen) {
    NextScreen nextScreen;

    List<NextScreen> nextScreens = getConditionalNextScreen(currentScreen, httpSession);

    if (isConditionalNavigation(currentScreen) && nextScreens.size() > 0) {
      nextScreen = nextScreens.get(0);
    } else {
      // TODO this needs to throw an error if there are more than 1 next screen that don't have a condition or more than one evaluate to true
      nextScreen = getNonConditionalNextScreen(currentScreen);
    }

    log.info("getNextScreenName: currentScreen:" + currentScreen.toString() + ", nextScreen: " + nextScreen.getName());
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
    return flowConfigurations.stream().filter(
        flowConfiguration -> flowConfiguration.getName().equals(flow)
    ).toList().get(0);
  }

  private Boolean isConditionalNavigation(ScreenNavigationConfiguration currentScreen) {
    return currentScreen.getNextScreens().stream()
        .anyMatch(nextScreen -> nextScreen.getCondition() != null);
  }

  /**
   * Returns a list of possible next screens, the ones whose conditions pass
   *
   * @param currentScreen
   * @param httpSession
   * @return
   */
  private List<NextScreen> getConditionalNextScreen(ScreenNavigationConfiguration currentScreen,
      HttpSession httpSession) {
    var submission = getSubmission(httpSession);

    return currentScreen.getNextScreens().stream()
        .filter(nextScreen -> nextScreen.getCondition() != null)
        .filter(nextScreen -> nextScreen.getConditionObject().run(submission))
        .toList();
  }

  private void handleBeforeSaveAction(ScreenNavigationConfiguration currentScreen, Submission submission, String uuid)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    if (currentScreen.getBeforeSave() != null) {
      currentScreen.getBeforeSaveAction().run(submission, uuid);
    }
  }

  private NextScreen getNonConditionalNextScreen(ScreenNavigationConfiguration currentScreen) {
    return currentScreen.getNextScreens().stream()
        .filter(nxtScreen -> nxtScreen.getCondition() == null).toList().get(0);
  }

  private Submission getSubmission(HttpSession httpSession) {
    var id = (Long) httpSession.getAttribute("id");
    if (id != null) {
      Optional<Submission> submissionOptional = submissionRepositoryService.findById(id);
      return submissionOptional.orElseGet(Submission::new);
    } else {
      return new Submission();
    }
  }

  private Boolean isIterationStartScreen(String flow, String screen) {
    HashMap<String, SubflowConfiguration> subflows = getFlowConfigurationByName(flow).getSubflows();
    if (subflows == null) {
      return false;
    }
    return subflows.entrySet().stream().anyMatch(subflowConfig ->
        subflowConfig.getValue().getIterationStartScreen().equals(screen));
  }

  private Boolean isNextScreenInSubflow(String flow, HttpSession session, ScreenNavigationConfiguration currentScreen) {
    String nextScreenName = getNextScreenName(session, currentScreen);
    return getScreenConfig(flow, nextScreenName).getSubflow() != null;
  }

  private String createFormActionString(String flow, String screen) {
    return isIterationStartScreen(flow, screen) ?
        "/%s/%s/new".formatted(flow, screen) : "/%s/%s".formatted(flow, screen);
  }

  private Map<String, Object> createModel(String flow, String screen, HttpSession httpSession, Submission submission) {
    Map<String, Object> model = new HashMap<>();
    FlowConfiguration flowConfig = getFlowConfigurationByName(flow);
    TemplateManager templateManager = flowConfig.getTemplateManager();
    model.put("flow", flow);
    model.put("screen", screen);

    if (templateManager != null) {
      model.put("templateManager", templateManager);
    }

    // Put subflow on model if on subflow delete confirmation screen
    HashMap<String, SubflowConfiguration> subflows = getFlowConfigurationByName(flow).getSubflows();
    if (subflows != null) {
      List<String> subflowFromDeleteConfirmationConfig = subflows
          .entrySet().stream().filter(entry ->
              entry.getValue().getDeleteConfirmationScreen().equals(screen)).map(Entry::getKey).toList();

      // Add the iteration start page to the model if we are on the review page for a subflow so we have it for the edit button
      subflows.forEach((key, value) -> {
        if (value.getReviewScreen().equals(screen)) {
          model.put("iterationStartScreen", value.getIterationStartScreen());
        }
      });

      if (!subflowFromDeleteConfirmationConfig.isEmpty()) {
        model.put("subflow", subflowFromDeleteConfirmationConfig.get(0));
      }
    }

    // If there are errors, merge form data that was submitted, with already existing inputData
    if (httpSession.getAttribute("formDataSubmission") != null) {
      submission.mergeFormDataWithSubmissionData((Map<String, Object>) httpSession.getAttribute("formDataSubmission"));
    }

    model.put("submission", submission);
    model.put("inputData", submission.getInputData());

    model.put("errorMessages", httpSession.getAttribute("errorMessages"));
    return model;
  }

  private void handleErrors(HttpSession httpSession, HashMap<String, ArrayList<String>> errorMessages,
      Map<String, Object> formDataSubmission) {
    if (errorMessages.size() > 0) {
      httpSession.setAttribute("errorMessages", errorMessages);
      httpSession.setAttribute("formDataSubmission", formDataSubmission);
    } else {
      httpSession.removeAttribute("errorMessages");
      httpSession.removeAttribute("formDataSubmission");
    }
  }

  @NotNull
  private Map<String, Object> removeEmptyValuesAndFlatten(MultiValueMap<String, String> formData) {
    return formData.entrySet().stream()
        .map(entry -> {
          // An empty checkboxSet has a hidden value of "" which needs to be removed
          if (entry.getKey().contains("[]") && entry.getValue().size() == 1) {
            entry.setValue(new ArrayList<>());
          }
          if (entry.getValue().size() > 1 && entry.getValue().get(0).equals("")) {
            entry.getValue().remove(0);
          }
          return entry;
        })
        // Flatten arrays to be single values if the array contains one item
        .collect(Collectors.toMap(
            Entry::getKey,
            entry -> entry.getValue().size() == 1 && !entry.getKey().contains("[]")
                ? entry.getValue().get(0) : entry.getValue()
        ));
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

  private void saveToRepository(Submission submission) {
    submissionRepositoryService.removeFlowCSRF(submission);
    submissionRepositoryService.save(submission);
  }

  private void saveToRepository(Submission submission, String subflowName) {
    submissionRepositoryService.removeFlowCSRF(submission);
    submissionRepositoryService.removeSubflowCSRF(submission, subflowName);
    submissionRepositoryService.save(submission);
  }
}
