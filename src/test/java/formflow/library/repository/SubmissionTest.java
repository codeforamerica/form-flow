package formflow.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.inputs.AddressParts;
import formflow.library.inputs.UnvalidatedField;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class SubmissionTest {

  @Test
  void submittedAddressMatchesValidatedAddress() {
    Submission testSubmission = new Submission();
    HashMap<String, Object> inputData = new HashMap<>();
    inputData.put("testAddress" + AddressParts.STREET_ADDRESS_1.toString(), "123 Main St");
    inputData.put("testAddress" + AddressParts.STREET_ADDRESS_2.toString(), "Apt 1");
    inputData.put("testAddress" + AddressParts.CITY.toString(), "San Francisco");
    inputData.put("testAddress" + AddressParts.STATE.toString(), "CA");
    inputData.put("testAddress" + AddressParts.ZIPCODE.toString(), "94105");
    inputData.put("testAddress" + AddressParts.STREET_ADDRESS_1 + UnvalidatedField.VALIDATED, "123 Main St");
    inputData.put("testAddress" + AddressParts.STREET_ADDRESS_2 + UnvalidatedField.VALIDATED, "Apt 1");
    inputData.put("testAddress" + AddressParts.CITY + UnvalidatedField.VALIDATED, "San Francisco");
    inputData.put("testAddress" + AddressParts.STATE + UnvalidatedField.VALIDATED, "CA");
    inputData.put("testAddress" + AddressParts.ZIPCODE + UnvalidatedField.VALIDATED, "94105");
    testSubmission.setInputData(inputData);

    assertThat(testSubmission.submittedAddressMatchesValidatedAddress("testAddress")).isTrue();
  }
}
