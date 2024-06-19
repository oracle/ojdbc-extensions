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

package oracle.jdbc.provider.azure.authentication;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.util.Configuration;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetBuilder;
import org.junit.jupiter.api.Test;

import static oracle.jdbc.provider.TestProperties.getOrAbort;
import static oracle.jdbc.provider.azure.AzureTestProperty.AZURE_TOKEN_SCOPE;
import static oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod.*;
import static oracle.jdbc.provider.azure.authentication.TokenCredentialFactory.AUTHENTICATION_METHOD;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TokenCredentialFactoryTest {

  /**
   * Verifies
   * {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * using a client secret for authentication.
   */
  @Test
  public void testServicePrincipalSecret() {
    verifyTokenCredential(
      buildParameterSet(SERVICE_PRINCIPLE)
        .add(
          AzureTestProperty.AZURE_CLIENT_SECRET.name(),
          TokenCredentialFactory.CLIENT_SECRET,
          getOrAbort(AzureTestProperty.AZURE_CLIENT_SECRET))
        .build());
  }

  /**
   * Verifies
   * {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * using environment variables to configure a client secret for
   * authentication.
   */
  @Test
  public void testServicePrincipalSecretEnv() {
    AzureTestProperty.abortIfEnvNotConfigured(
      Configuration.PROPERTY_AZURE_TENANT_ID,
      Configuration.PROPERTY_AZURE_CLIENT_ID,
      Configuration.PROPERTY_AZURE_CLIENT_SECRET);

    verifyTokenCredential(ParameterSet.builder()
      .add(
        "Test Authentication Method", AUTHENTICATION_METHOD, SERVICE_PRINCIPLE)
      .build());

    // Expect auto-detect to recognize the environment variables
    verifyTokenCredential(ParameterSet.builder()
      .add(
        "Test Authentication Method", AUTHENTICATION_METHOD, AUTO_DETECT)
      .build());
  }


  /**
   * Verifies
   * {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * using a client certificate for authentication.
   */
  @Test
  public void testServicePrincipalCertificate() {
    verifyTokenCredential(
      buildParameterSet(SERVICE_PRINCIPLE)
        .add(
          AzureTestProperty.AZURE_CLIENT_CERTIFICATE_PATH.name(),
          TokenCredentialFactory.CLIENT_CERTIFICATE_PATH,
          getOrAbort(AzureTestProperty.AZURE_CLIENT_CERTIFICATE_PATH))
        .build());
  }

  /**
   * Verifies
   * {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * using environment variables to configure a client certificate for
   * authentication.
   */
  @Test
  public void testServicePrincipalCertificateEnv() {
    AzureTestProperty.abortIfEnvNotConfigured(
      Configuration.PROPERTY_AZURE_TENANT_ID,
      Configuration.PROPERTY_AZURE_CLIENT_ID,
      Configuration.PROPERTY_AZURE_CLIENT_CERTIFICATE_PATH);

    verifyTokenCredential(ParameterSet.builder()
      .add(
        "Test Authentication Method", AUTHENTICATION_METHOD, SERVICE_PRINCIPLE)
      .build());

    // Expect auto-detect to recognize the environment variables
    verifyTokenCredential(ParameterSet.builder()
      .add(
        "Test Authentication Method", AUTHENTICATION_METHOD, AUTO_DETECT)
      .build());
  }

  /**
   * Verifies
   * {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * using a PFX client certificate for authentication.
   *
   * Note: There are no SDK environment variables to configure a PFX certificate
   * and password, so there is no corresponding test for that.
   */
  @Test
  public void testServicePrincipalPfxCertificate() {
    verifyTokenCredential(
      buildParameterSet(SERVICE_PRINCIPLE)
        .add(
          AzureTestProperty.AZURE_CLIENT_PFX_CERTIFICATE_PATH.name(),
          TokenCredentialFactory.CLIENT_CERTIFICATE_PATH,
          getOrAbort(AzureTestProperty.AZURE_CLIENT_PFX_CERTIFICATE_PATH))
        .add(
          AzureTestProperty.AZURE_CLIENT_PFX_PASSWORD.name(),
          TokenCredentialFactory.CLIENT_CERTIFICATE_PASSWORD,
          getOrAbort(AzureTestProperty.AZURE_CLIENT_PFX_PASSWORD))
        .build());
  }

  /**
   * Verifies
   * {@link AzureAuthenticationMethod#PASSWORD}
   * using the username and password of an Azure account for authentication.
   */
  @Test
  public void testPassword() {
    verifyTokenCredential(
      buildParameterSet(PASSWORD)
        .add(
          AzureTestProperty.AZURE_USERNAME.name(),
          TokenCredentialFactory.USERNAME,
          getOrAbort(AzureTestProperty.AZURE_USERNAME))
        .add(
          AzureTestProperty.AZURE_PASSWORD.name(),
          TokenCredentialFactory.PASSWORD,
          getOrAbort(AzureTestProperty.AZURE_PASSWORD))
        .build());
  }

  /**
   * Verifies
   * {@link AzureAuthenticationMethod#PASSWORD}
   * using environment variables to configure a username and password of an
   * Azure account for authentication.
   */
  @Test
  public void testPasswordEnv() {
    AzureTestProperty.abortIfEnvNotConfigured(
      Configuration.PROPERTY_AZURE_CLIENT_ID,
      Configuration.PROPERTY_AZURE_USERNAME,
      Configuration.PROPERTY_AZURE_PASSWORD);

    verifyTokenCredential(ParameterSet.builder()
      .add(
        "Test Authentication Method", AUTHENTICATION_METHOD, PASSWORD)
      .build());

    // Expect auto-detect to recognize the environment variables
    verifyTokenCredential(ParameterSet.builder()
      .add(
        "Test Authentication Method", AUTHENTICATION_METHOD, AUTO_DETECT)
      .build());
  }

  /**
   * Verifies
   * {@link AzureAuthenticationMethod#MANAGED_IDENTITY}
   * using the identity of an Azure VM for authentication.
   */
  @Test
  public void testManagedIdentity() {
    TestProperties.abortIfNotEqual(
      AzureTestProperty.AZURE_MANAGED_IDENTITY, "true");

    verifyTokenCredential(
      buildParameterSet(MANAGED_IDENTITY).build());
  }

  // TODO: Add tests for interactive and device-code authentication if there is
  //  a way  to automate that.

  /**
   * Returns a {@code ParameterSetBuilder} pre-configured with the tenant and
   * client id from {@link oracle.jdbc.provider.TestProperties}, along with a
   * given authentication method.
   */
  private static ParameterSetBuilder buildParameterSet(
    AzureAuthenticationMethod authenticationMethod) {
    return ParameterSet.builder()
      .add(
        AzureTestProperty.AZURE_TENANT_ID.name(),
        TokenCredentialFactory.TENANT_ID,
        getOrAbort(AzureTestProperty.AZURE_TENANT_ID))
      .add(
        AzureTestProperty.AZURE_CLIENT_ID.name(),
        TokenCredentialFactory.CLIENT_ID,
        getOrAbort(AzureTestProperty.AZURE_CLIENT_ID))
      .add("TokenCredentialFactoryTest.Authentication-Method",
        AUTHENTICATION_METHOD, authenticationMethod);
  }

  /** Verifies the TokenCredential created for a given set of parameters */
  private static void verifyTokenCredential(ParameterSet parameterSet) {
    String tokenScope = getOrAbort(AZURE_TOKEN_SCOPE);

    Resource<TokenCredential> tokenResource =
      TokenCredentialFactory.getInstance()
        .request(parameterSet);

    assertNotNull(tokenResource);
    assertTrue(tokenResource.isSensitive());
    assertTrue(tokenResource.isValid());

    TokenCredential tokenCredential = tokenResource.getContent();
    assertNotNull(tokenCredential);

    TokenRequestContext tokenRequestContext =
      new TokenRequestContext().addScopes(tokenScope);
    AccessToken accessToken =
      tokenCredential.getToken(tokenRequestContext)
        .block();
    assertNotNull(accessToken);
  }

}
