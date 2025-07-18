package oracle.provider.aws.configuration.resource;

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

public class ParameterStoreUsernameProviderTest {

  private static final UsernameProvider PROVIDER =
          findProvider(UsernameProvider.class, "ojdbc-provider-aws-parameterstore-username");

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

    Parameter regionParameter = parameters.stream()
            .filter(parameter -> "awsRegion".equals(parameter.name()))
            .findFirst()
            .orElseThrow(AssertionError::new);
    assertFalse(regionParameter.isSensitive());
    assertFalse(regionParameter.isRequired());
    assertNull(regionParameter.defaultValue());
  }

  @Test
  public void testGetPassword() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("parameterName", "test-password");
    testParameters.put("awsRegion","eu-north-1");

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    System.out.println(PROVIDER.getUsername(parameterValues));

    assertNotNull(PROVIDER.getUsername(parameterValues));
  }
}