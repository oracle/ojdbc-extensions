package oracle.provider.aws.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.ConnectionStringProvider;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class ParameterStoreConnectionStringProviderTest {
  private static final ConnectionStringProvider PROVIDER =
          findProvider(ConnectionStringProvider.class, "ojdbc-provider-aws-parameterstore-tnsnames");

  @Test
  public void testGetParameters() {
    Collection<? extends Parameter> parameters = PROVIDER.getParameters();
    assertNotNull(parameters);

    Parameter secretName = parameters.stream()
            .filter(p -> "parameterName".equals(p.name()))
            .findFirst().orElseThrow(AssertionError::new);
    assertFalse(secretName.isSensitive());
    assertTrue(secretName.isRequired());
    assertNull(secretName.defaultValue());

    Parameter tnsAlias = parameters.stream()
            .filter(p -> "tnsAlias".equals(p.name()))
            .findFirst().orElseThrow(AssertionError::new);
    assertTrue(tnsAlias.isSensitive());
    assertTrue(tnsAlias.isRequired());
    assertNull(tnsAlias.defaultValue());

    Parameter regionParam = parameters.stream()
            .filter(p -> "awsRegion".equals(p.name()))
            .findFirst().orElseThrow(AssertionError::new);
    assertFalse(regionParam.isSensitive());
    assertFalse(regionParam.isRequired());
    assertNull(regionParam.defaultValue());
  }

  @Test
  public void testValidAlias() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("parameterName", "base64-tnsnames");
    testParameters.put("tnsAlias","spsbs2pdq7sg7l3o_low");
    testParameters.put("awsRegion","eu-north-1");

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    System.out.println(PROVIDER.getConnectionString(parameterValues));

    assertNotNull(PROVIDER.getConnectionString(parameterValues));
  }

//  @Test
//  public void testInvalidAlias() {
//    Map<String, String> testParameters = new HashMap<>();
//    testParameters.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.TNSNAMES_SECRET_NAME));
//    testParameters.put("tnsAlias", "DOES_NOT_EXIST");
//    testParameters.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//
//    Map<Parameter, CharSequence> parameterValues =
//            createParameterValues(PROVIDER, testParameters);
//
//    assertThrows(IllegalArgumentException.class, () ->
//            PROVIDER.getConnectionString(parameterValues));
//  }
//
//  @Test
//  public void testValidAliasWithEmptyFieldNameOnPlainTextSecret() {
//    Map<String, String> testParameters = new HashMap<>();
//    testParameters.put("secretName",
//            TestProperties.getOrAbort(AwsTestProperty.TNSNAMES_SECRET_NAME));
//    testParameters.put("tnsAlias",
//            TestProperties.getOrAbort(AwsTestProperty.TNS_ALIAS));
//    testParameters.put("awsRegion",
//            TestProperties.getOrAbort(AwsTestProperty.AWS_REGION));
//    testParameters.put("fieldName", "");
//
//    Map<Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, testParameters);
//    assertNotNull(PROVIDER.getConnectionString(parameterValues));
//  }
}
