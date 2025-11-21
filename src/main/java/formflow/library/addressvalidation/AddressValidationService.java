package formflow.library.addressvalidation;

import formflow.library.data.FormSubmission;
import java.util.Map;

/**
 * Interface for validating addresses contained in form submissions. This interface allows consuming applications
 * to provide custom address validation implementations. If no custom implementation is provided, the library will
 * use the default SmartyStreets implementation.
 *
 * <p>
 * Implementations should validate addresses contained in a FormSubmission object and return a map where each key
 * is an identifier of an address and each value is the validated address. If address validation is disabled or
 * an error occurs, implementations should return an empty map.
 * </p>
 */
public interface AddressValidationService {

    /**
     * Validates addresses contained in a FormSubmission object.
     *
     * @param formSubmission The form submission containing the addresses to be validated.
     * @return A map of identifier strings to ValidatedAddress objects. Returns an empty map if validation is
     *         disabled or an error occurs. Returns null values for addresses that could not be validated.
     */
    Map<String, ValidatedAddress> validate(FormSubmission formSubmission);
}

