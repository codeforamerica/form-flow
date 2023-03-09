package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.Submission;
import java.util.Map;

@SuppressWarnings("unused")
public class EncryptSSN implements Action {

  TestEncryptor encryptor;

  public EncryptSSN() {
    encryptor = new TestEncryptor("key-from-property");
  }

  public void run(Submission submission) {
    String ssnInput = (String) submission.getInputData().remove("ssnInput");
    if (ssnInput != null) {
      String encrypted = encryptor.encrypt(ssnInput);
      submission.getInputData().put("ssnInputEncrypted", encrypted);
    }
  }

  public void run(Submission submission, String id) {
    Map<String, Object> subflowEntry = submission.getSubflowEntryByUuid("householdMembers", id);
    if (id.equals(subflowEntry.get("uuid"))) {
      String ssnInput = (String) subflowEntry.remove("ssnInput");
      if (ssnInput != null) {
        String encrypted = encryptor.encrypt(ssnInput);
        subflowEntry.put("ssnInputEncrypted", encrypted);
      }
    }
  }

}
