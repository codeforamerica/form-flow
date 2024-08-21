package formflow.library.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileValidationServiceTest {

  @Test
  void acceptedFileExtsShouldReturnTheDefaultIfNoFileTypesAreProvided() {
    FileValidationService fileValidationService = new FileValidationService("", 1);

    assertThat(fileValidationService.getAcceptableFileExts()).containsAll(
        List.of(".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp".split(",")));
  }

  @Test
  void acceptedFileTypesShouldReturnTheIntersectionOfDefaultTypesWithUserProvidedOnes() {
    FileValidationService fileValidationService = new FileValidationService(".bmp,.jpeg,.fake", 1);
    assertThat(fileValidationService.acceptedFileTypes()).isEqualTo(".bmp, .jpeg");
  }

  private static Stream<Arguments> provideMultiPartFiles() {
    return Stream.of(
        Arguments.of("image/jpeg", true),
        Arguments.of("fake/nonsense", false),
        Arguments.of(null, false),
        Arguments.of("", false)
    );
  }

  @ParameterizedTest
  @MethodSource("provideMultiPartFiles")
  void isAcceptedMimeTypeReturnsTrueIfAccepted(String contentType, boolean assertion) throws IOException {
    FileValidationService fileValidationService = new FileValidationService(".jpeg,.bmp", 1);
    MockMultipartFile testFile = new MockMultipartFile("test", "test", contentType, new byte[]{});

    assertThat(fileValidationService.isAcceptedMimeType(testFile)).isEqualTo(assertion);
  }


  @Test
  void detectsFalseMimeTypes() throws IOException {
    ClassPathResource resource = new ClassPathResource("I-am-not-a-png.txt.png");
    MultipartFile file = new MockMultipartFile("file", "I-am-not-a-png.txt.png", MediaType.IMAGE_PNG_VALUE, resource.getInputStream());
    FileValidationService fileValidationService = new FileValidationService(".jpeg,.bmp", 1);
    assertThat(fileValidationService.isAcceptedMimeType(file)).isFalse();
  }
  
  @Test
  void isTooLargeReturnsTrueIfSizeExceedsMax() {
    FileValidationService fileValidationService = new FileValidationService(".jpeg,.bmp", 1);
    byte[] oneMbPlusOne = new byte[1024 * 1024 + 1];
    MockMultipartFile tooBigFile = new MockMultipartFile("file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, oneMbPlusOne);
    assertThat(fileValidationService.isTooLarge(tooBigFile)).isTrue();

    byte[] oneMb = new byte[1024 * 1024];
    MockMultipartFile normalFile = new MockMultipartFile("file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, oneMb);
    assertThat(fileValidationService.isTooLarge(normalFile)).isFalse();
  }

  @Test
  void getFileMaxSizeReturnsMaxFileSize() {
    FileValidationService fileValidationService = new FileValidationService(".jpeg,.bmp", 1);
    assertThat(fileValidationService.getMaxFileSizeInMb()).isEqualTo(1);
  }
}