package formflow.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
   * @param flowName           The name of the current flow, not null
   * @param formDataSubmission The input data from a form as a map of field name to field value(s), not null
   * @return a HashMap of field to list of error messages, will be empty if no field violations
   */
  public HashMap<String, ArrayList<String>> validate(String flowName, Map<String, Object> formDataSubmission) {
    Class<?> clazz;
    try {
      clazz = Class.forName(inputConfigPath + StringUtils.capitalize(flowName));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    Class<?> flowClass = clazz;
    HashMap<String, ArrayList<String>> validationMessages = new HashMap<>();
    formDataSubmission.forEach((key, value) -> {
      var messages = new ArrayList<String>();
      if (key.contains("[]")) {
        key = key.replace("[]", "");
      }
      if (!key.equals("_csrf")) {
        List<String> annotationNames = null;
        try {
          annotationNames = Arrays.stream(flowClass.getDeclaredField(key).getDeclaredAnnotations())
              .map(annotation -> annotation.annotationType().getName()).toList();
        } catch (NoSuchFieldException e) {
          throw new RuntimeException(e);
        }
        if (!annotationNames.contains("javax.validation.constraints.NotNull") &&
            !annotationNames.contains("javax.validation.constraints.NotEmpty") &&
            !annotationNames.contains("javax.validation.constraints.NotBlank") &&
            value.equals("")) {
          return;
        }
      }
      validator.validateValue(flowClass, key, value)
          .forEach(violation -> messages.add(violation.getMessage()));
      if (!messages.isEmpty()) {
        validationMessages.put(key, messages);
      }
    });

    return validationMessages;
  }

  public HashMap<String, List<String>> validate(String flowName, String inputName, MultipartFile file) {
    Class<?> clazz;
    try {
      clazz = Class.forName(inputConfigPath + StringUtils.capitalize(flowName));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    Class<?> flowClass = clazz;
    HashMap<String, List<String>> validationMessages = new HashMap<>();
    validator.validateValue(flowClass, inputName, file)
        .forEach(violation -> validationMessages.put(inputName, List.of(violation.getMessage())));

    return validationMessages;
  }
}
