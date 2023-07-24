package formflow.library.validation.address;

import com.smartystreets.api.ClientBuilder;
import com.smartystreets.api.StaticCredentials;
import com.smartystreets.api.us_street.Client;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SmartyClientFactory {

  public SmartyClientFactory() {}

  public Client create(String authId, String authToken, String license) {
    return new ClientBuilder(new StaticCredentials(authId, authToken)).withLicenses(List.of(license)).buildUsStreetApiClient();
  }
}
