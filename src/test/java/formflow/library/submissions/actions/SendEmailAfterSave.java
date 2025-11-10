package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.Submission;
import formflow.library.email.MailgunEmailClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class SendEmailAfterSave implements Action {

    @Autowired
    private MailgunEmailClient mailgunEmailClient;

    public void run(Submission submission) {
        mailgunEmailClient.sendEmail(
                "Subject",
                "test@example.com",
                "This is a test email"
        );
    }

    public void run(Submission submission, String id) {
        mailgunEmailClient.sendEmail(
                "Subject",
                "test@example.com",
                "This is a test email"
        );
    }
}
