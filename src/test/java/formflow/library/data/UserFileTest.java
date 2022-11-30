package formflow.library.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


class UserFileTest {

  @ParameterizedTest
  @CsvSource(value = {
      "image/jpeg",
      "image/jpg",
      "image/png",
      "image/bmp",
      "image/gif"
  })
  void isSupportedImageReturnsTrueForSupportedMimeType(String mimeType) {
    assertThat(UserFile.isSupportedImage(mimeType)).isTrue();
  }

  @ParameterizedTest
  @CsvSource(value = {
      "image/tiff",
      "image/heic",
      "application/pdf",
      "application/msword",
      "application/doc",

  })
  void isSupportedImageReturnsFalseForUnsupportedMimeType(String mimeType) {
    assertThat(UserFile.isSupportedImage(mimeType)).isFalse();
  }
}