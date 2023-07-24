package formflow.library.validation.address;

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
