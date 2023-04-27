package formflow.library.pdf;

public record ApplicationFile(byte[] fileBytes, String fileName) {

  @Override
  public String toString() {
    return fileName;
  }
}
