package formflow.library.data;

import formflow.library.address_validation.ValidatedAddress;
import formflow.library.inputs.AddressParts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.util.MultiValueMap;

import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_CSRF;
import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS;
import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATED;

@Data
public class FormSubmission {

  public Map<String, Object> formData;
  private List<String> unvalidatedFields = List.of(
      UNVALIDATED_FIELD_MARKER_CSRF,
      UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS
  );

  public FormSubmission(MultiValueMap<String, String> formData) {
    this.formData = removeEmptyValuesAndFlatten(formData);
  }

  public FormSubmission(Map<String, Object> formData) {
    this.formData = formData;
  }

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

  public Map<String, Object> getValidatableFields() {
    return formData.entrySet().stream().filter(
            formField -> unvalidatedFields.stream().noneMatch(unvalidatedField -> formField.getKey().contains(unvalidatedField)))
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

  public void setValidatedAddress(Map<String, ValidatedAddress> validatedAddresses) {
    validatedAddresses.forEach((key, value) -> {
      if (value != null) {
        formData.put(key + AddressParts.STREET_ADDRESS_1 + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getStreetAddress());
        formData.put(key + AddressParts.STREET_ADDRESS_2 + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getApartmentNumber());
        formData.put(key + AddressParts.CITY + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getCity());
        formData.put(key + AddressParts.STATE + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getState());
        formData.put(key + AddressParts.ZIPCODE + UNVALIDATED_FIELD_MARKER_VALIDATED, value.getZipCode());
      }
    });
  }
}
