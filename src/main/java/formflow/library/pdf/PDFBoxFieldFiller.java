package formflow.library.pdf;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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
      substituteUnsupportedCharactersWithQuestionMarks(field, pdField);
      log.error(
          "Error setting value '%s' for field %s".formatted(field,
              pdField.getFullyQualifiedName()));
    }
  }

  private void substituteUnsupportedCharactersWithQuestionMarks(String field, PDField pdField) throws IOException {
    StringBuilder builder = new StringBuilder();
    Iterable<COSName> fontNames = pdField.getAcroForm().getDefaultResources().getFontNames();
    for (COSName cosName: fontNames){
      PDType1Font font = (PDType1Font) pdField.getAcroForm().getDefaultResources().getFont(cosName);
      if (builder.isEmpty()){
        for(int i = 0; i < field.length(); i++){
          Character character = field.charAt(i);
          if(font.hasGlyph(character)){
            builder.append(character);
          }else{
            builder.append('?');
          }
        }
      }
      System.out.println("test");
      pdField.setValue(builder.toString());
    }
  }

}
