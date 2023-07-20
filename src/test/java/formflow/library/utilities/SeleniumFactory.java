package formflow.library.utilities;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.nio.file.Path;
import java.util.HashMap;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.FactoryBean;

public class SeleniumFactory implements FactoryBean<RemoteWebDriver> {

  private final Path tempdir;
  private RemoteWebDriver driver;

  public SeleniumFactory(Path tempdir) {
    this.tempdir = tempdir;
  }

  @Override
  public RemoteWebDriver getObject() {
    return driver;
  }

  @Override
  public Class<RemoteWebDriver> getObjectType() {
    return RemoteWebDriver.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public void start() {
    // Warning: you may need this line to get a later version of the chromedriver.
    // once you have the version, you don't seem to need to specify it anymore.
    // (or maybe you can upgrade your driver outside of this)
    // WebDriverManager.chromedriver().driverVersion("111.0.5563.64").setup();
    //  WebDriverManager.chromedriver().driverVersion("114.0.5735.90").setup();
    Optional<Path> browserPath = WebDriverManager.chromedriver().getBrowserPath();
    WebDriverManager.chromedriver().setup();
    ChromeOptions options = new ChromeOptions();
    HashMap<String, Object> chromePrefs = new HashMap<>();
    chromePrefs.put("download.default_directory", tempdir.toString());
    options.setBinary(browserPath.get().toFile());
    options.setExperimentalOption("prefs", chromePrefs);
    options.addArguments("--window-size=1280,1600");
    options.addArguments("--headless=new");
    options.addArguments("--remote-allow-origins=*");
    driver = new ChromeDriver(options);
  }

  public void stop() {
    if (driver != null) {
      driver.close();
      driver.quit();
    }
  }
}
