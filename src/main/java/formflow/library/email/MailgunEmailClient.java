package formflow.library.email;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

import static java.util.Collections.emptyList;

@Component
@Slf4j
public class MailgunEmailClient implements EmailClient {

  private final String senderEmail;
  private final String mailgunDomain;
  private MailgunMessagesApi mailgunMessagesApi;

  public MailgunEmailClient(@Value("${form-flow.email-client.mailgun.sender-email:}") String senderEmail,
                            @Value("${form-flow.email-client.mailgun.domain:}") String mailgunDomain,
                            @Value("${form-flow.email-client.mailgun.key:}") String mailgunKey
  ) {
    this.senderEmail = senderEmail;
    this.mailgunDomain = mailgunDomain;
    this.mailgunMessagesApi = MailgunClient.config(mailgunKey)
      .createApi(MailgunMessagesApi.class);
  }

  @Override
  public void sendEmail(
    String subject,
    String recipientEmail,
    String emailBody) {
    sendEmail(
      subject,
      recipientEmail,
      emptyList(),
      emptyList(),
      emailBody,
      emptyList(),
      false);
  }

  @Override
  public void sendEmail(
    String subject,
    String recipientEmail,
    String emailBody,
    List<File> attachments) {
    sendEmail(
      subject,
      recipientEmail,
      emptyList(),
      emptyList(),
      emailBody,
      attachments,
      false);
  }

  @Override
  public void sendEmail(
    String subject,
    String recipientEmail,
    List<String> emailsToCC,
    List<String> emailsToBCC,
    String emailBody,
    List<File> attachments,
    boolean requireTls) {
    Message.MessageBuilder message = Message.builder()
      .from(senderEmail)
      .to(recipientEmail)
      .subject(subject)
      .text(emailBody)
      .requireTls(requireTls);

    if (emailsToCC != null && !emailsToCC.isEmpty()) {
      message.cc(emailsToCC);
    }
    if (emailsToBCC != null && !emailsToBCC.isEmpty()) {
      message.bcc(emailsToBCC);
    }
    if (attachments != null && !attachments.isEmpty()) {
      message.attachment(attachments);
    }
    Message builtMessage = message.build();

    try {
      mailgunMessagesApi.sendMessage(mailgunDomain, builtMessage);
    } catch (FeignException exception) {
      log.error(exception.getMessage());
    }
  }

  public void setMailgunMessagesApi(MailgunMessagesApi mailgunMessageApi) {
    this.mailgunMessagesApi = mailgunMessageApi;
  }
}
