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

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the {@link AzureVaultJsonProvider} as implementing behavior
 * specified by its JavaDoc.
 */

public class AzureVaultJsonProviderTest {

  static {
    OracleConfigurationProvider.allowedProviders.add("azurevault");
  }

  private static final OracleConfigurationProvider PROVIDER =
    OracleConfigurationProvider.find("azurevault");

  /**
   * <p>
   * Verifies AUTHENTICATION=AZURE_SERVICE_PRINCIPAL parameter setting.
   * This test uses {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE} as its
   * authentication method.
   **/
  @Test
  public void testServicePrincipleAuthentication() throws SQLException {
    String baseUrl = String.format("%s/secrets/%s",
      TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL),
      TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_SECRET_PAYLOAD_NAME));

    String[] options = new String[] {
      "AUTHENTICATION=AZURE_SERVICE_PRINCIPAL",
      "AZURE_CLIENT_ID=" + TestProperties.getOrAbort(
        AzureTestProperty.AZURE_CLIENT_ID),
      "AZURE_CLIENT_SECRET=" + TestProperties.getOrAbort(
        AzureTestProperty.AZURE_CLIENT_SECRET),
      "AZURE_TENANT_ID=" + TestProperties.getOrAbort(
        AzureTestProperty.AZURE_TENANT_ID)};

    String url = composeUrl(baseUrl, options);
    verifyProperties(url);
  }

  /**
   * <p>
   * Verifies AUTHENTICATION=AZURE_SERVICE_PRINCIPAL parameter setting with
   * key option.
   **/
  @Test
  public void testServicePrincipleAuthenticationWithKeyOption()
      throws SQLException {
    String baseUrl = String.format("%s/secrets/%s",
      TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL),
      TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_SECRET_PAYLOAD_NAME_MULTIPLE_KEYS));

    String[] options = new String[] {
            "AUTHENTICATION=AZURE_SERVICE_PRINCIPAL",
            "AZURE_CLIENT_ID=" + TestProperties.getOrAbort(
                    AzureTestProperty.AZURE_CLIENT_ID),
            "AZURE_CLIENT_SECRET=" + TestProperties.getOrAbort(
                    AzureTestProperty.AZURE_CLIENT_SECRET),
            "AZURE_TENANT_ID=" + TestProperties.getOrAbort(
                    AzureTestProperty.AZURE_TENANT_ID),
            "key=" + TestProperties.getOrAbort(
                    AzureTestProperty.AZURE_KEY_VAULT_SECRET_PAYLOAD_KEY)};

    String url = composeUrl(baseUrl, options);
    verifyProperties(url);
  }

  /** verifies a properties object returned with a given URL **/
  private static void verifyProperties(String url) throws SQLException {
    Properties properties = PROVIDER.getConnectionProperties(url);

    assertNotNull(properties);
  }

  private static String composeUrl(String baseUrl, String... options) {
    return String.format("%s?%s", baseUrl, String.join("&", options));
  }
}