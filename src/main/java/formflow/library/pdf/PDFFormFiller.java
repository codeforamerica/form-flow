package formflow.library.pdf;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Component that generates a filled PDF from a template and a collection of {@link PdfField}
 */
@Slf4j
@Component
public class PDFFormFiller {

  @Value("${form-flow.pdf.fontDirectory:file:/opt/pdf-fonts}")
  private String pdfFontDirectoryPath;

  @Value("${form-flow.pdf.generate-flattened:true}")
  private boolean generateFlattened;
  
  @Value("${form-flow.pdf.read-only:false}")
  private boolean readOnly;

  private final ApplicationContext applicationContext;

  /**
   * Constructor
   * @param applicationContext The Spring {@link ApplicationContext} that provides configuration information
   */
  public PDFFormFiller(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

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
      
      // Populate fields
      for (PdfField pdfField : fields) {
        acroFields.setField(pdfField.name(), pdfField.value());
      }
      
      if (readOnly) {
        // Set all fields in the document to read-only
        Set<String> allFieldNames = acroFields.getAllFields().keySet();
        for (String fieldName : allFieldNames) {
          acroFields.setFieldProperty(fieldName, "setfflags", PdfFormField.FF_READ_ONLY, null);
        }
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
    return fill(pdfTemplatePath, fields, generateFlattened);
  }

  /**
   * Configure the fonts to be used if the default fonts do not support the characters in the values to be filled
   * @param acroFields the `AcroFields` object (the form) in the PDF template
   */
  private void configureSubstitutionFonts(AcroFields acroFields) {
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
  private Set<String> getFontFiles() {
    log.info("Using font directory config: {}", pdfFontDirectoryPath);
    Set<String> emptySet = new HashSet<>();
    Resource resource = applicationContext.getResource(pdfFontDirectoryPath);
    File fontDirectory = null;
    try {
      fontDirectory = resource.getFile();
    } catch (IOException e) {
      log.error("Could not open resource {}, Error: {}", resource, e.getMessage());
    }
    if (fontDirectory == null || !fontDirectory.isDirectory()) {
      return emptySet;
    }
    return Stream.of(fontDirectory.listFiles())
        .filter(file -> !file.isDirectory())
        .map(File::getPath)
        .collect(Collectors.toSet());
  }
}
