package formflow.library.utils;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.Test;

import java.util.List;

//@TestPropertySource(properties = {"form-flow.uploads.accepted-file-types='.foo,.bar,.jpeg'"})
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
class AcceptedFileTypeUtilTest extends AbstractMockMvcTest {

  @Nested
      // @TestPropertySource(properties = {"form-flow.uploads.accepted-file-types=.bmp,.fake"})
  class AcceptedFileTypeUtilsTestNoConfig {

    @Test
    void acceptedFileTypesShouldReturnTheDefaultIfNoFileTypesAreProvided() {
      AcceptedFileTypeUtils acceptedFileTypeUtils = new AcceptedFileTypeUtils();

      List<String> expected = List.of(".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp".split(","));

      assertThat(acceptedFileTypeUtils.getAcceptableFileExts()).containsAll(expected);
    }

  }

  @Nested
      // @TestPropertySource(properties = {"form-flow.uploads.accepted-file-types=.bmp,.fake"})
  class AcceptedFileTypeUtilsBadValues {

    @Test
    void acceptedFileTypesShouldReturnTheIntersectionOfDefaultTypesWithUserProvidedOnes() {
      AcceptedFileTypeUtils acceptedFileTypeUtils = new AcceptedFileTypeUtils();
      assertThat(acceptedFileTypeUtils.acceptedFileTypes()).isEqualTo(".jpeg");
    }
  }
}