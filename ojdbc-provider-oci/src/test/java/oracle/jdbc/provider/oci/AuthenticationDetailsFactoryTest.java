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

package oracle.jdbc.provider.oci;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.oci.authentication.AuthenticationDetailsFactory;
import oracle.jdbc.provider.oci.authentication.AuthenticationMethod;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetBuilder;
import org.junit.jupiter.api.Test;

import static oracle.jdbc.provider.TestProperties.getOrAbort;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@link AuthenticationDetailsFactory} */
public class AuthenticationDetailsFactoryTest {

  /**
   * Verifies {@link AuthenticationMethod#CONFIG_FILE}
   */
  @Test
  public void testConfigFile() {
    verifyAuthenticationDetails(
      buildParameterSet(AuthenticationMethod.CONFIG_FILE)
        .add(
          OciTestProperty.OCI_CONFIG_FILE.name(),
          AuthenticationDetailsFactory.CONFIG_FILE_PATH,
          getOrAbort(OciTestProperty.OCI_CONFIG_FILE))
        .add(
          OciTestProperty.OCI_CONFIG_PROFILE.name(),
          AuthenticationDetailsFactory.CONFIG_PROFILE,
          getOrAbort(OciTestProperty.OCI_CONFIG_PROFILE))
        .build());
  }

  /**
   * Verifies partial API_KEY credentials fallback to config file.
   */
  @Test
  public void testApiKeyPartialCredentials() {
    verifyAuthenticationDetails(
      buildParameterSet(AuthenticationMethod.API_KEY)
        .add("Test OCI_USER", AuthenticationDetailsFactory.USER_ID, "dummy-user-id")
        .build());
  }

  /**
   * Verifies {@link AuthenticationMethod#CLOUD_SHELL}
   */
  @Test
  public void testCloudShell() {
    TestProperties.abortIfNotEqual(OciTestProperty.OCI_CLOUD_SHELL, "true");
    verifyAuthenticationDetails(
      buildParameterSet(AuthenticationMethod.CLOUD_SHELL).build());
  }

  /**
   * Verifies {@link AuthenticationMethod#INSTANCE_PRINCIPAL}
   */
  @Test
  public void testInstancePrincipal() {
    TestProperties.abortIfNotEqual(OciTestProperty.OCI_INSTANCE_PRINCIPAL, "true");
    verifyAuthenticationDetails(
      buildParameterSet(AuthenticationMethod.INSTANCE_PRINCIPAL).build());
  }

  /**
   * Verifies {@link AuthenticationMethod#RESOURCE_PRINCIPAL}
   */
  @Test
  public void testResourcePrincipal() {
    TestProperties.abortIfNotEqual(OciTestProperty.OCI_RESOURCE_PRINCIPAL, "true");
    verifyAuthenticationDetails(
      buildParameterSet(AuthenticationMethod.RESOURCE_PRINCIPAL).build());
  }

  /**
   * Verifies {@link AuthenticationMethod#INTERACTIVE}
   */
  @Test
  public void testResourceInteractive() {
    TestProperties.abortIfNotEqual(OciTestProperty.OCI_INTERACTIVE, "true");
    verifyAuthenticationDetails(
            buildParameterSet(AuthenticationMethod.INTERACTIVE).build());
  }

  /**
   * Returns a parameter set builder pre-configured with a given
   * {@code authenticationMethod}
   */
  private static ParameterSetBuilder buildParameterSet(
    AuthenticationMethod authenticationMethod) {
    return ParameterSet.builder()
      .add(
        AuthenticationDetailsFactoryTest.class.getName() +
          " : AuthenticationMethod",
        AuthenticationDetailsFactory.AUTHENTICATION_METHOD,
        authenticationMethod);
  }

  private static void verifyAuthenticationDetails(ParameterSet parameterSet) {
    Resource<AbstractAuthenticationDetailsProvider> authenticationResource =
      AuthenticationDetailsFactory.getInstance()
        .request(parameterSet);
    assertNotNull(authenticationResource);
    assertTrue(authenticationResource.isSensitive());
    assertTrue(authenticationResource.isValid());

    AbstractAuthenticationDetailsProvider authenticationDetails =
      authenticationResource.getContent();

    // TODO: Add additional verifications if possible
    assertNotNull(authenticationDetails);
  }
}
