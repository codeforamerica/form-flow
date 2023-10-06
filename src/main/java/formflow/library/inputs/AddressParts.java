package formflow.library.inputs;

/**
 * Fields used to define an address fragment
 */
public enum AddressParts {
  /**
   * Street Portion of an address
   */
  STREET_ADDRESS_1("StreetAddress1"),
  /**
   * Apartment, suite, or office number
   */
  STREET_ADDRESS_2("StreetAddress2"),
  /**
   * City name
   */
  CITY("City"),
  /**
   * The State value stored as the state code, a two character String, i.e. "IL", "CA", etc.
   */
  STATE("State"),
  /**
   * Five or nine digit value used by the post office to facilitate the delivery of mail.
   */
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
