package formflow.library.data;

import formflow.library.address_validation.ValidatedAddress;
import formflow.library.inputs.Inputs;
import formflow.library.inputs.UnvalidatedField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.util.MultiValueMap;

@Data
public class FormSubmission {

  public Map<String, Object> formData;
  private List<String> unvalidatedFields = List.of(UnvalidatedField.CSRF, UnvalidatedField.VALIDATE_ADDRESS);

  public FormSubmission(MultiValueMap<String, String> formData) {
    this.formData = removeEmptyValuesAndFlatten(formData);
  }

  public FormSubmission(Map<String, Object> formData) {
    this.formData = formData;
  }

  private Map<String, Object> removeEmptyValuesAndFlatten(MultiValueMap<String, String> formData) {
    return formData.entrySet().stream()
        .peek(entry -> {
          // An empty checkbox/checkboxSet has a hidden value of "" which needs to be removed
          if (entry.getKey().contains("[]") && entry.getValue().size() == 1) {
            entry.setValue(new ArrayList<>());
          }
          if (entry.getValue().size() > 1 && entry.getValue().get(0).equals("")) {
            entry.getValue().remove(0);
          }
        })
        // Flatten arrays to be single values if the array contains one item
        .collect(Collectors.toMap(
            Entry::getKey,
            entry -> entry.getValue().size() == 1 && !entry.getKey().contains("[]")
                ? entry.getValue().get(0) : entry.getValue()
        ));
  }

  public Map<String, Object> removeUnvalidatedInputs(Map<String, Object> formData) {
    return formData.entrySet().stream()
        .filter(
            formField -> unvalidatedFields.stream().noneMatch(unvalidatedField -> formField.getKey().contains(unvalidatedField)))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  public boolean shouldValidateAddress() {
    return formData.entrySet().stream().anyMatch(entry -> entry.getKey().contains(UnvalidatedField.VALIDATE_ADDRESS));
  }

  public void setFormData(Map<String, ValidatedAddress> validatedAddresses) {
    validatedAddresses.forEach((key, value) -> {
      formData.put(key + Inputs.STREET_ADDRESS_1, value.getStreetAddress());
      formData.put(key + Inputs.STREET_ADDRESS_2, "");
      formData.put(key + Inputs.CITY, value.getCity());
      formData.put(key + Inputs.STATE, value.getState());
      formData.put(key + Inputs.ZIPCODE, value.getZipCode());
    });
  }
}
