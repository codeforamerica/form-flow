package formflow.library.pdf;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PDFBoxFieldFiller {

  public PdfFile fill(PdfFile emptyFile, Collection<PdfField> fields)  {
    PdfFile tempFile = PdfFile.copyToTempFile(emptyFile);
    try {
      ByteArrayResource pdfResource = new ByteArrayResource(tempFile.fileBytes());
      PDDocument pdDocument = fillOutPdfs(fields, pdfResource);
      pdDocument.save(tempFile.path());
      pdDocument.close();
    } catch (IOException e) {
      throw new RuntimeException("Cannot read temp file: " + e);
    }

    return tempFile;
  }

  @NotNull
  private PDDocument fillOutPdfs(Collection<PdfField> fields, Resource pdfResource) {
    try {
      PDDocument loadedDoc = PDDocument.load(pdfResource.getInputStream());
      PDAcroForm acroForm = loadedDoc.getDocumentCatalog().getAcroForm();
      acroForm.setNeedAppearances(true);
      fillAcroForm(fields, acroForm);
      return loadedDoc;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void fillAcroForm(Collection<PdfField> fields, PDAcroForm acroForm) {
    fields.forEach(field ->
        Optional.ofNullable(acroForm.getField(field.name())).ifPresent(pdField -> {
          try {
            String fieldValue = field.value();
            if (pdField instanceof PDCheckBox && field.value().equals("No")) {
              fieldValue = "Off";
            }
            setPdfField(fieldValue, pdField);
          } catch (Exception e) {
            throw new RuntimeException("Error setting field: " + field.name(), e);
          }
        }));
  }

  private void setPdfField(String field, PDField pdField)
      throws IOException {
    try {
      if (pdField instanceof PDTextField textField) {
        textField.setActions(null);
      }
      pdField.setValue(field);
    } catch (IllegalArgumentException e) {
      log.error(
          "Error setting value '%s' for field %s".formatted(field,
              pdField.getFullyQualifiedName()));
    }
  }
}
