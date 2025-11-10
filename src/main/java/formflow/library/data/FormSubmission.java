package formflow.library.data;

import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_CSRF;
import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATED;
import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS;

import formflow.library.address_validation.ValidatedAddress;
import formflow.library.inputs.AddressParts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.util.MultiValueMap;

/**
 * Class representing a submission of a form in the application. This class handles the storage and manipulation of form data
 * including validation and processing of address fields.
 */
@Data
public class FormSubmission {

    /**
     * Form data that is received from the client.
     */
    public Map<String, Object> formData;
    private List<String> unvalidatedFields = List.of(
            UNVALIDATED_FIELD_MARKER_CSRF,
            UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS
    );

    /**
     * Constructor that initializes the form submission with data provided as a MultiValueMap. Empty values are removed and data
     * is flattened for processing.
     *
     * @param formData The form data as a MultiValueMap.
     */
    public FormSubmission(MultiValueMap<String, String> formData) {
        this.formData = removeEmptyValuesAndFlatten(formData);
    }

    /**
     * Constructor that initializes the form submission with a given map of form data.
     *
     * @param formData The form data as a Map.
     */
    public FormSubmission(Map<String, Object> formData) {
        this.formData = formData;
    }

    /**
     * Processes the provided MultiValueMap to remove empty values and flatten the data. This is used to clean up the form data
     * from the client.
     *
     * @param formData The form data as a MultiValueMap.
     * @return A Map with cleaned and flattened form data.
     */
    private Map<String, Object> removeEmptyValuesAndFlatten(MultiValueMap<String, String> formData) {
        return formData.entrySet().stream().peek(entry -> {
                    // An empty checkbox/checkboxSet has a hidden value of "" which needs to be removed
                    if (entry.getKey().contains("[]") && entry.getValue().size() == 1 && entry.getValue().get(0).equals("")) {
                        entry.setValue(new ArrayList<>());
                    }
                    if (entry.getValue().size() > 1 && entry.getValue().get(0).equals("")) {
                        entry.getValue().remove(0);
                    }
                })
                // Flatten arrays to be single values if the array contains one item
                .collect(Collectors.toMap(Entry::getKey,
                        entry -> entry.getValue().size() == 1 && !entry.getKey().contains("[]") ? entry.getValue().get(0)
                                : entry.getValue()));
    }

    /**
     * Retrieves a map of form fields that are eligible for validation. Fields marked as unvalidated are excluded from this map.
     *
     * @return A Map containing only validated address data mapped by field names.
     */
    public Map<String, Object> getValidatableFields() {
        return formData.entrySet().stream().filter(
                        formField -> unvalidatedFields.stream()
                                .noneMatch(unvalidatedField -> formField.getKey().contains(unvalidatedField)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Returns the list of keys that are for addresses that are requested to be validated
     *
     * @return List of Strings representing the validate field name
     */
    public List<String> getAddressValidationFields() {
        return formData.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS))
                .filter(entry -> entry.getValue().toString().equalsIgnoreCase("true"))
                .map(Entry::getKey).toList();
    }

    /**
     * Updates the form data with validated address information. For each address field in the form data, if a validated address
     * is provided, the form data is updated with this validated information.
     *
     * @param validatedAddresses A Map containing validated address data mapped by field names.
     */
    public void setValidatedAddress(Map<String, ValidatedAddress> validatedAddresses) {
        validatedAddresses.forEach((key, value) -> {
            if (value != null) {
                formData.put(key + AddressParts.STREET_ADDRESS_1 + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getStreetAddress());
                formData.put(key + AddressParts.STREET_ADDRESS_2 + UNVALIDATED_FIELD_MARKER_VALIDATED,
                        value.getApartmentNumber());
                formData.put(key + AddressParts.CITY + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getCity());
                formData.put(key + AddressParts.STATE + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getState());
                formData.put(key + AddressParts.ZIPCODE + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getZipCode());
            }
        });
    }
}
