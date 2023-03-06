package formflow.library;

import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.data.FormSubmission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Validator;
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
  @Value("${form-flow.paths.inputs: 'org.formflowstartertemplate.app.inputs'}")
  private String inputConfigPath;
  private final List<String> requiredAnnotationsList = List.of(
      "javax.validation.constraints.NotNull",
      "javax.validation.constraints.NotEmpty",
      "javax.validation.constraints.NotBlank"
  );

  /**
   * Autoconfigured constructor.
   *
   * @param validator Validator from javax package.
   */
  public ValidationService(Validator validator) {
    this.validator = validator;
  }

  /**
   * Validates client inputs with java bean validation based on input definition.
   *
   * @param currentScreen  The screen we are currently validating form data from
   * @param flowName       The name of the current flow, not null
   * @param formSubmission The input data from a form as a map of field name to field value(s), not null
   * @return a HashMap of field to list of error messages, will be empty if no field violations
   */
  public HashMap<String, List<String>> validate(ScreenNavigationConfiguration currentScreen, String flowName,
      FormSubmission formSubmission) {

    Class<?> flowClass;
    HashMap<String, List<String>> validationMessages = new HashMap<>();
    var formData = formSubmission.getFormData();
    var formDataToBeValidated = formSubmission.removeUnvalidatedInputs(formData);

    try {
      flowClass = Class.forName(inputConfigPath + StringUtils.capitalize(flowName));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    // run pre validation hook
    log.info("Running pre validation hook now");
    currentScreen.handleBeforeValidationAction(formSubmission);

    formDataToBeValidated.forEach((key, value) -> {
      var messages = new ArrayList<String>();

      if (key.contains("[]")) {
        key = key.replace("[]", "");
      }

      List<String> annotationNames = null;
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

    // validation hook for custom actions here
    Map<String, List<String>> validationActionErrors = currentScreen.handleValidationAction(formSubmission);

    if (!validationActionErrors.isEmpty()) {
      validationMessages.putAll(validationActionErrors);
    }

    return validationMessages;
  }
}
