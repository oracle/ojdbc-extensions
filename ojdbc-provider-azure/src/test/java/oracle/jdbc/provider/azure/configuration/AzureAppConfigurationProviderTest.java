package oracle.jdbc.provider.azure.configuration;

import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.azure.identity.ClientSecretCredentialBuilder;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class AzureAppConfigurationProviderTest {

  /**
   * Verify that the cache is purged after hitting 1017 error.
   * Specifically, get connection to the same url twice, but modify the 'user'
   * every time.
   * The provided app configuration should have correct username, password and
   * correct connect descriptor to connect to Database.
   **/
  @Test
  public void testCachePurged() throws SQLException {
    ConfigurationClient client = getSecretCredentialClient();
    String APP_CONFIG_NAME=
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_NAME);
    String APP_CONFIG_KEY =
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_KEY);
    String APP_CONFIG_LABEL =
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_LABEL);
    String username = "user";
    String originalUrl =
            "jdbc:oracle:thin:@config-azure://" + APP_CONFIG_NAME +
            "?key=" + APP_CONFIG_KEY + "&label=" + APP_CONFIG_LABEL;

    String url = composeUrlWithServicePrincipleAuthentication(originalUrl);

    // Retrieve original value of 'user'
    String originalKeyValue =
      client.getConfigurationSetting(APP_CONFIG_KEY + username,
      APP_CONFIG_LABEL).getValue();

    // Set value of 'user' wrong
    client.setConfigurationSetting( APP_CONFIG_KEY + username,
      APP_CONFIG_LABEL, originalKeyValue + "wrong");

    // Connection fails: hit 1017
    SQLException exception = assertThrows(SQLException.class,
      () -> tryConnection(url), "Should throw an SQLException");
    Assertions.assertEquals(exception.getErrorCode(), 1017);

    // Set value of 'user' correct
    ConfigurationSetting result =
      client.setConfigurationSetting(APP_CONFIG_KEY + username,
      APP_CONFIG_LABEL, originalKeyValue);
    Assertions.assertEquals(originalKeyValue, result.getValue());

    // Connection succeeds
    try (Connection conn = tryConnection(url)) {
      Assertions.assertNotNull(conn);

      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery("SELECT 'Hello, db' FROM sys.dual");
      Assertions.assertNotNull(rs.next());
      Assertions.assertEquals("Hello, db", rs.getString(1));
    }
  }

  /**
   * Helper function: try to get connection form specified url
   **/
  private Connection tryConnection(String url) throws SQLException {
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);
    Connection conn = ds.getConnection();
    return conn;
  }

  /**
   * Similar to the method in AzureAppConfigurationProviderURLParserTest
   **/
  private static ConfigurationClient getSecretCredentialClient() {
    return new ConfigurationClientBuilder()
      .credential( new ClientSecretCredentialBuilder()
        .clientId(TestProperties.getOrAbort(AzureTestProperty.AZURE_CLIENT_ID))
        .clientSecret(
          TestProperties.getOrAbort(AzureTestProperty.AZURE_CLIENT_SECRET))
        .tenantId(TestProperties.getOrAbort(AzureTestProperty.AZURE_TENANT_ID))
        .build())
      .endpoint("https://" + TestProperties.getOrAbort(
        AzureTestProperty.AZURE_APP_CONFIG_NAME) + ".azconfig.io")
      .buildClient();
  }

  /**
   * Use {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE} as its
   * authentication method.
   **/
  private String composeUrlWithServicePrincipleAuthentication(String originalUrl){
    String[] options = new String[] {
            "AUTHENTICATION=AZURE_SERVICE_PRINCIPAL",
            "AZURE_CLIENT_ID=" + TestProperties.getOrAbort(
                    AzureTestProperty.AZURE_CLIENT_ID),
            "AZURE_CLIENT_SECRET=" + TestProperties.getOrAbort(
                    AzureTestProperty.AZURE_CLIENT_SECRET),
            "AZURE_TENANT_ID=" + TestProperties.getOrAbort(
                    AzureTestProperty.AZURE_TENANT_ID)};
    return String.format("%s&%s", originalUrl, String.join("&", options));
  }
}