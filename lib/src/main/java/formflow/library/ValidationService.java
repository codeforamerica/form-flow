package formflow.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ValidationService {

  private final Validator validator;

  public ValidationService(Validator validator) {
    this.validator = validator;
  }

  public HashMap<String, ArrayList<String>> validate(String flowName, Map<String, Object> formDataSubmission) {
    Class<?> clazz;
    try {
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
