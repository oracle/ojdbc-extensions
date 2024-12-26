package oracle.jdbc.provider.hashicorp.dedicated.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.hashicorp.dedicated.configuration.DedicatedVaultTestProperty;
import oracle.jdbc.spi.OracleConfigurationJsonSecretProvider;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the Dedicated Vault Configuration Provider.
 */
public class DedicatedVaultConfigurationProviderTest {

  static {
    OracleConfigurationProvider.allowedProviders.add("hashicorpvault");
  }


  private static final OracleConfigurationProvider PROVIDER =
          OracleConfigurationProvider.find("hashicorpvault");


  /**
   * Verifies if Dedicated Vault Configuration Provider works with TOKEN-based authentication and a specific secret name.
   *
   * @throws SQLException if the provider encounters an error
   */
  @Test
  public void testTokenAuthentication() throws SQLException {

    String location =
            composeUrl(TestProperties.getOrAbort(DedicatedVaultTestProperty.DEDICATED_VAULT_SECRET_PATH),
                    "KEY="+TestProperties.getOrAbort(DedicatedVaultTestProperty.KEY),
                    "VAULT_ADDR="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR),
                    "VAULT_TOKEN="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_TOKEN));


    Properties properties = PROVIDER.getConnectionProperties(location);

    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }

  /**
   * Composes a full URL from a base URL and query options.
   */
  private static String composeUrl(String baseUrl, String... options) {
    return String.format("%s?%s", baseUrl, String.join("&", options));
  }
}
