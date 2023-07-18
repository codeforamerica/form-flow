package formflow.library.utils;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.Test;

import java.util.List;

@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.uploads.accepted-file-types='.foo,.bar,.jpeg'"})
class AcceptedFileTypeUtilTest extends AbstractMockMvcTest {

  @Nested
  class AcceptedFileTypeUtilsTestNoConfig {

    @Test
    void acceptedFileTypesShouldReturnTheDefaultIfNoFileTypesAreProvided() {
      List<String> resultList = List.of(AcceptedFileTypeUtils.acceptedFileTypes().split(","));
      List<String> expected = List.of(".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp".split( ","));

      assertThat(resultList.containsAll(expected));
    }

  }

  @Nested
  class AcceptedFileTypeUtilsBadValues {
    @Test
    void acceptedFileTypesShouldReturnTheIntersectionOfDefaultTypesWithUserProvidedOnes() {
      assertThat(AcceptedFileTypeUtils.acceptedFileTypes()).isEqualTo(".jpeg");
    }
  }
}