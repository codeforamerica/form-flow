package formflow.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Validator;
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
public class ValidationService {

  private final Validator validator;

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
   * @param formDataSubmission The input data from a form as a map of field name to field value(s),
   *                           not null
   * @return a HashMap of field to list of error messages, will be empty if no field violations
   */
  public HashMap<String, ArrayList<String>> validate(String flowName, Map<String, Object> formDataSubmission) {
    Class<?> clazz;
    try {
      // TODO - figure out how to rewire this, as we will not know the class path.
      clazz = Class.forName(
              "org.codeforamerica.formflowstarter.app.inputs." + StringUtils.capitalize(flowName));
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
      validator.validateValue(flowClass, key, value)
              .forEach(violation -> messages.add(violation.getMessage()));
      if (!messages.isEmpty()) {
        validationMessages.put(key, messages);
      }
    });

    return validationMessages;
  }
}
