package formflow.library.inputs;

public enum AddressParts {
  STREET_ADDRESS_1("StreetAddress1"),
  STREET_ADDRESS_2("StreetAddress2"),
  CITY("City"),
  STATE("State"),
  ZIPCODE("ZipCode");
  private final String value;

  @Override
  public String toString() {
    return value;
  }

  AddressParts(String value) {
    this.value = value;
  }
}
