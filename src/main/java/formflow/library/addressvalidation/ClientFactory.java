package formflow.library.addressvalidation;

import com.smartystreets.api.ClientBuilder;
import com.smartystreets.api.StaticCredentials;
import com.smartystreets.api.us_street.Client;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Factory component for creating SmartyStreets API clients. This class provides a method to instantiate a new SmartyStreets
 * client with given authentication and license information.
 */
@Component
public class ClientFactory {

    /**
     * Default constructor for ClientFactory.
     */
    public ClientFactory() {
    }

    /**
     * Creates and configures a new SmartyStreets API client. The client is configured with the provided authentication ID, token,
     * and license.
     *
     * @param authId    The authentication ID for the SmartyStreets API.
     * @param authToken The authentication token for the SmartyStreets API.
     * @param license   The license key for using the SmartyStreets API.
     * @return A configured instance of the SmartyStreets Client.
     */
    public Client create(String authId, String authToken, String license) {
        return new ClientBuilder(new StaticCredentials(authId, authToken)).withLicenses(List.of(license))
                .buildUsStreetApiClient();
    }
}
