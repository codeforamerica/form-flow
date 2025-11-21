package formflow.library.addressvalidation;

import formflow.library.data.FormSubmission;
import java.util.Map;

/**
 * Interface for address validation services. Implementations of this interface
 * can be provided by applications using this library to customize address validation behavior.
 */
public interface AddressValidationService {
    /**
     * Validates addresses contained in a FormSubmission object. It returns a map where each key is an identifier of an address
     * and each value is the validated address.
     *
     * @param formSubmission The form submission containing the addresses to be validated.
     * @return A map of identifier strings to ValidatedAddress objects.
     */
    Map<String, ValidatedAddress> validate(FormSubmission formSubmission);
}
