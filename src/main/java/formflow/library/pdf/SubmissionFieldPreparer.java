package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.List;

public interface SubmissionFieldPreparer {

  static SubmissionFieldValue formInputTypeToApplicationInputType(FormInputType type) {
    return switch (type) {
//      case CHECKBOX, PEOPLE_CHECKBOX -> DocumentFieldType.ENUMERATED_MULTI_VALUE;
//      case RADIO, SELECT -> DocumentFieldType.ENUMERATED_SINGLE_VALUE;
//      case DATE -> DocumentFieldType.DATE_VALUE;
//      case TEXT, LONG_TEXT, HOURLY_WAGE, NUMBER, YES_NO, MONEY, TEXTAREA, PHONE, SSN, CUSTOM -> DocumentFieldType.SINGLE_VALUE;
//      case HIDDEN -> DocumentFieldType.UNUSED;
      case SINGLE_VALUE -> SubmissionFieldValue.SINGLE_FIELD;
      case MULTIVALUE_INPUT -> SubmissionFieldValue.CHECKBOX;
    };
  }

  List<SubmissionField> prepareSubmissionFields(Submission submission);
}
