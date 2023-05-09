package formflow.library.email;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MailgunEmailClientTest extends AbstractMockMvcTest {

  private final MailgunEmailClient mailgunEmailClient = mock(MailgunEmailClient.class);

  MailgunMessagesApi mailgunMessagesApi = mock(MailgunMessagesApi.class);

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void mailgunWillSendWithMinimumInfo() throws Exception {
    final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
//    when(mailgunMessagesApi.sendMessage(any(), any())).thenAnswer(i -> i.getArguments()[0]);


//    doReturn(1).when(bloMock).doSomeStuff();
//    doReturn(null).when(mailgunMessagesApi).sendMessage(any(), captor.capture());
    mailgunEmailClient.sendEmail(
      "Subject \uD83D\uDD25",
      "cborg@codeforamerica.org",
      "This is a test \uD83D\uDC38"
    );
    verify(mailgunMessagesApi).sendMessage(any(), captor.capture());
    final Message builtMessage = captor.getValue();

    assertThat(builtMessage).isNotNull();
//    verify(mailgunMessagesApi, times(1)).sendMessage(any(), argThat(builtMessage -> {
//      assertThat(builtMessage).isNull();
//      return true;
//    }));
  }
}
