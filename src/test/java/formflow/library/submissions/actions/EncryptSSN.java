package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.Submission;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class EncryptSSN implements Action {

  public void run(Submission submission) {
    String ssnInput = (String) submission.getInputData().remove("ssnInput");
    if (ssnInput != null) {
      String encrypted = encrypt(ssnInput);
      submission.getInputData().put("ssnInputEncrypted", encrypted);
    }
  }

  public void run(Submission submission, String id) {
    Map<String, Object> subflowEntry = submission.getSubflowEntryByUuid("householdMembers", id);
    if (id.equals(subflowEntry.get("uuid"))) {
      String ssnInput = (String) subflowEntry.remove("ssnInput");
      if (ssnInput != null) {
        String encrypted = encrypt(ssnInput);
        subflowEntry.put("ssnInputEncrypted", encrypted);
      }
    }
  }

  private String encrypt(String input) {
    return input
        .replace('0', 'A')
        .replace('1', 'B')
        .replace('2', 'C')
        .replace('3', 'D')
        .replace('4', 'E')
        .replace('5', 'F');
  }
}
