package formflow.library.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AcceptedFileTypeServiceTest {

  @Test
  void acceptedFileTypesShouldReturnTheDefaultIfNoFileTypesAreProvided() {
    AcceptedFileTypeService acceptedFileTypeService = new AcceptedFileTypeService("");
    List<String> expected = List.of(".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp".split(","));

    assertThat(acceptedFileTypeService.getAcceptableFileExts()).containsAll(expected);
  }

  @Test
  void acceptedFileTypesShouldReturnTheIntersectionOfDefaultTypesWithUserProvidedOnes() {
    AcceptedFileTypeService acceptedFileTypeService = new AcceptedFileTypeService(".bmp,.jpeg,.fake");
    assertThat(acceptedFileTypeService.acceptedFileTypes()).isEqualTo(".bmp,.jpeg");
  }
}