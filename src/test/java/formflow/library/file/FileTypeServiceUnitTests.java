package formflow.library.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

class FileTypeServiceUnitTests {

  @Test
  void acceptedFileExtsShouldReturnTheDefaultIfNoFileTypesAreProvided() {
    FileTypeService fileTypeService = new FileTypeService("");

    assertThat(fileTypeService.getAcceptableFileExts()).containsAll(
        List.of(".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp".split(",")));
  }

  @Test
  void acceptedFileTypesShouldReturnTheIntersectionOfDefaultTypesWithUserProvidedOnes() {
    FileTypeService fileTypeService = new FileTypeService(".bmp,.jpeg,.fake");
    assertThat(fileTypeService.acceptedFileTypes()).isEqualTo(".bmp, .jpeg");
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
  void isAcceptedMimeTypeReturnsTrueIfAccepted(String contentType, boolean assertion) {
    FileTypeService fileTypeService = new FileTypeService(".jpeg,.bmp");
    MockMultipartFile testFile = new MockMultipartFile("test", "test", contentType, new byte[]{});

    assertThat(fileTypeService.isAcceptedMimeType(testFile)).isEqualTo(assertion);
  }
}