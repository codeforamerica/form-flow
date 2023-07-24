package formflow.library.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FileServiceUnitTests {

  @Test
  void acceptedFileTypesShouldReturnTheDefaultIfNoFileTypesAreProvided() {
    FileService fileService = new FileService("");
    List<String> expected = List.of(".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp".split(","));

    assertThat(fileService.getAcceptableFileExts()).containsAll(expected);
  }

  @Test
  void acceptedFileTypesShouldReturnTheIntersectionOfDefaultTypesWithUserProvidedOnes() {
    FileService fileService = new FileService(".bmp,.jpeg,.fake");
    assertThat(fileService.acceptedFileTypes()).isEqualTo(".bmp,.jpeg");
  }
}