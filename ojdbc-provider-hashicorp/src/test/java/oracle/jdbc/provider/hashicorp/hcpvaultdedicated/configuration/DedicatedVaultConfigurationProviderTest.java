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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the Dedicated Vault Configuration Provider.
 */
public class DedicatedVaultConfigurationProviderTest {

  static {
    OracleConfigurationProvider.allowedProviders.add("hcpvaultdedicated");
  }


  private static final OracleConfigurationProvider PROVIDER =
          OracleConfigurationProvider.find("hcpvaultdedicated");

  /**
   * Verifies if Dedicated Vault Configuration Provider works with TOKEN-based
   * authentication.
   * Without the key option.
   **/
  @Test
  public void testTokenAuthentication() throws SQLException {
    String location =
      composeUrl(TestProperties.getOrAbort(DedicatedVaultTestProperty.DEDICATED_VAULT_SECRET_PATH),
        "VAULT_ADDR="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR),
        "VAULT_TOKEN="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_TOKEN),
        "authentication=vault_token");

    Properties properties = PROVIDER.getConnectionProperties(location);

    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }


  /**
   * Verifies if Dedicated Vault Configuration Provider works with TOKEN-based
   * authentication.
   * With the key option.
   */
  @Test
  public void testTokenAuthenticationWithKeyOption() throws SQLException {
    String location =
      composeUrl(TestProperties.getOrAbort(DedicatedVaultTestProperty.DEDICATED_VAULT_SECRET_PATH_WITH_MULTIPLE_KEYS),
        "key="+TestProperties.getOrAbort(DedicatedVaultTestProperty.KEY),
        "VAULT_ADDR="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR),
        "VAULT_TOKEN="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_TOKEN),
        "authentication=vault_token");


    Properties properties = PROVIDER.getConnectionProperties(location);

    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }

  /**
   * Verifies if Dedicated Vault Configuration Provider works with TOKEN-based
   * authentication.
   * Without the key option.
   */
  @Test
  public void testUserPassAuthenticationWithoutKeyOption() throws SQLException {
    String location =
            composeUrl(TestProperties.getOrAbort(DedicatedVaultTestProperty.DEDICATED_VAULT_SECRET_PATH),
                    "VAULT_ADDR="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR),
                    "VAULT_USERNAME="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_USERNAME),
                    "VAULT_PASSWORD="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_PASSWORD),
                    "VAULT_NAMESPACE="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_NAMESPACE),
                    "authentication=userpass");


    Properties properties = PROVIDER.getConnectionProperties(location);

    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }

  /**
   * Verifies if Dedicated Vault Configuration Provider works with TOKEN-based
   * authentication.
   * With the key option.
   */
  @Test
  public void testUserPassAuthenticationWithKeyOption() throws SQLException {
    String location =
            composeUrl(TestProperties.getOrAbort(DedicatedVaultTestProperty.DEDICATED_VAULT_SECRET_PATH_WITH_MULTIPLE_KEYS),
                    "key="+TestProperties.getOrAbort(DedicatedVaultTestProperty.KEY),
                    "VAULT_ADDR="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR),
                    "VAULT_USERNAME="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_USERNAME),
                    "VAULT_PASSWORD="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_PASSWORD),
                    "VAULT_NAMESPACE="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_NAMESPACE),
                    "authentication=userpass");

    Properties properties = PROVIDER.getConnectionProperties(location);

    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }

  /**
   * Verifies if Dedicated Vault Configuration Provider works with AppRole
   * authentication and a key option for secrets with multiple keys.
   */
  @Test
  public void testAppRoleAuthenticationWithKeyOption() throws SQLException {
    String location = composeUrl(
            TestProperties.getOrAbort(DedicatedVaultTestProperty.DEDICATED_VAULT_SECRET_PATH_WITH_MULTIPLE_KEYS),
            "key=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.KEY),
            "VAULT_ADDR=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR),
            "ROLE_ID=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.ROLE_ID),
            "VAULT_NAMESPACE="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_NAMESPACE),
            "SECRET_ID=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.SECRET_ID),
            "authentication=approle");

    Properties properties = PROVIDER.getConnectionProperties(location);

    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }

  /**
   * Verifies if Dedicated Vault Configuration Provider works with GitHub
   * authentication and a key option for secrets with multiple keys.
   */
  @Test
  public void testGithubAuthenticationWithKeyOption() throws SQLException {
    String location = composeUrl(
            TestProperties.getOrAbort(DedicatedVaultTestProperty.DEDICATED_VAULT_SECRET_PATH_WITH_MULTIPLE_KEYS),
            "key=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.KEY),
            "VAULT_ADDR=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR),
            "GITHUB_TOKEN=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.GITHUB_TOKEN),
            "VAULT_NAMESPACE="+TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_NAMESPACE),
            "authentication=github");

    Properties properties = PROVIDER.getConnectionProperties(location);

    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }

  /**
   * Verifies if Dedicated Vault Configuration Provider works with the
   * AUTO-DETECT authentication and a key option for secrets with multiple keys.
   */
  @Test
  public void testAutoDetectAuthenticationWithKeyOption() throws SQLException {
    String baseUrl = TestProperties.getOrAbort(DedicatedVaultTestProperty.DEDICATED_VAULT_SECRET_PATH_WITH_MULTIPLE_KEYS);
    String key = "key=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.KEY);
    String vaultAddr = "VAULT_ADDR=" + TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR);

    List<String> params = new ArrayList<>();
    params.add(vaultAddr);
    params.add(key);

    // Add available authentication methods dynamically
    String vaultToken = TestProperties.getOptional(DedicatedVaultTestProperty.VAULT_TOKEN);
    if (vaultToken != null) {
      params.add("VAULT_TOKEN=" + vaultToken);
    }

    String vaultUsername = TestProperties.getOptional(DedicatedVaultTestProperty.VAULT_USERNAME);
    String vaultPassword =
            TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_PASSWORD);
    if (vaultUsername != null && vaultPassword != null) {
      params.add("VAULT_USERNAME=" + vaultUsername);
      params.add("VAULT_PASSWORD=" + vaultPassword);
    }

    String roleId = TestProperties.getOptional(DedicatedVaultTestProperty.ROLE_ID);
    String secretId = TestProperties.getOptional(DedicatedVaultTestProperty.SECRET_ID);
    if (roleId != null && secretId != null) {
      params.add("ROLE_ID=" + roleId);
      params.add("SECRET_ID=" + secretId);
    }

    String githubToken = TestProperties.getOptional(DedicatedVaultTestProperty.GITHUB_TOKEN);
    if (githubToken != null) {
      params.add("GITHUB_TOKEN=" + githubToken);
    }

    params.add("authentication=auto_detect");

    String location = composeUrl(baseUrl, params.toArray(new String[0]));

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
