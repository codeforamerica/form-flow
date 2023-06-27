package formflow.library.email;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import formflow.library.utilities.AbstractMockMvcTest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

public class MailgunEmailClientTest extends AbstractMockMvcTest {

  @Autowired
  private MailgunEmailClient mailgunEmailClient;

  private final MailgunMessagesApi mailgunMessagesApi = mock(MailgunMessagesApi.class);
  private final MessageResponse messageResponse = mock(MessageResponse.class);

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    mailgunEmailClient.setMailgunMessagesApi(mailgunMessagesApi);
    super.setUp();
  }

  @Test
  public void mailgunWillSendWithMinimumInfo() {
    final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    doReturn(messageResponse).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

    String expectedSubject = "sendEmail with no CC's or attachments";
    String expectedRecipient = "test@example.com";
    String expectedBody = "Email body";

    mailgunEmailClient.sendEmail(
        expectedSubject,
        expectedRecipient,
        expectedBody
    );

    final Message builtMessage = captor.getValue();
    assertThat(builtMessage).isNotNull();
    assertThat(builtMessage.getSubject()).isEqualTo(expectedSubject);
    assertThat(builtMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
    assertThat(builtMessage.getHtml()).isEqualTo(expectedBody);
    assertThat(builtMessage.getCc()).isNull();
    assertThat(builtMessage.getBcc()).isNull();
    assertThat(builtMessage.getAttachment()).isNull();
    assertThat(builtMessage.getRequireTls()).isEqualTo("yes");
  }

  @Test
  public void mailgunWillSendWithDifferentSenderEmail() {
    final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    doReturn(messageResponse).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

    String expectedSubject = "sendEmail with no CC's or attachments";
    String expectedRecipient = "test@example.com";
    String expectedBody = "Email body";
    String expectedSenderEmail = "Another Sender <test@example.org>";

    mailgunEmailClient.setSenderEmail(expectedSenderEmail);
    mailgunEmailClient.sendEmail(
        expectedSubject,
        expectedRecipient,
        expectedBody
    );

    final Message builtMessage = captor.getValue();
    assertThat(builtMessage).isNotNull();
    assertThat(builtMessage.getFrom()).isEqualTo(expectedSenderEmail);
    assertThat(builtMessage.getSubject()).isEqualTo(expectedSubject);
    assertThat(builtMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
    assertThat(builtMessage.getHtml()).isEqualTo(expectedBody);
    assertThat(builtMessage.getCc()).isNull();
    assertThat(builtMessage.getBcc()).isNull();
    assertThat(builtMessage.getAttachment()).isNull();
    assertThat(builtMessage.getRequireTls()).isEqualTo("yes");
  }


  @Test
  public void mailgunWillSendWithMinimumInfoAndAttachments() {
    final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    doReturn(messageResponse).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

    String expectedSubject = "sendEmail with no CC's";
    String expectedRecipient = "test@example.com";
    String expectedBody = "Email body";
    List<File> expectedAttachments = Arrays.asList(new File("src/test/resources/pdfs/blankPdf.pdf"),
        new File("src/test/resources/pdfs/testPdf.pdf"));

    mailgunEmailClient.sendEmail(
        expectedSubject,
        expectedRecipient,
        expectedBody,
        expectedAttachments
    );

    final Message builtMessage = captor.getValue();
    assertThat(builtMessage).isNotNull();
    assertThat(builtMessage.getSubject()).isEqualTo(expectedSubject);
    assertThat(builtMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
    assertThat(builtMessage.getHtml()).isEqualTo(expectedBody);
    assertThat(builtMessage.getCc()).isNull();
    assertThat(builtMessage.getBcc()).isNull();
    assertThat(builtMessage.getAttachment().size()).isEqualTo(2);
    assertThat(builtMessage.getRequireTls()).isEqualTo("yes");
  }

  @Test
  public void mailgunWillSendAllProvidedInfo() {
    final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    doReturn(messageResponse).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

    String expectedSubject = "sendEmail with all parameters";
    String expectedRecipient = "test@example.com";
    List<String> expectedEmailsToCC = Arrays.asList("one@test.com", "two@test.com", "three@test.com");
    List<String> expectedEmailsToBCC = Arrays.asList("secret-one@test.com", "secret-two@test.com");
    String expectedBody = "Email body";
    List<File> expectedAttachments = Arrays.asList(new File("src/test/resources/pdfs/blankPdf.pdf"));

    mailgunEmailClient.sendEmail(
        expectedSubject,
        expectedRecipient,
        expectedEmailsToCC,
        expectedEmailsToBCC,
        expectedBody,
        expectedAttachments
    );

    final Message builtMessage = captor.getValue();
    assertThat(builtMessage).isNotNull();
    assertThat(builtMessage.getSubject()).isEqualTo(expectedSubject);
    assertThat(builtMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
    assertThat(builtMessage.getHtml()).isEqualTo(expectedBody);
    assertThat(builtMessage.getCc()).isNotNull();
    assertThat(builtMessage.getCc().size()).isEqualTo(3);
    assertThat(builtMessage.getBcc()).isNotNull();
    assertThat(builtMessage.getBcc().size()).isEqualTo(2);
    assertThat(builtMessage.getAttachment().size()).isEqualTo(1);
    assertThat(builtMessage.getRequireTls()).isEqualTo("yes");
  }

  @Test
  public void mailgunWillSendWithMinimumInfoAndTlsTurnedOff() {
    //Default requireTls field is set to true and tested below
    final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    doReturn(messageResponse).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

    String expectedSubject = "sendEmail with no CC's or attachments";
    String expectedRecipient = "test@example.com";
    String expectedBody = "Email body";

    mailgunEmailClient.sendEmail(
        expectedSubject,
        expectedRecipient,
        expectedBody
    );

    final Message builtMessage = captor.getValue();
    assertThat(builtMessage).isNotNull();
    assertThat(builtMessage.getSubject()).isEqualTo(expectedSubject);
    assertThat(builtMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
    assertThat(builtMessage.getHtml()).isEqualTo(expectedBody);
    assertThat(builtMessage.getCc()).isNull();
    assertThat(builtMessage.getBcc()).isNull();
    assertThat(builtMessage.getAttachment()).isNull();
    assertThat(builtMessage.getRequireTls()).isEqualTo("yes");

    //requireTls is set to false and set to false on the mailgunEmailClient.
    //The code below checks that the sendEmail method sends an email without requiring Tls.
    Boolean requireTls = false;
    mailgunEmailClient.setRequireTls(requireTls);
    mailgunEmailClient.sendEmail(
        expectedSubject,
        expectedRecipient,
        expectedBody
    );

    final Message builtNoTlsMessage = captor.getValue();
    assertThat(builtNoTlsMessage).isNotNull();
    assertThat(builtNoTlsMessage.getSubject()).isEqualTo(expectedSubject);
    assertThat(builtNoTlsMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
    assertThat(builtNoTlsMessage.getHtml()).isEqualTo(expectedBody);
    assertThat(builtNoTlsMessage.getCc()).isNull();
    assertThat(builtNoTlsMessage.getBcc()).isNull();
    assertThat(builtNoTlsMessage.getAttachment()).isNull();
    assertThat(builtNoTlsMessage.getRequireTls()).isEqualTo("no");

    //returns mailgunEmailClient to its default state.   Otherwise, the mailgunEmailClient maintains the state that requireTls
    //was set to last.
    mailgunEmailClient.setRequireTls(true);
  }
}
