package formflow.library.pdf;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class PDFFormFiller {

  /**
   * Fill the fields in a PDF form with the values provided
   * @param pdfTemplatePath path to the PDF template
   * @param fields collection of field names and values
   * @param flatten whether to flatten the output PDF, preventing its use for further filling
   * @return `File` object for the filled PDF file
   */
  public File fill(String pdfTemplatePath, Collection<PdfField> fields, boolean flatten) {
    try (PdfReader reader = new PdfReader(pdfTemplatePath)) {
      File outputFile = File.createTempFile("Filled_", ".pdf");
      PdfStamper pdfStamper = new PdfStamper(reader, new FileOutputStream(outputFile));
      AcroFields acroFields = pdfStamper.getAcroFields();
      configureSubstitutionFonts(acroFields);
      for (PdfField pdfField : fields) {
        acroFields.setField(pdfField.name(), pdfField.value());
      }
      pdfStamper.setFormFlattening(flatten);
      pdfStamper.close();
      return outputFile;
    } catch (IOException e) {
      log.error("Failed to generate PDF: %s", e);
      throw new RuntimeException(e);
    }
  }

  /**
   *
   * Fill the fields in a PDF form with the values provided and return a flattened PDF
   * @param pdfTemplatePath path to the PDF template
   * @param fields collection of field names and values
   * @return `File` object for the filled PDF file
   */
  public File fill(String pdfTemplatePath, Collection<PdfField> fields) {
    return fill(pdfTemplatePath, fields, true);
  }

  /**
   * Configure the fonts to be used if the default fonts do not support the characters in the values to be filled
   * @param acroFields the `AcroFields` object (the form) in the PDF template
   */
  private static void configureSubstitutionFonts(AcroFields acroFields) {
    Set<String> files = getFontFiles();
    files.forEach(fontResource -> {
      BaseFont font = null;
      try {
        font = BaseFont.createFont(fontResource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
      } catch (IOException e) {
        log.warn("Failed to add substitution font: %s", e);
      }
      acroFields.addSubstitutionFont(font);
    });
  }

  /**
   * Get the list of font files to be used as substitution fonts from resources/pdf-fonts
   * @return Set of filenames with paths
   */
  @NotNull
  private static Set<String> getFontFiles() {
    URI directoryURI = null;
    try {
      directoryURI = new ClassPathResource("pdf-fonts").getURI();
    } catch (IOException e) {
      log.warn("Failed to find pdf-fonts directory: %s", e);
    }

    return Stream.of(Objects.requireNonNull(new File(directoryURI).listFiles()))
            .filter(file -> !file.isDirectory())
            .map(File::getPath)
            .collect(Collectors.toSet());
  }
}