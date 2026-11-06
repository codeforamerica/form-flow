package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.fasterxml.jackson.databind.ObjectMapper;
import formflow.library.FileController;
import formflow.library.utils.UserFileMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Verifies that two concurrent uploads to the same session don't race on the session's {@link UserFileMap}
 * read-modify-write and silently drop one of the two files - the file-upload analogue of the submission-map race
 * fixed by {@link formflow.library.FormFlowControllerConcurrencyTest}.
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
class FileControllerUserFileMapConcurrencyTest {

    private static final int THREAD_COUNT = 5;

    @Autowired
    private FileController fileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Standalone setup (matching FileControllerTest's approach), so this exercises the real, non-mocked
        // FileController/SubmissionRepositoryService/UserFileRepositoryService without also going through
        // Spring Security's filter chain (CSRF, auth), which the file-upload endpoint isn't set up for in a
        // bare @AutoConfigureMockMvc context.
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    }

    @Test
    void concurrentUploadsToTheSameSessionDoNotDropEachOtherFromTheUserFileMap() throws Exception {
        // Needs to be real, validly-encoded JPEG bytes: FileValidationService inspects file content (not just the
        // declared content-type), so fake bytes fail validation before the race is ever exercised.
        byte[] realJpegBytes = Files.readAllBytes(Path.of(new ClassPathResource("testA.jpeg").getURI()));

        MockHttpSession session = new MockHttpSession();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<MvcResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
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

            assertThat(results).allSatisfy(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));
        } finally {
            executor.shutdownNow();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        UserFileMap userFileMap = objectMapper.readValue(session.getAttribute("userFiles").toString(), UserFileMap.class);
        assertThat(userFileMap.getUserFileMap().get("testFlow").get("dropZoneTestInstance"))
                .as("every concurrently-uploaded file should still be present in the session's file map")
                .hasSize(THREAD_COUNT);
    }
}
