package oracle.provider.aws.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.spi.UsernameProvider;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class SecretsManagerSepsProviderTest {
  private static final UsernameProvider USERNAME_PROVIDER =
          findProvider(UsernameProvider.class, "ojdbc-provider-aws-parameterStore-seps");

  private static final PasswordProvider PASSWORD_PROVIDER =
          findProvider(PasswordProvider.class, "ojdbc-provider-aws-parameterStore-seps");


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
            .filter(p -> "parameterName".equals(p.name()))
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
  }

  @Test
  public void testPkcs12PasswordWithIndex() {
    Map<String, String> params = new HashMap<>();
    params.put("parameterName", "test-seps-p12");
    params.put("walletPassword", "Iczg5944$");
//    params.put("connectionStringIndex", "1");
    params.put("awsRegion","eu-north-1");

    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
    System.out.println(PASSWORD_PROVIDER.getPassword(values));
    assertNotNull(PASSWORD_PROVIDER.getPassword(values));
  }

//  @Test
//  public void testPkcs12PasswordWithoutIndex() {
//    Map<String, String> params = new HashMap<>();
//    params.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
//    params.put("walletPassword",
//            TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
//    params.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//
//    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
//    assertNotNull(PASSWORD_PROVIDER.getPassword(values));
//  }
//
//  @Test
//  public void testSsoPasswordWithIndex() {
//    Map<String, String> params = new HashMap<>();
//    params.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
//    params.put("connectionStringIndex", "1");
//    params.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//    params.put("fieldName", "sso");
//
//    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
//    assertNotNull(PASSWORD_PROVIDER.getPassword(values));
//  }
//
//  @Test
//  public void testSsoPasswordWithoutIndex() {
//    Map<String, String> params = new HashMap<>();
//    params.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
//    params.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//
//    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
//    assertNotNull(PASSWORD_PROVIDER.getPassword(values));
//  }
//
//  @Test
//  public void testPkcs12UsernameWithIndex() {
//    Map<String, String> params = new HashMap<>();
//    params.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
//    params.put("walletPassword",
//            TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
//    params.put("connectionStringIndex", "1");
//    params.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//
//    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
//    assertNotNull(USERNAME_PROVIDER.getUsername(values));
//  }
//
//  @Test
//  public void testPkcs12UsernameWithoutIndex() {
//    Map<String, String> params = new HashMap<>();
//    params.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
//    params.put("walletPassword",
//            TestProperties.getOrAbort(AwsTestProperty.WALLET_PASSWORD));
//    params.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//
//    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
//    assertNotNull(USERNAME_PROVIDER.getUsername(values));
//  }
//
//  @Test
//  public void testSsoUsernameWithIndex() {
//    Map<String, String> params = new HashMap<>();
//    params.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
//    params.put("connectionStringIndex", "1");
//    params.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//
//    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
//    assertNotNull(USERNAME_PROVIDER.getUsername(values));
//  }
//
//  @Test
//  public void testSsoUsernameWithoutIndex() {
//    Map<String, String> params = new HashMap<>();
//    params.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
//    params.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//
//    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
//    assertNotNull(USERNAME_PROVIDER.getUsername(values));
//  }
//
//  @Test
//  public void testPkcs12MissingPassword() {
//    Map<String, String> params = new HashMap<>();
//    params.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.PKCS12_SEPS_WALLET_SECRET_NAME));
//    params.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//
//    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
//    assertThrows(IllegalStateException.class, () -> PASSWORD_PROVIDER.getPassword(values));
//  }
//
//  @Test
//  public void testValidSepsWithEmptyFieldNameOnPlainTextSecret() {
//    Map<String, String> testParameters = new HashMap<>();
//    testParameters.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.SSO_SEPS_WALLET_SECRET_NAME));
//    testParameters.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//    testParameters.put("fieldName", "");
//
//    Map<Parameter, CharSequence> parameterValues = createParameterValues(USERNAME_PROVIDER,
//            testParameters);
//    assertNotNull(USERNAME_PROVIDER.getUsername(parameterValues));
//  }
}
