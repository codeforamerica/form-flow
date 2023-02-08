package formflow.library.address_validation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidatedAddress {

  String streetAddress1; // primary_number + street_postdirection + street_name + street_suffix  

  String city; // components.city_name
  String state; // components.state_abbreviation
  String zipCode; // components.zipcode + components.plus4_code (if it exists!)

}
