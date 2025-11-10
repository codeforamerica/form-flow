package formflow.library.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static Stream<Arguments> provideMultiPartFiles() {
        return Stream.of(
                Arguments.of("test.jpeg", true),
                Arguments.of("test-archive.zip", false),
                Arguments.of("i-am-not-a-png.txt.png", false)
        );
    }

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

    @ParameterizedTest
    @MethodSource("provideMultiPartFiles")
    void isAcceptedMimeTypeReturnsTrueIfAccepted(String contentName, boolean assertion) throws IOException {
        FileValidationService fileValidationService = new FileValidationService(".jpeg,.bmp", 1);
        ClassPathResource resource = new ClassPathResource(contentName);
        MultipartFile file = new MockMultipartFile("file", contentName, Files.probeContentType(Path.of(contentName)),
                resource.getInputStream());

        assertThat(fileValidationService.isAcceptedMimeType(file)).isEqualTo(assertion);
    }


    @Test
    void detectsFalseMimeTypes() throws IOException {
        ClassPathResource resource = new ClassPathResource("i-am-not-a-png.txt.png");
        MultipartFile file = new MockMultipartFile("file", "i-am-not-a-png.txt.png", MediaType.IMAGE_PNG_VALUE,
                resource.getInputStream());
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