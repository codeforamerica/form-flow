package formflow.library.utils;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.upload.AcceptedFileTypeService;
import org.junit.jupiter.api.Test;

class AcceptedFileTypeServiceTest {

  @Test
  void acceptedFileTypesShouldReturnTheDefaultIfNoFileTypesAreProvided() {
    AcceptedFileTypeService acceptedFileTypeService = new AcceptedFileTypeService("");
    assertThat(acceptedFileTypeService.acceptedFileTypes()).isEqualTo(".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp");
  }

  @Test
  void acceptedFileTypesShouldReturnTheIntersectionOfDefaultTypesWithUserProvidedOnes() {
    AcceptedFileTypeService acceptedFileTypeService = new AcceptedFileTypeService(".foo,.bar,.jpeg");
    assertThat(acceptedFileTypeService.acceptedFileTypes()).isEqualTo(".jpeg");
  }
}