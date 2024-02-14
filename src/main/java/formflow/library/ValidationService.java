package formflow.library;

import formflow.library.config.ActionManager;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import static formflow.library.inputs.FieldNameMarkers.DYNAMIC_FIELD_MARKER;

/**
 * A service that validates flow inputs based on input definition.
 *
 * <p>
 * Flow inputs come from screen POST submissions to the server.
 * </p>
 * <p>
 * Input definitions are located in {@code formflowstarter/app/inputs/<flow-name>}.
 * </p>
 */
@Service
@Slf4j
public class ValidationService {

  private final Validator validator;
  private final ActionManager actionManager;
  private static String inputConfigPath;
  
  public static List<String> requiredInputs = new ArrayList<>();
  
  private static final List<String> requiredAnnotationsList = List.of(
      NotNull.class.getName(),
      NotEmpty.class.getName(),
      NotBlank.class.getName()
  );

  /**
   * Autoconfigured constructor.
   *
   * @param validator       Validator from Jakarta package.
   * @param actionManager   the <code>ActionManager</code> that manages the logic to be run at specific points
   * @param inputConfigPath the package path where inputs classes are located
   */
  public ValidationService(Validator validator, ActionManager actionManager,
      @Value("${form-flow.inputs: 'formflow.library.inputs.'}") String inputConfigPath) {
    this.validator = validator;
    this.actionManager = actionManager;
    ValidationService.inputConfigPath = inputConfigPath;
  }

  /**
   * Validates client inputs with java bean validation based on input definition.
   *
   * @param currentScreen  The screen we are currently validating form data from
   * @param flowName       The name of the current flow, not null
   * @param formSubmission The input data from a form as a map of field name to field value(s), not null
   * @param submission     The submission that we are cross validating with
   * @return a HashMap of field to list of error messages, will be empty if no field violations
   */
  public Map<String, List<String>> validate(ScreenNavigationConfiguration currentScreen, String flowName,
      FormSubmission formSubmission, Submission submission) {

    Map<String, List<String>> crossFieldValidationMessages;
    FormSubmission filteredSubmission = new FormSubmission(formSubmission.getValidatableFields());

    // perform field level validations
    Map<String, List<String>> validationMessages = performFieldLevelValidation(flowName, filteredSubmission);

    // perform cross-field validations, if supplied in action
    crossFieldValidationMessages = actionManager.handleCrossFieldValidationAction(currentScreen, filteredSubmission, submission);

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
      boolean dynamicField = false;
      var messages = new ArrayList<String>();
      List<String> annotationNames = null;

      if (key.contains("[]")) {
        key = key.replace("[]", "");
      }

      String originalKey = key;

      if (key.contains(DYNAMIC_FIELD_MARKER)) {
        dynamicField = true;
        key = StringUtils.substringBefore(key, DYNAMIC_FIELD_MARKER);
      }

      try {
        annotationNames = Arrays.stream(flowClass.getDeclaredField(key).getDeclaredAnnotations())
            .map(annotation -> annotation.annotationType().getName()).toList();
      } catch (NoSuchFieldException e) {
        if (dynamicField) {
          throw new RuntimeException(
              String.format(
                  "Input field '%s' has dynamic field marker '%s' in its name, but we are unable to " +
                      "find the field in the input file. Is it a dynamic field?",
                  originalKey, DYNAMIC_FIELD_MARKER
              )
          );
        } else {
          throw new RuntimeException(e);
        }
      }

      // if it's acting like a dynamic field, then ensure that it is marked as one
      if (dynamicField) {
        if (!annotationNames.contains("formflow.library.data.annotations.DynamicField")) {
          throw new RuntimeException(
              String.format(
                  "Field name '%s' (field: '%s') acts like it's a dynamic field, but the field does not contain the @DynamicField annotation",
                  key, originalKey
              )
          );
        }
      }

      if (Collections.disjoint(annotationNames, requiredAnnotationsList) && value.equals("")) {
        log.info("skipping validation - found empty input for non-required field");
        return;
      }

      validator.validateValue(flowClass, key, value)
          .forEach(violation -> messages.add(violation.getMessage()));

      if (!messages.isEmpty()) {
        // uses original key to accommodate dynamic input names
        validationMessages.put(originalKey, messages);
      }
    });

    return validationMessages;
  }

  public static List<String> getRequiredInputs(String flowName) {
    if (requiredInputs.isEmpty()) {
      Class<?> flowClass;

      try {
        flowClass = Class.forName(inputConfigPath + StringUtils.capitalize(flowName));
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }

      Field[] declaredFields = flowClass.getDeclaredFields();
      for (Field field : declaredFields) {
        if (Arrays.stream(field.getAnnotations())
            .anyMatch(annotation -> requiredAnnotationsList.contains(annotation.annotationType().getName()))) {
          requiredInputs.add(field.getName());
        }
      }
    }
    return requiredInputs;
  }
}
