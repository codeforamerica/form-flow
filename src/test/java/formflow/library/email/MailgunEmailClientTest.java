package formflow.library.email;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;
import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MailgunEmailClientTest extends AbstractMockMvcTest {

    @Autowired
    private MailgunEmailClient mailgunEmailClient;

    private final MailgunMessagesApi mailgunMessagesApi = mock(MailgunMessagesApi.class);

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        mailgunEmailClient.setMailgunMessagesApi(mailgunMessagesApi);
        super.setUp();
    }

    @Test
    public void mailgunWillSendWithMinimumInfo() throws Exception {
        final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        doReturn(null).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

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
        assertThat(builtMessage.getText()).isEqualTo(expectedBody);
        assertThat(builtMessage.getCc()).isNull();
        assertThat(builtMessage.getBcc()).isNull();
        assertThat(builtMessage.getAttachment()).isNull();
        assertThat(builtMessage.getRequireTls()).isEqualTo("no");
    }

    @Test
    public void mailgunWillSendWithDifferentSenderEmail() throws Exception {
        final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        doReturn(null).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

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
        assertThat(builtMessage.getText()).isEqualTo(expectedBody);
        assertThat(builtMessage.getCc()).isNull();
        assertThat(builtMessage.getBcc()).isNull();
        assertThat(builtMessage.getAttachment()).isNull();
        assertThat(builtMessage.getRequireTls()).isEqualTo("no");
    }


    @Test
    public void mailgunWillSendWithMinimumInfoAndAttachments() throws Exception {
        final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        doReturn(null).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

        String expectedSubject = "sendEmail with no CC's";
        String expectedRecipient = "test@example.com";
        String expectedBody = "Email body";
        List<File> expectedAttachments = Arrays.asList(new File("src/test/resources/pdfs/blankPdf.pdf"), new File("src/test/resources/pdfs/testPdf.pdf"));

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
        assertThat(builtMessage.getText()).isEqualTo(expectedBody);
        assertThat(builtMessage.getCc()).isNull();
        assertThat(builtMessage.getBcc()).isNull();
        assertThat(builtMessage.getAttachment().size()).isEqualTo(2);
        assertThat(builtMessage.getRequireTls()).isEqualTo("no");
    }

    @Test
    public void mailgunWillSendAllProvidedInfo() throws Exception {
        final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        doReturn(null).when(mailgunMessagesApi).sendMessage(any(), captor.capture());

        String expectedSubject = "sendEmail with all parameters";
        String expectedRecipient = "test@example.com";
        List<String> expectedEmailsToCC = Arrays.asList("one@test.com", "two@test.com", "three@test.com");
        List<String> expectedEmailsToBCC = Arrays.asList("secret-one@test.com", "secret-two@test.com");
        String expectedBody = "Email body";
        List<File> expectedAttachments = Arrays.asList(new File("src/test/resources/pdfs/blankPdf.pdf"));
        Boolean expectedRequireTls = true;

        mailgunEmailClient.sendEmail(
                expectedSubject,
                expectedRecipient,
                expectedEmailsToCC,
                expectedEmailsToBCC,
                expectedBody,
                expectedAttachments,
                expectedRequireTls
        );

        final Message builtMessage = captor.getValue();
        assertThat(builtMessage).isNotNull();
        assertThat(builtMessage.getSubject()).isEqualTo(expectedSubject);
        assertThat(builtMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
        assertThat(builtMessage.getText()).isEqualTo(expectedBody);
        assertThat(builtMessage.getCc()).isNotNull();
        assertThat(builtMessage.getCc().size()).isEqualTo(3);
        assertThat(builtMessage.getBcc()).isNotNull();
        assertThat(builtMessage.getBcc().size()).isEqualTo(2);
        assertThat(builtMessage.getAttachment().size()).isEqualTo(1);
        assertThat(builtMessage.getRequireTls()).isEqualTo("yes");
    }
}