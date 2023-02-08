package formflow.library.address_validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.smartystreets.api.exceptions.BatchFullException;
import com.smartystreets.api.us_street.Batch;
import com.smartystreets.api.us_street.Lookup;
import formflow.library.data.FormSubmission;
import formflow.library.inputs.UnvalidatedField;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidationRequestFactoryTest {

  ValidationRequestFactory validationRequestFactory = new ValidationRequestFactory();

  @Test
  void shouldCreateBatchFromFormSubmission() throws BatchFullException {
    FormSubmission formSubmission = new FormSubmission(Map.ofEntries(
        Map.entry("testAddressStreetAddress1", "123 Main St"),
        Map.entry("testAddressStreetAddress2", "Apt 1"),
        Map.entry("testAddressCity", "San Francisco"),
        Map.entry("testAddressState", "CA"),
        Map.entry("testAddressZipCode", "94105"),
        Map.entry("otherAddressStreetAddress1", "1254 Fake St"),
        Map.entry("otherAddressStreetAddress2", "Apt 2"),
        Map.entry("otherAddressCity", "San Mateo"),
        Map.entry("otherAddressState", "CA"),
        Map.entry("otherAddressZipCode", "94002"),
        Map.entry(UnvalidatedField.VALIDATE_ADDRESS + "testAddress", "true"),
        Map.entry(UnvalidatedField.VALIDATE_ADDRESS + "otherAddress", "true")
    ));

    Lookup lookup1 = new Lookup();
    lookup1.setStreet("123 Main St");
    lookup1.setStreet2("Apt 1");
    lookup1.setCity("San Francisco");
    lookup1.setState("CA");
    lookup1.setZipCode("94105");

    Lookup lookup2 = new Lookup();
    lookup2.setStreet("1254 Fake St");
    lookup2.setStreet2("Apt 2");
    lookup2.setCity("San Mateo");
    lookup2.setState("CA");
    lookup2.setZipCode("94002");

    Batch batch = validationRequestFactory.create(formSubmission);
    Batch testBatch = new Batch();
    testBatch.add(lookup1);
    testBatch.add(lookup2);

    assertThat(batch.get("testAddress").getStreet()).isEqualTo(lookup1.getStreet());
    assertThat(batch.get("testAddress").getStreet2()).isEqualTo(lookup1.getStreet2());
    assertThat(batch.get("testAddress").getCity()).isEqualTo(lookup1.getCity());
    assertThat(batch.get("testAddress").getState()).isEqualTo(lookup1.getState());
    assertThat(batch.get("testAddress").getZipCode()).isEqualTo(lookup1.getZipCode());

    assertThat(batch.get("otherAddress").getStreet()).isEqualTo(lookup2.getStreet());
    assertThat(batch.get("otherAddress").getStreet2()).isEqualTo(lookup2.getStreet2());
    assertThat(batch.get("otherAddress").getCity()).isEqualTo(lookup2.getCity());
    assertThat(batch.get("otherAddress").getState()).isEqualTo(lookup2.getState());
    assertThat(batch.get("otherAddress").getZipCode()).isEqualTo(lookup2.getZipCode());
  }
}