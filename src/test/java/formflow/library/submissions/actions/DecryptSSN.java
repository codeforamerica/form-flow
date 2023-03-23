package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class DecryptSSN implements Action {

  public void run(Submission submission) {
    String ssnEncrypted = (String) submission.getInputData().remove("ssnInputEncrypted");
    if (ssnEncrypted != null) {
      String decrypted = decrypt(ssnEncrypted);
      submission.getInputData().put("ssnInput", decrypted);
    }
  }

  public void run(Submission submission, String id) {
    Map<String, Object> subflowEntry = submission.getSubflowEntryByUuid("householdMembers", id);
    if (id.equals(subflowEntry.get("uuid"))) {
      String ssnInput = (String) subflowEntry.remove("ssnInputEncrypted");
      if (ssnInput != null) {
        String decrypted = decrypt(ssnInput);
        subflowEntry.put("ssnInput", decrypted);
      }
    }
  }

  private String decrypt(String input) {
    return input
        .replace('A', '0')
        .replace('B', '1')
        .replace('C', '2')
        .replace('D', '3')
        .replace('E', '4')
        .replace('F', '5');
  }
}
