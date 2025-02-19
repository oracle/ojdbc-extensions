/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the HCP Vault Configuration Provider.
 */
public class HcpVaultConfigurationProviderTest {

  static {
    OracleConfigurationProvider.allowedProviders.add("hcpvaultsecret");
  }

  private static final OracleConfigurationProvider PROVIDER =
          OracleConfigurationProvider.find("hcpvaultsecret");


  @BeforeAll
  public static void setUp() {
    System.setProperty("HCP_ORG_ID",
            TestProperties.getOrAbort(HcpVaultTestProperty.HCP_ORG_ID));
    System.setProperty("HCP_PROJECT_ID",
            TestProperties.getOrAbort(HcpVaultTestProperty.HCP_PROJECT_ID));
    System.setProperty("HCP_APP_NAME",
            TestProperties.getOrAbort(HcpVaultTestProperty.HCP_APP_NAME));

  }

  /**
   * Verifies if HCP Vault Configuration Provider works with
   * CLIENT_CREDENTIALS authentication.
   * Without Key Option
   */
  @Test
  public void testClientCredentialsAuthentication() throws SQLException {
    // Load parameters from TestProperties
    String baseUrl = TestProperties.getOrAbort(HcpVaultTestProperty.SECRET_NAME);
    String clientId = "HCP_CLIENT_ID=" + TestProperties.getOrAbort(HcpVaultTestProperty.HCP_CLIENT_ID);
    String clientSecret = "HCP_CLIENT_SECRET=" + TestProperties.getOrAbort(HcpVaultTestProperty.HCP_CLIENT_SECRET);
    // Compose the connection URL
    String location = composeUrl(baseUrl, clientId, clientSecret);

    // Fetch properties using the provider
    Properties properties = PROVIDER.getConnectionProperties(location);

    // Assert required properties
    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }

  /**
   * Verifies if HCP Vault Configuration Provider works with
   * CLIENT_CREDENTIALS authentication.
   * With Key Option
   */
  @Test
  public void testClientCredentialsAuthenticationWithKeyOption() throws SQLException {
    // Load parameters from TestProperties
    String baseUrl = TestProperties.getOrAbort(HcpVaultTestProperty.SECRET_NAME_WITH_MULTIPLE_KEYS);
    String clientId = "HCP_CLIENT_ID=" + TestProperties.getOrAbort(HcpVaultTestProperty.HCP_CLIENT_ID);
    String clientSecret = "HCP_CLIENT_SECRET=" + TestProperties.getOrAbort(HcpVaultTestProperty.HCP_CLIENT_SECRET);
    String authMethod = "authentication=CLIENT_CREDENTIALS";
    String key = "key=" + TestProperties.getOrAbort(HcpVaultTestProperty.KEY);
    // Compose the connection URL
    String location = composeUrl(baseUrl, clientId, clientSecret, key, authMethod);

    // Fetch properties using the provider
    Properties properties = PROVIDER.getConnectionProperties(location);

    // Assert required properties
    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }


  /**
   * Verifies if HCP Vault Configuration Provider works with
   * CLI_CREDENTIALS_FILE authentication.
   * With Key Option
   */
  @Test
  @Disabled
  public void testCLICredentialsFileAuthenticationWithKeyOption() throws SQLException {
    // Load parameters from TestProperties
    String baseUrl = TestProperties.getOrAbort(HcpVaultTestProperty.SECRET_NAME_WITH_MULTIPLE_KEYS);
    String key = "key=" + TestProperties.getOrAbort(HcpVaultTestProperty.KEY);
    String authMethod = "authentication=CLI_CREDENTIALS_FILE";
    // Compose the connection URL
    String location = composeUrl(baseUrl, key, authMethod);

    // Fetch properties using the provider
    Properties properties = PROVIDER.getConnectionProperties(location);

    // Assert required properties
    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }

  /**
   * Verifies if the HCP Vault Configuration Provider works with
   * AUTO_DETECT authentication (with the key option).
   */
  @Test
  public void testAutoDetectAuthenticationWithKeyOption() throws SQLException {
    List<String> params = new ArrayList<>();
    String baseUrl = TestProperties.getOrAbort(HcpVaultTestProperty.SECRET_NAME_WITH_MULTIPLE_KEYS);
    String key = "key=" + TestProperties.getOrAbort(HcpVaultTestProperty.KEY);
    params.add(key);
    // Construct optional authentication parameters
    String clientId =
            TestProperties.getOptional(HcpVaultTestProperty.HCP_CLIENT_ID);
    String clientSecret =
            TestProperties.getOptional(HcpVaultTestProperty.HCP_CLIENT_SECRET);
    if(clientId!=null && clientSecret!=null) {
      params.add("HCP_CLIENT_ID=" + clientId);
      params.add("HCP_CLIENT_SECRET=" + clientSecret);
    }
    String credentialsFile =
            TestProperties.getOptional(HcpVaultTestProperty.HCP_CREDENTIALS_FILE);
    if(credentialsFile!=null) {
      params.add("HCP_CREDENTIALS_FILE=" + credentialsFile);
    }

    // Compose the connection URL
    String location = composeUrl(baseUrl, params.toArray(new String[0]));

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
