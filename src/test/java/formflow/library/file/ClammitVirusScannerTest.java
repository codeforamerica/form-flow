package formflow.library.file;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class ClammitVirusScannerTest {

    ClammitVirusScanner clammitVirusScanner;
    MockWebServer mockWebServer;
    int timeout = 2000;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        HttpUrl clammitUrl = mockWebServer.url("/scan");
        clammitVirusScanner = new ClammitVirusScanner(clammitUrl.toString(), timeout);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void virusDetectedReturnsTrueWhenClammitServiceReturns418() throws Exception {
        MultipartFile virusFile = new MockMultipartFile("virus", "virus.jpg", IMAGE_JPEG_VALUE, "virus".getBytes());
        mockWebServer.enqueue(
                new MockResponse().setBody("File - has a virus!").setResponseCode(HttpStatus.I_AM_A_TEAPOT.value()));

        assertThat(clammitVirusScanner.virusDetected(virusFile)).isTrue();
    }

    @Test
    void virusDetectedReturnsFalseWhenClammitServiceReturns200() throws Exception {
        MultipartFile cleanFile = new MockMultipartFile("file", "file.jpg", IMAGE_JPEG_VALUE, "file".getBytes());
        mockWebServer.enqueue(new MockResponse().setBody("No virus here!").setResponseCode(HttpStatus.OK.value()));

        assertThat(clammitVirusScanner.virusDetected(cleanFile)).isFalse();
    }

    @Test
    void virusDetectedThrowsExceptionWhenRequestTimesOut() {
        MultipartFile cleanFile = new MockMultipartFile("file", "file.jpg", IMAGE_JPEG_VALUE, "file".getBytes());
        mockWebServer.enqueue(new MockResponse().setBodyDelay(timeout + 1, MILLISECONDS).setBody("Timed out"));

        assertThatExceptionOfType(TimeoutException.class).isThrownBy(() -> clammitVirusScanner.virusDetected(cleanFile));
    }

    @Test
    void virusDetectedThrowsExceptionWhenUnexpectedResponseOccurs() {
        MultipartFile cleanFile = new MockMultipartFile("file", "file.jpg", IMAGE_JPEG_VALUE, "file".getBytes());
        mockWebServer.enqueue(new MockResponse().setBody("Something weird happened!")
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        assertThatExceptionOfType(WebClientResponseException.class).isThrownBy(
                () -> clammitVirusScanner.virusDetected(cleanFile));
    }
}