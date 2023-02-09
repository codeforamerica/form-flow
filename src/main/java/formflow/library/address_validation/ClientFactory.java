package formflow.library.address_validation;

import com.smartystreets.api.ClientBuilder;
import com.smartystreets.api.StaticCredentials;
import com.smartystreets.api.us_street.Client;
import java.util.List;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ClientFactory {

  public ClientFactory() {}

  public Client create(String authId, String authToken, String license) {
    return new ClientBuilder(new StaticCredentials(authId, authToken)).withLicenses(List.of(license)).buildUsStreetApiClient();
  }
}
