package formflow.library.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;
import formflow.library.ScreenController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.email.MailgunEmailClient;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-after-save-action.yaml"})
public class AfterSaveActionTest extends AbstractMockMvcTest {

  Submission submission;

  @MockitoBean
  private SubmissionRepositoryService submissionRepositoryService;

  @Autowired
  private ScreenController screenController;

  @Autowired
  private MailgunEmailClient mailgunEmailClient;

  private final MailgunMessagesApi mailgunMessagesApi = mock(MailgunMessagesApi.class);

  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    mockMvc = MockMvcBuilders.standaloneSetup(screenController).build();
    submission = Submission.builder().id(submissionUUID).inputData(new HashMap<>()).build();
    mailgunEmailClient.setMailgunMessagesApi(mailgunMessagesApi);

    super.setUp();
    ////when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
  }

  @Test
  void shouldSendEmailAfterSave() throws Exception {
    final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    doReturn(null).when(mailgunMessagesApi).sendMessage(any(), captor.capture());
    String expectedSubject = "Subject";
    String expectedRecipient = "test@example.com";
    String expectedBody = "This is a test email";

    postWithoutData("inputs").andExpect(redirectedUrl(getUrlForPageName("inputs") + "/navigation"));

    verify(mailgunMessagesApi, times(1)).sendMessage(any(), any());

    final Message builtMessage = captor.getValue();
    AssertionsForClassTypes.assertThat(builtMessage).isNotNull();
    AssertionsForClassTypes.assertThat(builtMessage.getSubject()).isEqualTo(expectedSubject);
    AssertionsForClassTypes.assertThat(builtMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
    AssertionsForClassTypes.assertThat(builtMessage.getHtml()).isEqualTo(expectedBody);
  }

  @Test
  void shouldSendEmailAfterSaveInSubflow() throws Exception {
    final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    doReturn(null).when(mailgunMessagesApi).sendMessage(any(), captor.capture());
    String expectedSubject = "Subject";
    String expectedRecipient = "test@example.com";
    String expectedBody = "This is a test email";

    ResultActions test = postWithoutData("subflowIterationStart/new");
    String url = test.andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getRedirectedUrl();
    assertThat(url).isNotNull();
    assertThat(url.contains("next"));

    verify(mailgunMessagesApi, times(1)).sendMessage(any(), any());

    final Message builtMessage = captor.getValue();
    AssertionsForClassTypes.assertThat(builtMessage).isNotNull();
    AssertionsForClassTypes.assertThat(builtMessage.getSubject()).isEqualTo(expectedSubject);
    AssertionsForClassTypes.assertThat(builtMessage.getTo().contains(expectedRecipient)).isEqualTo(true);
    AssertionsForClassTypes.assertThat(builtMessage.getHtml()).isEqualTo(expectedBody);
  }
}
