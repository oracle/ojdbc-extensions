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

package oracle.provider.aws.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import oracle.jdbc.spi.TlsConfigurationProvider;
import oracle.provider.aws.AwsTestProperty;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class SecretsManagerTcpsProviderTest {

  private static final TlsConfigurationProvider PROVIDER =
          findProvider(TlsConfigurationProvider.class, "ojdbc-provider-aws-secrets-manager-tls");

  /**
   * Verifies that {@link SecretsManagerTcpsProviderTest} parameters include
   * secretName, walletPassword, type, and awsRegion.
   */
  @Test
  public void testGetParameters() {
    Collection<? extends Parameter> parameters = PROVIDER.getParameters();
    assertNotNull(parameters);

    Parameter secretName = parameters.stream()
      .filter(p -> "secretName".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertFalse(secretName.isSensitive());
    assertTrue(secretName.isRequired());

    Parameter type = parameters.stream()
      .filter(p -> "type".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertFalse(type.isSensitive());
    assertTrue(type.isRequired());

    Parameter walletPassword = parameters.stream()
      .filter(p -> "walletPassword".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertTrue(walletPassword.isSensitive());
    assertFalse(walletPassword.isRequired());

    Parameter region = parameters.stream()
      .filter(p -> "awsRegion".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertFalse(region.isSensitive());
    assertFalse(region.isRequired());

    Parameter fieldParam = parameters.stream()
      .filter(p -> "fieldName".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertFalse(fieldParam.isSensitive());
    assertFalse(fieldParam.isRequired());
    assertNull(fieldParam.defaultValue());
  }


  @Test
  public void testPKCS12Wallet() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PKCS12_WALLET_SECRET_NAME));
    testParameters.put("walletPassword",
      TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
    testParameters.put("type", "PKCS12");
    testParameters.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext context = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(context);
  }

  @Test
  public void testSSOWallet() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.SSO_WALLET_SECRET_NAME));
    testParameters.put("type", "SSO");
    testParameters.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext context = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(context);
  }

  @Test
  public void testPemWallet() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PEM_WALLET_SECRET_NAME));
    testParameters.put("type", "PEM");
    testParameters.put("walletPassword",
      TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
    testParameters.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext context = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(context);
  }

  @Test
  public void testMissingPasswordPKCS12() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PKCS12_WALLET_SECRET_NAME));
    testParameters.put("type", "PKCS12");
    testParameters.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    assertThrows(IllegalStateException.class, () ->
            PROVIDER.getSSLContext(parameterValues));
  }

  @Test
  public void testMissingPasswordPem() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PEM_WALLET_SECRET_NAME));
    testParameters.put("type", "PEM");
    testParameters.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    assertThrows(IllegalStateException.class, () ->
            PROVIDER.getSSLContext(parameterValues));
  }

  @Test
  public void testValidTcpsWithEmptyFieldNameOnPlainTextSecret() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PEM_WALLET_SECRET_NAME));
    testParameters.put("walletPassword",
      TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
    testParameters.put("type", "PEM");
    testParameters.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
    testParameters.put("fieldName", "");

    Map<Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, testParameters);
    assertNotNull(PROVIDER.getSSLContext(parameterValues));
  }

}
