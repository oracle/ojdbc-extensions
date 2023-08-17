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

package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.AccessTokenProvider;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static oracle.jdbc.provider.TestProperties.abortIfNotEqual;
import static oracle.jdbc.provider.TestProperties.getOrAbort;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the {@link DataplaneTokenProvider} as implementing behavior specified by
 * its JavaDoc.
 */
public class DataplaneTokenProviderTest {

  private static final AccessTokenProvider PROVIDER =
    findProvider(AccessTokenProvider.class, "ojdbc-provider-oci-token");

  /**
   * Verifies the authentication-method=config-file URI parameter setting.
   * This test will abort if an OCI configuration file and profile name are not
   * configured.
   */
  @Test
  public void testConfigFile() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "config-file");
    testParameters.put("configFile", TestProperties.getOrAbort(
      OciTestProperty.OCI_CONFIG_FILE));
    testParameters.put("profile", TestProperties.getOrAbort(
      OciTestProperty.OCI_CONFIG_PROFILE));
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies the authenticationMethod=cloud-shell URI parameter setting.
   * This test needs to be run the OCI Cloud Shell to actually verify the
   * parameter setting. If not run in OCI Cloud Shell, then this test will
   * abort.
   */
  @Test
  public void testCloudShell() {
    TestProperties.abortIfNotEqual(OciTestProperty.OCI_CLOUD_SHELL, "true");
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "cloud-shell");
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies the authenticationMethod=instance-principal URI parameter
   * setting. This test needs to be run in a Compute Instance. It requires
   * the instance to be a member of a Dynamic Group, and for that Dynamic Group
   * to included in Policy, and for that Policy to allow access to all resources
   * in a tenancy (ie: in the root compartment).
   */
  @Test
  public void testInstancePrincipal() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    TestProperties.abortIfNotEqual(OciTestProperty.OCI_INSTANCE_PRINCIPAL, "true");
    testParameters.put("authenticationMethod", "instance-principal");
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies the authenticationMethod=auto-detect URI parameter
   * setting. This test needs to be run in a Compute Instance, in Cloud Shell,
   * or in an environment with a $HOME/.oci/config file.
   */
  @Test
  public void testAutoDetect() {

    // Configure any available credentials
    boolean isCloudShell =
      "true".equalsIgnoreCase(
        TestProperties.getOptional(OciTestProperty.OCI_CLOUD_SHELL));
    boolean isInstancePrincipal =
      "true".equalsIgnoreCase(
        TestProperties.getOptional(OciTestProperty.OCI_INSTANCE_PRINCIPAL));

    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "auto-detect");

    if (!isCloudShell && !isInstancePrincipal) {
      testParameters.put("configFile", TestProperties.getOrAbort(
        OciTestProperty.OCI_CONFIG_FILE));
      testParameters.put("profile", TestProperties.getOrAbort(
        OciTestProperty.OCI_CONFIG_PROFILE));
    }

    verifyAccessToken(testParameters);
  }

  /** Verifies an access token returned for a URI with the given parameters */
  private static void verifyAccessToken(
    Map<String, CharSequence> testParameters) {

    // Default values for all tests
    testParameters.put("scope", TestProperties.getOrAbort(
      OciTestProperty.OCI_TOKEN_SCOPE));

    Map<Parameter, CharSequence> parameterValues =
      createParameterValues(PROVIDER, testParameters);

    AccessToken accessToken = PROVIDER.getAccessToken(parameterValues);

    assertNotNull(accessToken);
  }

}
