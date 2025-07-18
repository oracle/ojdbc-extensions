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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret;

import oracle.jdbc.provider.TestProperties;
import org.junit.jupiter.api.Assumptions;

import java.util.Map;

/**
 * Utility class for configuring authentication parameters in tests that verify
 * implementations of {@link oracle.jdbc.spi.OracleResourceProvider} in the
 * ojdbc-provider-hcpvault-secrets provider.
 */
public final class HcpVaultTestUtil {

  private HcpVaultTestUtil() { }

  public static void configureAuthentication(Map<String, String> testParameters) {
    testParameters.putIfAbsent("authenticationMethod", "auto-detect");
    String authMethod = testParameters.get("authenticationMethod");

    switch (authMethod) {
      case "client-credentials":
        testParameters.put("clientId", TestProperties.getOrAbort(HcpVaultTestProperty.HCP_CLIENT_ID));
        testParameters.put("clientSecret", TestProperties.getOrAbort(HcpVaultTestProperty.HCP_CLIENT_SECRET));
        break;
      case "cli-credentials-file":
        testParameters.put("credentialsFile", TestProperties.getOrAbort(HcpVaultTestProperty.HCP_CREDENTIALS_FILE));
        break;
      case "auto-detect":
        if (!configureAutoDetect(testParameters))
          Assumptions.abort("No valid authentication method found for auto-detect.");
        break;
      default:
        throw new IllegalArgumentException("Unsupported authentication method: " + authMethod);
    }

    testParameters.put("orgId",
      TestProperties.getOrAbort(HcpVaultTestProperty.HCP_ORG_ID));
    testParameters.put("projectId",
      TestProperties.getOrAbort(HcpVaultTestProperty.HCP_PROJECT_ID));
    testParameters.put("appName",
      TestProperties.getOrAbort(HcpVaultTestProperty.HCP_APP_NAME));
  }

  private static boolean configureAutoDetect(Map<String, String> testParameters) {
    return setIfAvailable(testParameters, "clientId", HcpVaultTestProperty.HCP_CLIENT_ID) &&
            setIfAvailable(testParameters, "clientSecret", HcpVaultTestProperty.HCP_CLIENT_SECRET) ||
            setIfAvailable(testParameters, "credentialsFile", HcpVaultTestProperty.HCP_CREDENTIALS_FILE);
  }

  private static boolean setIfAvailable(Map<String, String> testParameters, String key, HcpVaultTestProperty property) {
    String value = TestProperties.getOptional(property);
    if (value != null) {
      testParameters.put(key, value);
      return true;
    }
    return false;
  }
}
