package formflow.library;

import formflow.library.config.ActionManager;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.data.FormSubmission;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * A service that validates flow inputs based on input definition.
 *
 * <p>
 * Flow inputs come from screen POST submissions to the server.
 * </p>
 * <p>
 * Input definitions are located in <code>formflowstarter/app/inputs/<flow-name></code>.
 * </p>
 */
@Service
@Slf4j
public class ValidationService {

  private final Validator validator;
  private final ActionManager actionManager;
  @Value("${form-flow.inputs: 'org.formflowstartertemplate.app.inputs'}")
  private String inputConfigPath;
  private final List<String> requiredAnnotationsList = List.of(
      NotNull.class.getName(),
      NotEmpty.class.getName(),
      NotBlank.class.getName()
  );

  /**
   * Autoconfigured constructor.
   *
   * @param validator Validator from Jakarta package.
   */
  public ValidationService(Validator validator, ActionManager actionManager) {
    this.validator = validator;
    this.actionManager = actionManager;
  }

  /**
   * Validates client inputs with java bean validation based on input definition.
   *
   * @param currentScreen  The screen we are currently validating form data from
   * @param flowName       The name of the current flow, not null
   * @param formSubmission The input data from a form as a map of field name to field value(s), not null
   * @return a HashMap of field to list of error messages, will be empty if no field violations
   */
  public Map<String, List<String>> validate(ScreenNavigationConfiguration currentScreen, String flowName,
      FormSubmission formSubmission) {

    Map<String, List<String>> crossFieldValidationMessages;
    FormSubmission filteredSubmission = new FormSubmission(formSubmission.getValidatableFields());

    // perform field level validations
    Map<String, List<String>> validationMessages = performFieldLevelValidation(flowName, filteredSubmission);

    // perform cross-field validations, if supplied in action
    crossFieldValidationMessages = actionManager.handleCrossFieldValidationAction(currentScreen, filteredSubmission);

    // combine messages and return them
    validationMessages.putAll(crossFieldValidationMessages);

    return validationMessages;
  }

  private Map<String, List<String>> performFieldLevelValidation(String flowName, FormSubmission formSubmission) {

    Class<?> flowClass;
    HashMap<String, List<String>> validationMessages = new HashMap<>();

    try {
      flowClass = Class.forName(inputConfigPath + StringUtils.capitalize(flowName));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    formSubmission.getFormData().forEach((key, value) -> {
      var messages = new ArrayList<String>();
      List<String> annotationNames = null;

      if (key.contains("[]")) {
        key = key.replace("[]", "");
      }

      try {
        annotationNames = Arrays.stream(flowClass.getDeclaredField(key).getDeclaredAnnotations())
            .map(annotation -> annotation.annotationType().getName()).toList();
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }

      // TODO: this requires explicitly using annotations NotNull, NotEmpty, NotBlank in addition to other constraints. Is that desirable?
      if (Collections.disjoint(annotationNames, requiredAnnotationsList) && value.equals("")) {
        log.info("skipping validation - found empty input for non-required field");
        return;
      }

      validator.validateValue(flowClass, key, value)
          .forEach(violation -> messages.add(violation.getMessage()));

      if (!messages.isEmpty()) {
        validationMessages.put(key, messages);
      }
    });

    return validationMessages;
  }
}
