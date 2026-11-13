package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import formflow.library.FileController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFileRepositoryService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Verifies that the file-count limit is enforced correctly under concurrent uploads: with only one slot left,
 * two concurrent uploads to the same session must result in exactly one success and one rejection - not both
 * succeeding (overshooting the limit) and not both failing.
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
@TestPropertySource(properties = "form-flow.uploads.max-files=1")
class FileControllerMaxFilesConcurrencyTest {

    @Autowired
    private FileController fileController;

    @Autowired
    private UserFileRepositoryService userFileRepositoryService;

    @Autowired
    private SubmissionRepositoryService submissionRepositoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    }

    @Test
    void concurrentUploadsPastTheFileLimitRejectExactlyOne() throws Exception {
        byte[] realJpegBytes = Files.readAllBytes(Path.of(new ClassPathResource("testA.jpeg").getURI()));
        MockHttpSession session = new MockHttpSession();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<MvcResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    MockMultipartFile file = new MockMultipartFile("file", "image" + index + ".jpg",
                            MediaType.IMAGE_JPEG_VALUE, realJpegBytes);
                    return mockMvc.perform(multipart("/file-upload")
                                    .file(file)
                                    .param("flow", "testFlow")
                                    .param("inputName", "dropZoneTestInstance")
                                    .param("thumbDataURL", "base64string")
                                    .param("screen", "testUploadScreen")
                                    .session(session)
                                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                            .andReturn();
                }));
            }

            startLatch.countDown();

            List<MvcResult> results = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            long successes = results.stream().filter(r -> r.getResponse().getStatus() == 200).count();
            long rejections = results.stream().filter(r -> r.getResponse().getStatus() == 400).count();

            assertThat(successes).as("exactly one of the two concurrent uploads should succeed").isEqualTo(1);
            assertThat(rejections).as("exactly one of the two concurrent uploads should be rejected").isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        // Find the submission via the session and confirm exactly one UserFile row was actually persisted.
        Object submissionMap = session.getAttribute("submissionMap");
        assertThat(submissionMap).isNotNull();
        UUID submissionId = ((java.util.Map<String, UUID>) submissionMap).get("testFlow");
        Submission submission = submissionRepositoryService.findById(submissionId).orElseThrow();
        assertThat(userFileRepositoryService.findAllBySubmission(submission))
                .as("only one file should have actually been persisted for this submission")
                .hasSize(1);
    }
}
