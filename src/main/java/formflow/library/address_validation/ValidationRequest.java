package formflow.library.address_validation;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationRequest {

  String streetAddress1;
  String streetAddress2;
  String city;
  String state;
  String zipCode;
}
