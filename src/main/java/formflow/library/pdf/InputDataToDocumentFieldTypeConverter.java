package formflow.library.pdf;

import formflow.library.data.Submission;

public class InputDataToDocumentFieldTypeConverter {

  public static FormInputType getInputType(Submission submission, String input) {
    //Verify that input is found
    //If input is found and is one string then it is a single value.
    //If its part of an array then its a multi-value
    //return a string indicating that

    Object inputValue = submission.getInputData().get(input);
    return ((inputValue instanceof String) ? FormInputType.SINGLE_VALUE : FormInputType.MULTIVALUE_INPUT);
  }
}
