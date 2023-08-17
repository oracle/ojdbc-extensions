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

package oracle.jdbc.provider.azure.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import org.junit.jupiter.api.Assumptions;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static oracle.jdbc.provider.TestProperties.getOrAbort;

/**
 * Common utility methods for tests that verify implementations of
 * {@link oracle.jdbc.spi.OracleResourceProvider} in the ojdbc-provider-azure
 * module.
 */
final class AzureResourceProviderTestUtil {
  private AzureResourceProviderTestUtil() { }

  /**
   * <p>
   * Configures provider parameters for authentication with Azure based on
   * values from a test.properties file.
   * </p><p>
   * This method does nothing if the "authenticationMethod" parameter is already
   * configured in the given {@code testParameters}. This allows tests to
   * explicitly verify a particular authentication method, if needed.
   * </p><p>
   * Otherwise, this method configures "authenticationMethod" as "auto-detect",
   * and configures additional parameters for authentication as a managed
   * identity or service principal. These additional parameters must be
   * specified in a test.properties file. The test that calls this method will
   * abort if the test.properties file does not contain additional parameters
   * needed for managed identity or service principal authentication.
   * </p><p>
   * Future work might expand this method to check for username/password
   * authentication, or check for environment variables that configure the
   * additional parameters.
   * </p>
   * @param testParameters Parameters to configure with an authentication
   * method.
   */
  static void configureAuthentication(Map<String, String> testParameters) {

    boolean isManagedIdentity =
      "true".equalsIgnoreCase(
        TestProperties.getOptional(AzureTestProperty.AZURE_MANAGED_IDENTITY));

    if (!isManagedIdentity) {
      testParameters.put("tenantId", TestProperties.getOrAbort(
        AzureTestProperty.AZURE_TENANT_ID));
      testParameters.put("clientId", TestProperties.getOrAbort(
        AzureTestProperty.AZURE_CLIENT_ID));

      // Use any secret or certificate available
      String clientSecret =
        TestProperties.getOptional(AzureTestProperty.AZURE_CLIENT_SECRET);
      String clientCertificatePath =
        TestProperties.getOptional(
          AzureTestProperty.AZURE_CLIENT_CERTIFICATE_PATH);
      String clientPfxCertificate =
        TestProperties.getOptional(
          AzureTestProperty.AZURE_CLIENT_PFX_CERTIFICATE_PATH);
      String pfxPassword =
        TestProperties.getOptional(AzureTestProperty.AZURE_CLIENT_PFX_PASSWORD);

      if (clientSecret != null) {
        testParameters.put("clientSecret", clientSecret);
      }
      else if (clientCertificatePath != null) {
        testParameters.put("clientCertificatePath", clientCertificatePath);
      }
      else if (clientPfxCertificate != null) {
        testParameters.put("certificate", clientPfxCertificate);
        testParameters.put("certificatePassword", pfxPassword);
      }
      else {
        Assumptions.abort(
          "None of the following test.properties are configured:" +
            Stream.of(
                AzureTestProperty.AZURE_MANAGED_IDENTITY,
                AzureTestProperty.AZURE_CLIENT_SECRET,
                AzureTestProperty.AZURE_CLIENT_CERTIFICATE_PATH,
                AzureTestProperty.AZURE_CLIENT_PFX_CERTIFICATE_PATH)
              .map(AzureTestProperty::name)
              .collect(Collectors.joining()));
      }
    }
  }

}
