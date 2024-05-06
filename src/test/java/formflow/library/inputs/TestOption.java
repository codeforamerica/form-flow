package formflow.library.inputs;

public enum TestOption implements InputOption {
  MANGO("input-options.fruit.mango", "input-options.fruit.mango-desc"),
  PAPAYA("input-options.fruit.papaya", "input-options.fruit.papaya-desc"),
  GUAVA("input-options.fruit.guava", null),
  OTHER("general.other", null);

  public String getLabel() {
    return label;
  }

  private final String label;

  public String getHelpText() {
    return helpText;
  }

  private final String helpText;

  TestOption(String label, String helpText) {
    this.label = label;
    this.helpText = helpText;
  }

  @Override
  public String getValue() {
    return this.name();
  }
}
