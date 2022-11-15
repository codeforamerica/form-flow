package formflow.library.data.validators;

import formflow.library.data.UserFile;
import java.util.Objects;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class FileTypeValidator implements ConstraintValidator<CheckFileType, MultipartFile> {

  @Override
  public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
    String fileExtension = Objects.requireNonNull(file.getContentType()).split("/")[1];

    if (UserFile.SUPPORTED_FILE_TYPES.contains("." + fileExtension)) {
      return true;
    } else {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
          "{org.formflow.library.data.validators.CheckFileType.message}"
      ).addConstraintViolation();
      return false;
    }
  }
}
