package formflow.library.upload;

public enum FileType {
  TXT("txt", "text/plain"),
  PDF("pdf", "application/pdf"),
  JPG("jpg", "image/jpeg"),
  PNG("png", "image/png"),
  DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");


  //.jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp
  //.jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp
  //.jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp

  private final String extension;
  private final String mimeType;

  FileType(String extension, String mimeType) {
    this.extension = extension;
    this.mimeType = mimeType;
  }
}
