package formflow.library.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ClammitVirusScannerTest {

  ClammitVirusScanner clammitVirusScanner;
  MockWebServer mockWebServer;

  @BeforeEach
  void setUp() {
    clammitVirusScanner = new ClammitVirusScanner();
    mockWebServer = new MockWebServer();
  }

  @Test
  void virusDetectedReturnsTrueWhenClammitServiceReturns418() throws Exception {
    MultipartFile virusFile = new MockMultipartFile("virus", "virus.jpg",
        MediaType.IMAGE_JPEG_VALUE, "virus".getBytes());
    mockWebServer.enqueue(new MockResponse().setBody("File - has a virus!").setResponseCode(418));

    assertThat(clammitVirusScanner.virusDetected(virusFile)).isTrue();
  }

  @Test
  void virusDetectedReturnsFalseWhenClammitServiceReturnsXXX() {
  }
}