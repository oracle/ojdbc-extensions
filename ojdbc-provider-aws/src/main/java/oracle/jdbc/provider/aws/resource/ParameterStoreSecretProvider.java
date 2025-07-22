package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.provider.aws.parameterstore.ParameterStoreFactory;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.ResourceParameterUtils;

import java.util.Map;

import static oracle.jdbc.provider.aws.resource.AwsParameterStoreResourceParameterNames.PARAMETER_NAME;

/**
 * <p>
 * Base class for all providers that retrieve secrets from AWS Parameter Store.
 * This class defines shared parameters and secret retrieval logic.
 * </p>
 */
public abstract class ParameterStoreSecretProvider extends AwsResourceProvider {

  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter(PARAMETER_NAME, ParameterStoreFactory.PARAMETER_NAME)
  };

  protected ParameterStoreSecretProvider(String resourceType) {
    super(resourceType, PARAMETERS);
  }

  protected ParameterStoreSecretProvider(String resourceType, ResourceParameter[] additionalParameters) {
    super(resourceType, ResourceParameterUtils.combineParameters(PARAMETERS, additionalParameters));
  }

  /**
   * <p>
   * Retrieves a secret from AWS Parameter Store based on parameters provided
   * in {@code parameterValues}. This method centralizes secret retrieval logic
   * and is intended to be used by subclasses implementing the
   * {@link oracle.jdbc.spi.OracleResourceProvider} SPI.
   * </p>
   *
   * @param parameterValues A map of parameter names and their corresponding
   * values required for secret retrieval.
   * @return The secret value as a {@code String}.
   * @throws IllegalStateException If secret retrieval fails or returns null.
   */
  protected final String getSecret(Map<Parameter, CharSequence> parameterValues) {
    return getResource(ParameterStoreFactory.getInstance(), parameterValues);
  }
}
