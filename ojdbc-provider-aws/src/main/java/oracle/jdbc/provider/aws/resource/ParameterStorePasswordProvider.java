package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.spi.PasswordProvider;

import java.util.Map;

/**
 * <p>
 * A provider of password managed as parameters in AWS Systems Manager Parameter Store.
 * This class inherits parameters and behavior from {@link ParameterStoreSecretProvider}
 * and {@link AwsResourceProvider}.
 * </p><p>
 * This class implements the {@link PasswordProvider} SPI defined by Oracle JDBC.
 * It is designed to be located and instantiated by {@link java.util.ServiceLoader}.
 * </p>
 */
public class ParameterStorePasswordProvider
        extends ParameterStoreSecretProvider
        implements PasswordProvider {

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader}
   * to construct an instance of this provider.
   */
  public ParameterStorePasswordProvider() {
    super("parameterstore-password");
  }

  /**
   * Retrieves a password stored in AWS Parameter Store.
   *
   * @param parameterValues A map of parameter names and values required for
   * retrieving the parameter. Must not be null.
   * @return The secret value as a char array. Not null.
   */
  @Override
  public char[] getPassword(Map<Parameter, CharSequence> parameterValues) {
    return getSecret(parameterValues).toCharArray();
  }
}
