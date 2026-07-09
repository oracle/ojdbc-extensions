package oracle.jdbc.provider.azure.configuration;

import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.azure.data.appconfiguration.models.SettingSelector;
import com.azure.identity.ClientSecretCredentialBuilder;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AzureAppConfigurationProviderTest {

  /**
   * Label-related tests
   */
  @Nested
  class LabelTests {
    @Test
    public void testNoLabel() throws SQLException {
      String url = buildUrlWithLabel(null);
      try (Connection conn = tryConnection(url)) {
      } catch (IllegalArgumentException e) {
        // no connect descriptor is set for no label
        Assertions.assertTrue(e.getMessage().contains(
          "Missing mandatory attributes: connect_descriptor"));
      }
    }

    @Test
    public void testEmptyLabel() throws SQLException {
      String url = buildUrlWithLabel("");
      try (Connection conn = tryConnection(url)) {
      } catch (IllegalArgumentException e) {
        // no connect descriptor is set for empty label
        Assertions.assertTrue(e.getMessage().contains(
          "Missing mandatory attributes: connect_descriptor"));
      }
    }

    @Test
    public void testLabelEqualsAll() throws SQLException {
      String url = buildUrlWithLabel("all");
      try (Connection conn = tryConnection(url)) {
      } catch (IllegalArgumentException e) {
        Assertions.assertTrue(e.getMessage().contains(
          "Label 'all' or '*' is not supported."));
      }
    }

    @Test
    public void testLabelEqualsStar() throws SQLException {
      String url = buildUrlWithLabel("*");
      try (Connection conn = tryConnection(url)) {
      } catch (IllegalArgumentException e) {
        Assertions.assertTrue(e.getMessage().contains(
          "Label 'all' or '*' is not supported."));
      }
    }

    @Test
    public void testMultipleLabels() throws SQLException {
      String url = buildUrlWithLabel("dev,prod");
      try (Connection conn = tryConnection(url)) {
      } catch (IllegalArgumentException e) {
        Assertions.assertTrue(e.getMessage().contains(
          "Multiple labels and wildcard patterns are not supported."));
      }
    }

    @Test
    public void testWildcardLabel() throws SQLException {
      String url = buildUrlWithLabel("dev*");
      try (Connection conn = tryConnection(url)) {
      } catch (IllegalArgumentException e) {
        Assertions.assertTrue(e.getMessage().contains(
          "Multiple labels and wildcard patterns are not supported."));
      }
    }
  }

  /**
   * Verify that the cache is purged after hitting 1017 error.
   * Specifically, get connection to the same url twice, but modify the 'user'
   * every time.
   * The provided app configuration should have correct username, password and
   * correct connect descriptor to connect to Database.
   */
  @Test
  public void testCachePurged() {
    ConfigurationClient client = getSecretCredentialClient();
    String APP_CONFIG_NAME=
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_NAME);
    String APP_CONFIG_KEY =
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_KEY);
    String APP_CONFIG_LABEL =
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_LABEL);

    String prefix = "/testCachePurged/";
    String label = APP_CONFIG_LABEL;

    setupCachePurgeTestData(client, prefix, label, APP_CONFIG_KEY, APP_CONFIG_LABEL);

    String originalUrl =
            "jdbc:oracle:thin:@config-azure://" + APP_CONFIG_NAME +
            "?key=" + prefix + "&label=" + label;

    String url = composeUrlWithServicePrincipleAuthentication(originalUrl);

    try {
      // Connection fails: hit 1017
      SQLException exception = assertThrows(SQLException.class,
        () -> tryConnection(url), "Should throw an SQLException");
      Assertions.assertEquals(1017, exception.getErrorCode(), "Unexpected error message: " + exception.getMessage());
    } finally {
      cleanupCachePurgeTestData(client, prefix, label);
    }
  }

  /**
   * Sets up the test data for testCachePurged.
   * Copies the original configuration and updates the username in the copied
   * configuration to an invalid value to verify cache purging behavior.
   */
  private void setupCachePurgeTestData(
      ConfigurationClient client,
      String prefix,
      String label,
      String originalPrefix,
      String originalLabel) {

    cleanupCachePurgeTestData(client, prefix, label);

    // Copy the original configuration setting with the new prefix value
    SettingSelector selector = new SettingSelector();
    selector.setKeyFilter(originalPrefix + "*");
    selector.setLabelFilter(originalLabel);

    for (ConfigurationSetting setting : client.listConfigurationSettings(selector)) {
      String newKey;
      newKey = setting.getKey().replace(originalPrefix, prefix);

      if (setting.getKey().endsWith("user")) {
        setting.setValue("wrong_" + setting.getValue());
      }

      setting.setKey(newKey);
      client.addConfigurationSetting(setting);
    }
  }

  private void cleanupCachePurgeTestData(ConfigurationClient client, String prefix, String label) {
    SettingSelector selector = new SettingSelector();
    selector.setKeyFilter(prefix + "*");
    selector.setLabelFilter(label);

    for (ConfigurationSetting setting : client.listConfigurationSettings(selector)) {
      client.deleteConfigurationSetting(setting);
    }
  }

  @Test
  public void testInvalidConfiguration() {
    String invalidUrl = "jdbc:oracle:thin:@config-azure://invalid-config";
    assertThrows(SQLException.class, () -> tryConnection(invalidUrl), "Should throw an SQLException");
  }

  /**
   * Helper function: try to get connection form specified url
   */
  private Connection tryConnection(String url) throws SQLException {
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);
    Connection conn = ds.getConnection();
    return conn;
  }

  /**
   * Similar to the method in AzureAppConfigurationProviderURLParserTest
   */
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
   */
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

  /**
   * Helper function: to construct a URL with label parameter.
   */
  private String buildUrlWithLabel(String label) {
    String APP_CONFIG_NAME=
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_NAME);
    String APP_CONFIG_KEY =
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_KEY);
    String baseUrl =
      "jdbc:oracle:thin:@config-azure://" + APP_CONFIG_NAME +
        "?key=" + APP_CONFIG_KEY;
    if (label != null) {
      baseUrl += "&label=" + label;
    }
    return composeUrlWithServicePrincipleAuthentication(baseUrl);
  }
}