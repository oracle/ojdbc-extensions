package oracle.jdbc.provider.hashicorp.hcpvault.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the HCP Vault Configuration Provider.
 */
public class HcpVaultConfigurationProviderTest {

  static {
    OracleConfigurationProvider.allowedProviders.add("hcpvault");
  }

  private static final OracleConfigurationProvider PROVIDER =
          OracleConfigurationProvider.find("hcpvault");

  /**
   * Verifies if HCP Vault Configuration Provider works with CLIENT_CREDENTIALS authentication
   *
   * @throws SQLException if the provider encounters an error
   */
  @Test
  public void testClientCredentialsAuthentication() throws SQLException {
    // Load parameters from TestProperties
    String baseUrl = TestProperties.getOrAbort(HcpVaultTestProperty.APP_NAME);
    String orgId = "ORG_ID=" + TestProperties.getOrAbort(HcpVaultTestProperty.ORG_ID);
    String projectId = "PROJECT_ID=" + TestProperties.getOrAbort(HcpVaultTestProperty.PROJECT_ID);
    String clientId = "CLIENT_ID=" + TestProperties.getOrAbort(HcpVaultTestProperty.CLIENT_ID);
    String clientSecret = "CLIENT_SECRET=" + TestProperties.getOrAbort(HcpVaultTestProperty.CLIENT_SECRET);
    String secretName = "SECRET_NAME=" + TestProperties.getOrAbort(HcpVaultTestProperty.SECRET_NAME);
    // Compose the connection URL
    String location = composeUrl(baseUrl, orgId, projectId, clientId, clientSecret, secretName);

    // Fetch properties using the provider
    Properties properties = PROVIDER.getConnectionProperties(location);

    // Assert required properties
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
