package oracle.jdbc.provider.hashicorp.hcpvault.configuration;

import oracle.jdbc.provider.hashicorp.hcpvault.authentication.HcpVaultAuthenticationMethod;
import oracle.jdbc.provider.hashicorp.hcpvault.authentication.HcpVaultCredentialsFactory;
import oracle.jdbc.provider.parameter.ParameterSetParser;

/**
 * Defines how we parse HCP parameters in the JDBC URL or property sets.
 */
public final class HcpVaultConfigurationParameters {

  private HcpVaultConfigurationParameters() {}

  public static ParameterSetParser.Builder configureBuilder(ParameterSetParser.Builder builder) {
    return builder
            .addParameter(
                    "AUTHENTICATION_METHOD",
                    HcpVaultCredentialsFactory.AUTHENTICATION_METHOD,
                    HcpVaultAuthenticationMethod.CLIENT_CREDENTIALS,
                    HcpVaultConfigurationParameters::parseAuthMethod
            )
            .addParameter(
                    "CLIENT_ID",
                    HcpVaultCredentialsFactory.CLIENT_ID
            )
            .addParameter(
                    "CLIENT_SECRET",
                    HcpVaultCredentialsFactory.CLIENT_SECRET
            );
  }

  private static HcpVaultAuthenticationMethod parseAuthMethod(String value) {
    if ("CLIENT_CREDENTIALS".equalsIgnoreCase(value)) {
      return HcpVaultAuthenticationMethod.CLIENT_CREDENTIALS;
    }
    throw new IllegalArgumentException("Unrecognized HCP auth method: " + value);
  }
}
