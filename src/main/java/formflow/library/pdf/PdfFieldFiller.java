package formflow.library.pdf;

import java.util.Collection;

public interface PdfFieldFiller {

  ApplicationFile fill(Collection<PdfField> fields, String fileName);
}
