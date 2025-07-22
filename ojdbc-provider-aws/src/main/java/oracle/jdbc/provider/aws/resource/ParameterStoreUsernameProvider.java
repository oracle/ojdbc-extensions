package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.spi.UsernameProvider;

import java.util.Map;

/**
 * <p>
 * A provider of username managed as parameters in AWS Systems Manager Parameter Store.
 * This class inherits parameters and behavior from {@link ParameterStoreSecretProvider}
 * and {@link AwsResourceProvider}.
 * </p><p>
 * This class implements the {@link UsernameProvider} SPI defined by Oracle JDBC.
 * It is designed to be located and instantiated by {@link java.util.ServiceLoader}.
 * </p>
 */
public class ParameterStoreUsernameProvider
        extends ParameterStoreSecretProvider
        implements UsernameProvider {

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader}
   * to construct an instance of this provider.
   */
  public ParameterStoreUsernameProvider() {
    super("parameterstore-username");
  }

  /**
   * Retrieves a username stored in AWS Parameter Store.
   *
   * @param parameterValues A map of parameter names and values required for
   * retrieving the parameter. Must not be null.
   * @return The secret value as the database username. Not null.
   */
  @Override
  public String getUsername(Map<Parameter, CharSequence> parameterValues) {
    return getSecret(parameterValues);
  }
}
