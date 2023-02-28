package formflow.library.address_validation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidatedAddress {

  String streetAddress;
  String apartmentNumber;
  String city;
  String state;
  String zipCode;

}
