package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.Map;

public class TestCustomPreparer implements SubmissionFieldPreparer {

    @Override
    public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap) {
        HashMap<String, SubmissionField> testFieldMap = new HashMap<>();
        testFieldMap.put("fieldThatGetsOverwritten", new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null));
        return testFieldMap;
    }
}
