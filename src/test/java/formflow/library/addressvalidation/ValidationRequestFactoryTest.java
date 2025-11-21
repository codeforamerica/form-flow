package formflow.library.addressvalidation;

import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.smartystreets.api.us_street.Batch;
import formflow.library.data.FormSubmission;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidationRequestFactoryTest {

    ValidationRequestFactory validationRequestFactory = new ValidationRequestFactory();

    @Test
    void shouldCreateBatchFromFormSubmission() {
        String firstStreetAddress1 = "123 Main St";
        String firstStreetAddress2 = "Apt 1";
        String firstCity = "San Francisco";
        String firstState = "CA";
        String firstZipcode = "94105";
        String secondStreetAddress1 = "1254 Fake St";
        String secondStreetAddress2 = "Apt 2";
        String secondCity = "San Mateo";
        String secondState = "NV";
        String secondZipcode = "94002";

        FormSubmission formSubmission = new FormSubmission(Map.ofEntries(
                Map.entry("testAddressStreetAddress1", firstStreetAddress1),
                Map.entry("testAddressStreetAddress2", firstStreetAddress2),
                Map.entry("testAddressCity", firstCity),
                Map.entry("testAddressState", firstState),
                Map.entry("testAddressZipCode", firstZipcode),
                Map.entry("otherAddressStreetAddress1", secondStreetAddress1),
                Map.entry("otherAddressStreetAddress2", secondStreetAddress2),
                Map.entry("otherAddressCity", secondCity),
                Map.entry("otherAddressState", secondState),
                Map.entry("otherAddressZipCode", secondZipcode),
                Map.entry(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS + "testAddress", "true"),
                Map.entry(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS + "otherAddress", "true")
        ));

        Batch batch = validationRequestFactory.create(formSubmission);

        assertThat(batch.get("testAddress").getStreet()).isEqualTo(firstStreetAddress1);
        assertThat(batch.get("testAddress").getStreet2()).isEqualTo(firstStreetAddress2);
        assertThat(batch.get("testAddress").getCity()).isEqualTo(firstCity);
        assertThat(batch.get("testAddress").getState()).isEqualTo(firstState);
        assertThat(batch.get("testAddress").getZipCode()).isEqualTo(firstZipcode);

        assertThat(batch.get("otherAddress").getStreet()).isEqualTo(secondStreetAddress1);
        assertThat(batch.get("otherAddress").getStreet2()).isEqualTo(secondStreetAddress2);
        assertThat(batch.get("otherAddress").getCity()).isEqualTo(secondCity);
        assertThat(batch.get("otherAddress").getState()).isEqualTo(secondState);
        assertThat(batch.get("otherAddress").getZipCode()).isEqualTo(secondZipcode);
    }
}

