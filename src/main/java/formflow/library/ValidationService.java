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
  @Value("${form-flow.inputs: 'org.formflowstartertemplate.app.inputs'}")
  private String inputConfigPath;

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
   * @param flowName       The name of the current flow, not null
   * @param formSubmission The input data from a form as a map of field name to field value(s), not null
   * @return a HashMap of field to list of error messages, will be empty if no field violations
   */
  public HashMap<String, ArrayList<String>> validate(ScreenNavigationConfiguration currentScreen, String flowName,
      FormSubmission formSubmission) {
    Class<?> clazz;
    try {
      clazz = Class.forName(inputConfigPath + StringUtils.capitalize(flowName));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    log.info("Running pre validation hook now");
    // run pre validation hook
    currentScreen.handleBeforeValidationAction(formSubmission);

    Class<?> flowClass = clazz;
    HashMap<String, ArrayList<String>> validationMessages = new HashMap<>();
    var formData = formSubmission.getFormData();
    var formDataToBeValidated = formSubmission.removeUnvalidatedInputs(formData);
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
      List<String> requireAnnotations = List.of("javax.validation.constraints.NotNull", "javax.validation.constraints.NotEmpty",
          "javax.validation.constraints.NotBlank");
      if (Collections.disjoint(annotationNames, requireAnnotations) &&
          value.equals("")) {
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
    Map<String, List<String>> errors = currentScreen.handleValidationAction(formSubmission);
    // merge with validation messages

    return validationMessages;
  }
}