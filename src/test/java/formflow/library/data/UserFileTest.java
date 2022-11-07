package formflow.library.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


class UserFileTest {

  @Test
  void calculateFilesizeInMbShouldReturnFileSizeInMegabytes() {
  }

  @ParameterizedTest
  @MethodSource(supportedImageTypes)
  void isSupportedImageReturnsTrueForSupportedMimeType() {
    assertThat(UserFile.isSupportedImage("image/png")).isTrue();
    assertThat(UserFile.isSupportedImage("image/tiff")).isFalse();
  }
}