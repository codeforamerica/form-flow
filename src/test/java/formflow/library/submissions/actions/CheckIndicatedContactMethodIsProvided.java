package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CheckIndicatedContactMethodIsProvided implements Action {

    public Map<String, List<String>> runValidation(FormSubmission formSubmission) {

        Map<String, List<String>> errorMessages = new java.util.HashMap<>(Collections.emptyMap());


        if (formSubmission.formData.containsKey("contactMethod") ){

            if ((formSubmission.formData.get("contactMethod") == "phonePreferred") && (formSubmission.formData.get("phoneNumber") == "")) {
                errorMessages.put("phoneNumber", List.of("please provide a phone number"));
            } else if ((formSubmission.formData.get("contactMethod") == "emailPreferred") && (formSubmission.formData.get("emailAddress") == "")) {
                errorMessages.put("emailAddress", List.of("please provide an email address"));
            }
        }
        return errorMessages;
    }
}
