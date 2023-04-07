package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class CheckIndicatedContactMethodIsProvided implements Action {

  @Autowired
  MessageSource messageSource;

  public Map<String, List<String>> runValidation(FormSubmission formSubmission) {

    String HOW_TO_CONTACT_YOU_INPUT = "contactMethod[]";
    String PHONE_NUMBER_INPUT = "phoneNumber";
    String EMAIL_INPUT = "email";

    Map<String, List<String>> errorMessages = new java.util.HashMap<>(Collections.emptyMap());

    if (formSubmission.getFormData().containsKey(HOW_TO_CONTACT_YOU_INPUT)) {
      ArrayList<String> preferredContactMethods = (ArrayList<String>) formSubmission.getFormData().get(HOW_TO_CONTACT_YOU_INPUT);
      if (preferredContactMethods.contains("phonePreferred") && formSubmission.getFormData().get(PHONE_NUMBER_INPUT).equals("")) {
        errorMessages.put(PHONE_NUMBER_INPUT,
            List.of("You indicated you would like to be contacted by phone. Please make sure to provide a phone number."));
      }
      if (preferredContactMethods.contains("emailPreferred") && formSubmission.getFormData().get("email").equals("")) {
        errorMessages.put(EMAIL_INPUT,
            List.of("You indicated you would like to be contacted by email. Please make sure to provide an email address."));
      }
    }
    return errorMessages;
  }
}
