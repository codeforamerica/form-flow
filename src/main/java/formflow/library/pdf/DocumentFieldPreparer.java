package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.List;

public interface DocumentFieldPreparer {

  static DocumentFieldType formInputTypeToApplicationInputType(FormInputType type) {
    return switch (type) {
//      case CHECKBOX, PEOPLE_CHECKBOX -> DocumentFieldType.ENUMERATED_MULTI_VALUE;
//      case RADIO, SELECT -> DocumentFieldType.ENUMERATED_SINGLE_VALUE;
//      case DATE -> DocumentFieldType.DATE_VALUE;
//      case TEXT, LONG_TEXT, HOURLY_WAGE, NUMBER, YES_NO, MONEY, TEXTAREA, PHONE, SSN, CUSTOM -> DocumentFieldType.SINGLE_VALUE;
//      case HIDDEN -> DocumentFieldType.UNUSED;
      case SINGLE_VALUE -> DocumentFieldType.SINGLE_VALUE;
      case MULTIVALUE_INPUT -> DocumentFieldType.ENUMERATED_MULTI_VALUE;
    };
  }

  List<DocumentField> prepareDocumentFields(Submission submission);
}
