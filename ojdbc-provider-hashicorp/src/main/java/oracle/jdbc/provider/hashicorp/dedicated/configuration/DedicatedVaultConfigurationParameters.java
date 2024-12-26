package oracle.jdbc.provider.hashicorp.dedicated.configuration;

import oracle.jdbc.provider.hashicorp.dedicated.authentication.DedicatedVaultAuthenticationMethod;
import oracle.jdbc.provider.hashicorp.dedicated.authentication.DedicatedVaultCredentialsFactory;
import oracle.jdbc.provider.parameter.ParameterSetParser;

/**
 * Defines how we parse common Vault parameters.
 */
public final class DedicatedVaultConfigurationParameters {

  private DedicatedVaultConfigurationParameters() {}

  public static ParameterSetParser.Builder configureBuilder(ParameterSetParser.Builder builder) {
    return builder.addParameter(
            // The parameter name is "AUTHENTICATION"
            "AUTHENTICATION",
            // Tied to HashicorpCredentialsFactory.AUTHENTICATION_METHOD
            DedicatedVaultCredentialsFactory.AUTHENTICATION_METHOD,
            // Default value if none is specified:
            DedicatedVaultAuthenticationMethod.TOKEN,
            DedicatedVaultConfigurationParameters::parseAuthentication)
            ;
  }

  private static DedicatedVaultAuthenticationMethod parseAuthentication(String value) {
    if ("TOKEN".equalsIgnoreCase(value) || "VAULT_TOKEN".equalsIgnoreCase(value)) {
      return DedicatedVaultAuthenticationMethod.TOKEN;
    }
    throw new IllegalArgumentException(
            "Unrecognized Hashicorp authentication value: " + value);
  }
}
