package formflow.library.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.Loader;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@Component
public class PDFBoxFieldFiller {

//  PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

  public PdfFile fill(String pathToPdfResource, Collection<PdfField> fields) {
    PdfFile tempFile = PdfFile.copyToTempFile(pathToPdfResource);
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
      PDDocument loadedDoc = Loader.loadPDF(pdfResource.getContentAsByteArray());
      PDAcroForm acroForm = loadedDoc.getDocumentCatalog().getAcroForm();
      acroForm.setNeedAppearances(false);
      fillAcroForm(fields, acroForm);
      return loadedDoc;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void fillAcroForm(Collection<PdfField> fields, PDAcroForm acroForm) {
    fields.forEach(field ->
        Optional.ofNullable(acroForm.getField(field.name())).ifPresent(pdField -> {
          String fieldValue = field.value();
          try {
            if (pdField instanceof PDCheckBox && field.value().equals("No")) {
              fieldValue = "Off";
            }
            if (pdField instanceof PDTextField textField) {
              textField.setActions(null);
            }
            pdField.setValue(fieldValue);
          } catch (Exception e) {
            log.error("Error setting value '%s' for field %s"
                    .formatted(fieldValue, pdField.getFullyQualifiedName()));
          }
        }));
  }
}
