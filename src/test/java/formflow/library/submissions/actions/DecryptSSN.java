package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class DecryptSSN implements Action {
  TestEncryptor encryptor;

  public DecryptSSN() {
    encryptor = new TestEncryptor("key-from-property");
  }

  public void run(Submission submission) {
    String ssnEncrypted = (String) submission.getInputData().remove("ssnInputEncrypted");
    if (ssnEncrypted != null) {
      String decrypted = encryptor.decrypt(ssnEncrypted);
      submission.getInputData().put("ssnInput", decrypted);
    }
  }

  public void run(Submission submission, String id) {
    Map<String, Object> subflowEntry = submission.getSubflowEntryByUuid("householdMembers", id);
    if (id.equals(subflowEntry.get("uuid"))) {
      String ssnInput = (String) subflowEntry.remove("ssnInputEncrypted");
      if (ssnInput != null) {
        String decrypted = encryptor.decrypt(ssnInput);
        subflowEntry.put("ssnInput", decrypted);
      }
    }
  }


}
