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

import com.azure.core.util.Configuration;
import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.resource.ResourceProviderTestUtil;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.spi.AccessTokenProvider;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod;

import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static oracle.jdbc.provider.TestProperties.abortIfNotEqual;
import static oracle.jdbc.provider.TestProperties.getOrAbort;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies {@link AzureTokenProvider} as
 * implementing the behavior specified by its JavaDoc
 */
public class AzureTokenProviderTest {

  private static final AccessTokenProvider PROVIDER =
    findProvider(AccessTokenProvider.class, "ojdbc-provider-azure-token");

  /**
   * Verifies {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * with a client secret.
   */
  @Test
  public void testServicePrincipleSecret() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "service-principal");
    testParameters.put("tenantId", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_TENANT_ID));
    testParameters.put("clientId", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_CLIENT_ID));
    testParameters.put("clientSecret", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_CLIENT_SECRET));
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * using environment variables to configure a client secret.
   */
  @Test
  public void testServicePrincipleSecretEnv() {
    AzureTestProperty.abortIfEnvNotConfigured(
      Configuration.PROPERTY_AZURE_TENANT_ID,
      Configuration.PROPERTY_AZURE_CLIENT_ID,
      Configuration.PROPERTY_AZURE_CLIENT_SECRET);

    Map<String, CharSequence> testParameters = new HashMap<>();

    testParameters.put("authenticationMethod", "service-principal");
    verifyAccessToken(testParameters);

    // Expect auto-detect to recognize the environment variables
    testParameters.put("authenticationMethod", "auto-detect");
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   */
  @Test
  public void testServicePrincipleCertificate() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "service-principal");
    testParameters.put("tenantId", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_TENANT_ID));
    testParameters.put("clientId", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_CLIENT_ID));
    testParameters.put("clientCertificatePath",
      TestProperties.getOrAbort(
        AzureTestProperty.AZURE_CLIENT_CERTIFICATE_PATH));
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   * using environment variables to configure a client certificate.
   */
  @Test
  public void testServicePrincipleCertificateEnv() {
    AzureTestProperty.abortIfEnvNotConfigured(
      Configuration.PROPERTY_AZURE_TENANT_ID,
      Configuration.PROPERTY_AZURE_CLIENT_ID,
      Configuration.PROPERTY_AZURE_CLIENT_CERTIFICATE_PATH);

    Map<String, CharSequence> testParameters = new HashMap<>();

    testParameters.put("authenticationMethod", "service-principal");
    verifyAccessToken(testParameters);

    // Expect auto-detect to recognize the environment variables
    testParameters.put("authenticationMethod", "auto-detect");
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies {@link AzureAuthenticationMethod#SERVICE_PRINCIPLE}
   */
  @Test
  public void testServicePrinciplePfxCertificate() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "service-principal");
    testParameters.put("tenantId", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_TENANT_ID));
    testParameters.put("clientId", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_CLIENT_ID));
    testParameters.put("clientCertificatePath",
      TestProperties.getOrAbort(
        AzureTestProperty.AZURE_CLIENT_PFX_CERTIFICATE_PATH));
    testParameters.put("clientCertificatePassword",
      TestProperties.getOrAbort(AzureTestProperty.AZURE_CLIENT_PFX_PASSWORD));
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies {@link AzureAuthenticationMethod#PASSWORD}
   */
  @Test
  public void testPassword() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "password");
    testParameters.put("tenantId", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_TENANT_ID));
    testParameters.put("clientId", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_CLIENT_ID));
    testParameters.put("username", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_USERNAME));
    testParameters.put("password", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_PASSWORD));
    verifyAccessToken(testParameters);
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

    Map<String, CharSequence> testParameters = new HashMap<>();

    testParameters.put("authenticationMethod", "password");
    verifyAccessToken(testParameters);

    // Expect auto-detect to recognize the environment variables
    testParameters.put("authenticationMethod", "auto-detect");
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies authenticationMethod=managed-identity. This test must be run
   * in an Azure hosted environment (a virtual machine hosted by Azure) which
   * has a system assigned managed identity.
   */
  @Test
  public void testManagedIdentity() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    TestProperties.abortIfNotEqual(AzureTestProperty.AZURE_MANAGED_IDENTITY, "true");
    testParameters.put("authenticationMethod", "managed-identity");
    verifyAccessToken(testParameters);
  }

  /**
   * Verifies {@link AzureAuthenticationMethod#AUTO_DETECT}
   */
  @Test
  public void testAutoDetect() {
    Map<String, CharSequence> testParameters = new HashMap<>();

    testParameters.put("authenticationMethod", "auto-detect");

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

    verifyAccessToken(testParameters);
  }

  /** Verifies an access token returned for a URI with the given parameters */
  private static void verifyAccessToken(
    Map<String, CharSequence> testParameters) {

    Map<Parameter, CharSequence> parameterValues =
      createParameters(testParameters);

    AccessToken accessToken = PROVIDER.getAccessToken(parameterValues);

    Assertions.assertNotNull(accessToken);
  }

  private static Map<Parameter, CharSequence> createParameters(
    Map<String, CharSequence> testParameters) {

    // Add default values for all tests
    testParameters.putIfAbsent("scope", TestProperties.getOrAbort(
      AzureTestProperty.AZURE_TOKEN_SCOPE));

    return ResourceProviderTestUtil.createParameterValues(PROVIDER, testParameters);
  }
}
