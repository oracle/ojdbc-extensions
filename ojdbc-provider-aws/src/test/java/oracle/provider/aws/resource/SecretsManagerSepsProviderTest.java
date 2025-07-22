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
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.spi.UsernameProvider;
import oracle.provider.aws.AwsTestProperty;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class SecretsManagerSepsProviderTest {

  private static final UsernameProvider USERNAME_PROVIDER =
    findProvider(UsernameProvider.class, "ojdbc-provider-aws-secrets-manager-seps");

  private static final PasswordProvider PASSWORD_PROVIDER =
    findProvider(PasswordProvider.class, "ojdbc-provider-aws-secrets-manager-seps");


  /**
   * Verifies that {@link SecretsManagerSepsProviderTest} parameters include
   * secretName, walletPassword, connectionStringIndex, and awsRegion.
   */
  @Test
  public void testGetParameters() {
    Collection<? extends Parameter> usernameParams = USERNAME_PROVIDER.getParameters();
    Collection<? extends Parameter> passwordParams = PASSWORD_PROVIDER.getParameters();

    assertEquals(usernameParams, passwordParams, "Username and Password providers should expose same parameters");

    assertNotNull(usernameParams);
    assertNotNull(passwordParams);

    Parameter secretName = usernameParams.stream()
      .filter(p -> "secretName".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertFalse(secretName.isSensitive());
    assertTrue(secretName.isRequired());

    Parameter walletPassword = usernameParams.stream()
      .filter(p -> "walletPassword".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertTrue(walletPassword.isSensitive());
    assertFalse(walletPassword.isRequired());

    Parameter connIndex = usernameParams.stream()
      .filter(p -> "connectionStringIndex".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertFalse(connIndex.isSensitive());
    assertFalse(connIndex.isRequired());

    Parameter awsRegion = usernameParams.stream()
      .filter(p -> "awsRegion".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertFalse(awsRegion.isSensitive());
    assertFalse(awsRegion.isRequired());

    Parameter fieldParam = usernameParams.stream()
      .filter(p -> "fieldName".equals(p.name()))
      .findFirst().orElseThrow(AssertionError::new);
    assertFalse(fieldParam.isSensitive());
    assertFalse(fieldParam.isRequired());
    assertNull(fieldParam.defaultValue());
  }

  @Test
  public void testPkcs12PasswordWithIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
    params.put("walletPassword",
      TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
    params.put("connectionStringIndex", "1");
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
    assertNotNull(PASSWORD_PROVIDER.getPassword(values));
  }

  @Test
  public void testPkcs12PasswordWithoutIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
    params.put("walletPassword",
      TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
    assertNotNull(PASSWORD_PROVIDER.getPassword(values));
  }

  @Test
  public void testSsoPasswordWithIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
    params.put("connectionStringIndex", "1");
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
    params.put("fieldName", "sso");

    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
    assertNotNull(PASSWORD_PROVIDER.getPassword(values));
  }

  @Test
  public void testSsoPasswordWithoutIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
    assertNotNull(PASSWORD_PROVIDER.getPassword(values));
  }

  @Test
  public void testPkcs12UsernameWithIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
    params.put("walletPassword",
      TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
    params.put("connectionStringIndex", "1");
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
    assertNotNull(USERNAME_PROVIDER.getUsername(values));
  }

  @Test
  public void testPkcs12UsernameWithoutIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
    params.put("walletPassword",
      TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
    assertNotNull(USERNAME_PROVIDER.getUsername(values));
  }

  @Test
  public void testSsoUsernameWithIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
    params.put("connectionStringIndex", "1");
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
    assertNotNull(USERNAME_PROVIDER.getUsername(values));
  }

  @Test
  public void testSsoUsernameWithoutIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
    assertNotNull(USERNAME_PROVIDER.getUsername(values));
  }

  @Test
  public void testPkcs12MissingPassword() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
    params.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));

    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
    assertThrows(IllegalStateException.class, () -> PASSWORD_PROVIDER.getPassword(values));
  }

  @Test
  public void testValidSepsWithEmptyFieldNameOnPlainTextSecret() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretName",
      TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
    testParameters.put("awsRegion",
      TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
    testParameters.put("fieldName", "");

    Map<Parameter, CharSequence> parameterValues = createParameterValues(USERNAME_PROVIDER,
            testParameters);
    assertNotNull(USERNAME_PROVIDER.getUsername(parameterValues));
  }


}
