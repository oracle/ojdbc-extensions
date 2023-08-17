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

package oracle.jdbc.provider.azure;

import com.azure.core.util.Configuration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Names of properties that configure Azure tests. Descriptions and examples of
 * each property can be found in the "example-test.properties" file within the
 * root directory of the project.
 */
public enum AzureTestProperty {

  AZURE_TENANT_ID,

  AZURE_CLIENT_ID,

  AZURE_CLIENT_SECRET,

  AZURE_CLIENT_CERTIFICATE_PATH,

  AZURE_CLIENT_PFX_CERTIFICATE_PATH,

  AZURE_CLIENT_PFX_PASSWORD,

  AZURE_USERNAME,

  AZURE_PASSWORD,

  AZURE_MANAGED_IDENTITY,

  AZURE_APP_CONFIG_NAME,

  AZURE_APP_CONFIG_KEY,

  AZURE_APP_CONFIG_LABEL,

  AZURE_TOKEN_SCOPE,

  AZURE_KEY_VAULT_URL,

  AZURE_KEY_VAULT_SECRET_NAME;

  /**
   * Aborts the calling test if the given {@code names} are not configured in
   * the environment read by the Azure SDK.
   * @param names Names of configurable values that the Azure SDK reads from the
   * environment.
   */
  public static void abortIfEnvNotConfigured(String... names) {
    Configuration configuration = Configuration.getGlobalConfiguration();

    for (String name : names) {
      assumeTrue(
        configuration.contains(name),
        String.format(
          "\"%s\" is not configured in the environment read by the" +
            " Azure SDK for Java",
          name));
    }
  }

}
