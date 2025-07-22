package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.provider.util.CommonParameters;
import oracle.jdbc.spi.UsernameProvider;
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.WalletUtils;

import java.util.Base64;
import java.util.Map;

import static oracle.jdbc.provider.aws.resource.AwsParameterStoreResourceParameterNames.WALLET_PASSWORD;
import static oracle.jdbc.provider.aws.resource.AwsParameterStoreResourceParameterNames.CONNECTION_STRING_INDEX;
import static oracle.jdbc.provider.util.CommonParameters.PASSWORD;

/**
 * <p>
 * A provider for an Oracle SEPS (Secure External Password Store) wallet stored
 * as a base64-encoded parameter in AWS Parameter Store.
 * </p>
 * <p>
 * This class supports both PKCS12 and SSO wallet formats. It implements the
 * {@link UsernameProvider} and {@link PasswordProvider} SPIs defined by Oracle JDBC.
 * </p>
 * <p>
 * It is designed to be instantiated via {@link java.util.ServiceLoader}.
 * </p>
 */
public class ParameterStoreSEPSProvider
        extends ParameterStoreSecretProvider
        implements UsernameProvider, PasswordProvider {

  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter(WALLET_PASSWORD, PASSWORD),
          new ResourceParameter(CONNECTION_STRING_INDEX, CommonParameters.CONNECTION_STRING_INDEX)
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader}
   * to construct an instance of this provider.
   */
  public ParameterStoreSEPSProvider() {
    super("parameterstore-seps", PARAMETERS);
  }

  @Override
  public String getUsername(Map<Parameter, CharSequence> parameterValues) {
    return getWalletCredentials(parameterValues).username();
  }

  @Override
  public char[] getPassword(Map<Parameter, CharSequence> parameterValues) {
    return getWalletCredentials(parameterValues).password();
  }

  /**
   * Retrieves the OracleWallet by decoding the base64-encoded wallet stored
   * in AWS Parameter Store and opening it as either SSO or PKCS12, based on
   * whether a password is provided.
   */
  private WalletUtils.Credentials getWalletCredentials(Map<Parameter, CharSequence> parameterValues) {
    ParameterSet parameterSet = parseParameterValues(parameterValues);
    String secret = getSecret(parameterValues);
    byte[] walletBytes = Base64.getDecoder().decode(secret);

    char[] walletPassword = parameterSet.getOptional(PASSWORD) != null
            ? parameterSet.getOptional(PASSWORD).toCharArray() : null;

    String connectionStringIndex =
            parameterSet.getOptional(CommonParameters.CONNECTION_STRING_INDEX);
    return WalletUtils.getCredentials(walletBytes, walletPassword, connectionStringIndex);
  }
}
