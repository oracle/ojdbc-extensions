package oracle.jdbc.provider.hashicorp.configuration;

import oracle.jdbc.provider.hashicorp.authentication.HashicorpAuthenticationMethod;
import oracle.jdbc.provider.hashicorp.authentication.HashicorpCredentialsFactory;
import oracle.jdbc.provider.hashicorp.secrets.HashiVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSetParser;

/**
 * Defines how we parse common Vault parameters (similar to AWS approach).
 */
public final class HashicorpConfigurationParameters {

  private HashicorpConfigurationParameters() {}

  public static ParameterSetParser.Builder configureBuilder(ParameterSetParser.Builder builder) {
    return builder.addParameter(
            // The parameter name is "AUTHENTICATION"
            "AUTHENTICATION",
            // Tied to HashicorpCredentialsFactory.AUTHENTICATION_METHOD
            HashicorpCredentialsFactory.AUTHENTICATION_METHOD,
            // Default value if none is specified:
            HashicorpAuthenticationMethod.TOKEN,
            HashicorpConfigurationParameters::parseAuthentication)
            ;
  }

  private static HashicorpAuthenticationMethod parseAuthentication(String value) {
    // Map user-provided string to enum
    if ("TOKEN".equalsIgnoreCase(value) || "VAULT_TOKEN".equalsIgnoreCase(value)) {
      return HashicorpAuthenticationMethod.TOKEN;
    }
    throw new IllegalArgumentException(
            "Unrecognized Hashicorp authentication value: " + value);
  }
}
