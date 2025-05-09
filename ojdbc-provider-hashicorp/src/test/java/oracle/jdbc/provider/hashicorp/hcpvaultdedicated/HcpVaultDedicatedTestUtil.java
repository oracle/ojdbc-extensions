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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated;

import oracle.jdbc.provider.TestProperties;
import org.junit.jupiter.api.Assumptions;

import java.util.Map;

import static oracle.jdbc.provider.TestProperties.getOptional;
import static oracle.jdbc.provider.TestProperties.getOrAbort;

/**
 * Utility class for configuring authentication parameters in tests that verify
 * implementations of {@link oracle.jdbc.spi.OracleResourceProvider} in the
 * ojdbc-provider-hcpvault-dedicated provider.
 */
public final class HcpVaultDedicatedTestUtil {

  private HcpVaultDedicatedTestUtil() { }

  /**
   * Configures authentication parameters for HashiCorp Vault Dedicated based on
   * values from a test.properties file. If the "authenticationMethod" is already set,
   * it verifies required parameters. If "auto-detect" is used, it selects the first
   * available method from the test properties.
   *
   * @param testParameters The parameter map used for authentication.
   */
  public static void configureAuthentication(Map<String, String> testParameters) {
    testParameters.putIfAbsent("authenticationMethod", "auto-detect");
    String authMethod = testParameters.get("authenticationMethod");

    switch (authMethod) {
      case "vault-token":
        testParameters.put("vaultToken", TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_TOKEN));
        break;
      case "approle":
        testParameters.put("roleId", getOrAbort(DedicatedVaultTestProperty.ROLE_ID));
        testParameters.put("secretId", getOrAbort(DedicatedVaultTestProperty.SECRET_ID));
        break;
      case "userpass":
        testParameters.put("vaultUsername", getOrAbort(DedicatedVaultTestProperty.VAULT_USERNAME));
        testParameters.put("vaultPassword", getOrAbort(DedicatedVaultTestProperty.VAULT_PASSWORD));
        break;
      case "github":
        testParameters.put("githubToken", getOrAbort(DedicatedVaultTestProperty.GITHUB_TOKEN));
        break;
      case "auto-detect":
        if (!configureAutoDetect(testParameters))
          Assumptions.abort("No valid authentication method found for auto-detect.");
        break;
      default:
        throw new IllegalArgumentException("Unsupported authentication method: " + authMethod);
    }
  }

  /**
   * Auto-detects the first available authentication method and configures the necessary parameters.
   * If no valid method is found, it returns {@code false}.
   *
   * @param testParameters The parameter map to populate.
   * @return {@code true} if a valid authentication method was found, otherwise {@code false}.
   */
  private static boolean configureAutoDetect(Map<String, String> testParameters) {
    return setIfAvailable(testParameters, "vaultToken", DedicatedVaultTestProperty.VAULT_TOKEN) ||
      (setIfAvailable(testParameters, "roleId", DedicatedVaultTestProperty.ROLE_ID) &&
       setIfAvailable(testParameters, "secretId", DedicatedVaultTestProperty.SECRET_ID)) ||
       (setIfAvailable(testParameters, "vaultUsername", DedicatedVaultTestProperty.VAULT_USERNAME) &&
       setIfAvailable(testParameters, "vaultPassword", DedicatedVaultTestProperty.VAULT_PASSWORD)) ||
       setIfAvailable(testParameters, "githubToken", DedicatedVaultTestProperty.GITHUB_TOKEN);
  }

  /**
   * Retrieves an optional test property and adds it to the parameters if available.
   *
   * @param testParameters The parameter map to populate.
   * @param key The parameter key.
   * @param property The test property to retrieve.
   * @return {@code true} if the value was added, otherwise {@code false}.
   */
  private static boolean setIfAvailable(Map<String, String> testParameters, String key, DedicatedVaultTestProperty property) {
    String value = getOptional(property);
    if (value != null) {
      testParameters.put(key, value);
      return true;
    }
    return false;
  }
}
