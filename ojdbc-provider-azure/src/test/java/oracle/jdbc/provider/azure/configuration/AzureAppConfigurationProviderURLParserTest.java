/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.azure.configuration;

import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.spi.OracleConfigurationCachableProvider;
import oracle.jdbc.spi.OracleConfigurationProvider;
import oracle.jdbc.util.OracleConfigurationCache;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static oracle.jdbc.provider.TestProperties.getOrAbort;
import static oracle.jdbc.provider.TestProperties.getProperties;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the {@link AzureAppConfigurationProvider} as implementing behavior
 * specified by its JavaDoc.
 */
public class AzureAppConfigurationProviderURLParserTest {
  private static OracleConfigurationCache CACHE;
  @BeforeAll
  static void init() {
    CACHE = ((AzureAppConfigurationProvider)OracleConfigurationProvider
        .find("azure"))
        .getCache();
  }


  /**
   * Verifies {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * with a client secret.
   */
  @Nested
  class TestServicePrincipleSecret {
    String[] options;
    @BeforeEach
    void beforeEach() {
      options = new String[] {
        "AUTHENTICATION=AZURE_SERVICE_PRINCIPAL",
        "AZURE_CLIENT_ID=" + TestProperties.getOrAbort(
          AzureTestProperty.AZURE_CLIENT_ID),
        "AZURE_CLIENT_SECRET=" + TestProperties.getOrAbort(
          AzureTestProperty.AZURE_CLIENT_SECRET),
        "AZURE_TENANT_ID=" + TestProperties.getOrAbort(
          AzureTestProperty.AZURE_TENANT_ID)};

      AzureAppConfigurationProvider provider =
          (AzureAppConfigurationProvider)OracleConfigurationProvider
              .find("azure");
    }
    @Test
    void testValidUrlWithSecret() throws SQLException {
      String url = composeURL(options);
      removeCacheEntry(url);
      verifyValidUrl(url);
    }

    @Test
    void testInvalidUrlWithSecret() {
      String invalidUrl = composeURL(options)
          .replace("@config-azure", "@config-azurex");
      removeCacheEntry(invalidUrl);
      verifyInvalidTypeThrowsException(invalidUrl);
    }

    @Test
    void testInvalidKeyWithSecret() {
      ConfigurationClient client = getSecretCredentialClient();
      String url = composeURL(options);
      removeCacheEntry(url);
      verifyInvalidKeyThrowsException(client, url);
    }

    @Test
    void testNonWhitelistedKeyWithSecret() {
      ConfigurationClient client = getSecretCredentialClient();
      String url = composeURL(options);
      removeCacheEntry(url);
      verifyNonWhitelistedKeyThrowsException(client, url);
    }
  }

  /**
   * Verifies {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   */
  @Test
  public void testServicePrincipleCertificate() throws SQLException {
    String url = composeURL(
        "AUTHENTICATION=AZURE_SERVICE_PRINCIPAL",
        "AZURE_CLIENT_ID=" + TestProperties.getOrAbort(
            AzureTestProperty.AZURE_CLIENT_ID),
        "AZURE_CLIENT_CERTIFICATE_PATH=" + TestProperties.getOrAbort(
            AzureTestProperty.AZURE_CLIENT_CERTIFICATE_PATH),
        "AZURE_TENANT_ID=" + TestProperties.getOrAbort(
            AzureTestProperty.AZURE_TENANT_ID));
    removeCacheEntry(url);
    verifyValidUrl(url);
  }

  /**
   * Verifies {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   */
  @Test
  public void testServicePrinciplePfxCertificate() throws SQLException {
    String url = composeURL(
        "AUTHENTICATION=AZURE_SERVICE_PRINCIPAL",
        "AZURE_CLIENT_ID=" + TestProperties.getOrAbort(
            AzureTestProperty.AZURE_CLIENT_ID),
        "AZURE_CLIENT_CERTIFICATE_PATH=" + TestProperties.getOrAbort(
            AzureTestProperty.AZURE_CLIENT_PFX_CERTIFICATE_PATH),
        "AZURE_CLIENT_CERTIFICATE_PASSWORD=" + TestProperties.getOrAbort(
            AzureTestProperty.AZURE_CLIENT_PFX_PASSWORD),
        "AZURE_TENANT_ID=" + TestProperties.getOrAbort(
            AzureTestProperty.AZURE_TENANT_ID));
    removeCacheEntry(url);
    verifyValidUrl(url);
  }

  /** Verifies a valid URL */
  private static void verifyValidUrl(String url) throws SQLException {
    // Remove the entry from cache before verifying url


    // No changes required, configuration provider is loaded at runtime
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);

    // Standard JDBC code
    int result = -1;
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("select 1 from dual");
    if (rs.next())
      result = rs.getInt(1);

    assertEquals(1, result);
  }

  /**
   * Verifies an exception thrown with an invalid type of the provider
   */
  private static void verifyInvalidTypeThrowsException(String invalidUrl) {
    Exception exception = assertThrows(Exception.class,
      () -> {
        OracleDataSource ds = new OracleDataSource();
        ds.setURL(invalidUrl);
        ds.getConnection();},
      "Should throw an Exception");

    assertTrue(exception.getMessage().contains("Cannot find the provider type"),
      "Something went unexpected: " + exception.getMessage());
  }

  /**
   * Verifies an invalid key value in App configuration throws Exception
   */
  private static void verifyInvalidKeyThrowsException(
    ConfigurationClient client, String url) {
    // Name of the invalid key to add to the configuration service.
    String key = TestProperties.getOrAbort(
      AzureTestProperty.AZURE_APP_CONFIG_KEY) + "jdbc/invalidKey";
    String label = TestProperties.getOrAbort(
      AzureTestProperty.AZURE_APP_CONFIG_LABEL);
    String value = "foo";

    // Add the new configuration setting
    client.addConfigurationSetting(key, label, value);

    try {
      SQLException exception = assertThrows(SQLException.class,
        () -> {
          OracleDataSource ds = new OracleDataSource();
          ds.setURL(url);
          ds.getConnection();},
        "Should throw an SQLException");
      // Expected exception:
      // ORA-18729: Property is not whitelisted for external providers
      assertEquals(exception.getErrorCode(), 18729,
        "Something went unexpected: " + exception.getMessage());
    } finally {
      client.deleteConfigurationSetting(key, label);
    }
  }

  /**
   * Verifies a non-whitelisted key value in App configuration throws Exception
   */
  private static void verifyNonWhitelistedKeyThrowsException(
    ConfigurationClient client, String url) {
    // Name of the non-whitelisted key to add to the configuration service.
    String key = TestProperties.getOrAbort(
      AzureTestProperty.AZURE_APP_CONFIG_KEY) + "jdbc/oracle.jdbc.newPassword";
    String label = TestProperties.getOrAbort(
      AzureTestProperty.AZURE_APP_CONFIG_LABEL);
    String value = "foo";

    // Add the new configuration setting
    client.addConfigurationSetting(key, label, value);

    try {
      SQLException exception = assertThrows(SQLException.class,
        () -> {
          OracleDataSource ds = new OracleDataSource();
          ds.setURL(url);
          ds.getConnection();},
        "Should throw an SQLException");
      // Expected exception:
      // ORA-18729: Property is not whitelisted for external providers
      assertEquals(exception.getErrorCode(), 18729,

        "Something went unexpected: " + exception.getMessage());
    } finally {
      client.deleteConfigurationSetting(key, label);
    }
  }

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

  private static String composeURL(String... options) {
    String optionsString = String.join("&", options);

    Properties properties = getProperties();
    if (properties.containsKey(AzureTestProperty.AZURE_APP_CONFIG_LABEL)) {
      optionsString = String.format("label=%s&%s",
        properties.getProperty(AzureTestProperty.AZURE_APP_CONFIG_LABEL.name()),
        optionsString);
    }
    return String.format("jdbc:oracle:thin:@config-azure://%s?key=%s&%s",
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_NAME),
      TestProperties.getOrAbort(AzureTestProperty.AZURE_APP_CONFIG_KEY),
      optionsString);
  }

  /**
   * Remove configuration from the cache in
   * {@link AzureAppConfigurationProvider} to ensure the tests are independent.
   * @param url to be used in the test
   */
  private static void removeCacheEntry(String url) {
    String location =
        url.replaceFirst("jdbc:oracle:thin:@config-azure://", "");
    CACHE.remove(location);
  }
}
