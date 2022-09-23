package formflow.library.config;

import formflow.library.data.Submission;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

@Data
@Component
public class SubmissionHandler {
  Class<SubmissionActions> submissionActions = SubmissionActions.class;

  public void handleSubmission(String beforeSaveAction, Submission submission, String uuid)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    try {
      submissionActions.getMethod(beforeSaveAction, Submission.class, String.class).invoke(submissionActions, submission, uuid);
    } catch (NoSuchMethodException e) {
      System.out.println("No such method could be found in the SubmissionActions class.");
    } catch (InvocationTargetException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
