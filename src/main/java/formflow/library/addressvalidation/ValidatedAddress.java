package formflow.library.addressvalidation;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a validated address. This class stores information about an address, including street address, apartment number,
 * city, state, and zip code.
 */
@Data
@AllArgsConstructor
public class ValidatedAddress {

    String streetAddress;
    String apartmentNumber;
    String city;
    String state;
    String zipCode;
    /**
     * Default constructor.
     */
    public ValidatedAddress() {
    }

}

