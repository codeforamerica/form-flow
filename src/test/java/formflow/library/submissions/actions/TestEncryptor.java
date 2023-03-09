package formflow.library.submissions.actions;

public class TestEncryptor {

  String key;

  public TestEncryptor(String key) {
    this.key = key; // pretend that we do something with this key
  }

  public String decrypt(String input) {
    return input
        .replace('A', '0')
        .replace('B', '1')
        .replace('C', '2')
        .replace('D', '3')
        .replace('E', '4')
        .replace('F', '5');
  }

  public String encrypt(String input) {
    return input
        .replace('0', 'A')
        .replace('1', 'B')
        .replace('2', 'C')
        .replace('3', 'D')
        .replace('4', 'E')
        .replace('5', 'F');
  }
}
