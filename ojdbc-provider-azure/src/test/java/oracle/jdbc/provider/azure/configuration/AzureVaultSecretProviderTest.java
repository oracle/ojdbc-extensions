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
import oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.spi.OracleConfigurationJsonSecretProvider;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static oracle.jdbc.provider.TestProperties.getOrAbort;

public class AzureVaultSecretProviderTest {
  private static final OracleConfigurationJsonSecretProvider PROVIDER =
    OracleConfigurationJsonSecretProvider.find("azurevault");

  /**
   * Verifies {@link OracleConfigurationJsonSecretProvider} as implementing
   * behavior specified by its JavaDoc.
   * This test uses {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE} as its
   * authentication method.
   */
  @Test
  public void test() {
    Assertions.assertNotNull(PROVIDER.getSecret(
      constructJsonObject(
        TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL),
        TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_SECRET_NAME),
        TestProperties.getOrAbort(AzureTestProperty.AZURE_CLIENT_ID),
        TestProperties.getOrAbort(AzureTestProperty.AZURE_CLIENT_SECRET),
        TestProperties.getOrAbort(AzureTestProperty.AZURE_TENANT_ID)
    )));
  }

  /**
   * Returns a JSON object in following format:
   * <pre>
   * {
   *   "type": "azurevault",
   *   "value": "{"uri":"https://ojdbc-plugin-test-vault.vault.azure.net/secrets/test-db-password"}",
   *   "authentication": {
   *     "method": "AZURE_SERVICE_PRINCIPAL",
   *     "AZURE_CLIENT_ID": "<client-id>",
   *     "AZURE_CLIENT_SECRET": "<client-secret>",
   *     "AZURE_TENANT_ID": "<tenant-id>",
   *   }
   * }
   * </pre>
   */
  private OracleJsonObject constructJsonObject(
    String vaultUrl, String secretName, String clientId, String clientSecret, String tenantId) {

    OracleJsonFactory factory = new OracleJsonFactory();

    OracleJsonObject auth = factory.createObject();
    auth.put("method", "AZURE_SERVICE_PRINCIPAL");
    auth.put("AZURE_CLIENT_ID", clientId);
    auth.put("AZURE_CLIENT_SECRET", clientSecret);
    auth.put("AZURE_TENANT_ID", tenantId);

    OracleJsonObject password = factory.createObject();
    password.put("type", "azurevault");
    password.put("value", constructSecretUri(vaultUrl, secretName));
    password.put("authentication", auth);

    return password;
  }

  private String constructSecretUri(String vaultUrl, String secretName) {
    if (!vaultUrl.endsWith("/")) {
      vaultUrl += "/";
    }
    return vaultUrl + "secrets/" + secretName;
  }
}
