package oracle.jdbc.provider.hashicorp.authentication;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.secrets.HashiVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * A factory for creating {@link HashiCredentials} objects for Vault.
 */
public final class HashicorpCredentialsFactory implements ResourceFactory<HashiCredentials> {

  public static final Parameter<HashicorpAuthenticationMethod> AUTHENTICATION_METHOD =
          Parameter.create(REQUIRED);

  private static final HashicorpCredentialsFactory INSTANCE =
          new HashicorpCredentialsFactory();

  private HashicorpCredentialsFactory() { }

  public static HashicorpCredentialsFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<HashiCredentials> request(ParameterSet parameterSet) {
    HashiCredentials credentials = getCredential(parameterSet);
    return Resource.createPermanentResource(credentials, true);
  }

  private static HashiCredentials getCredential(ParameterSet parameterSet) {
    // Check which authentication method is requested
    HashicorpAuthenticationMethod method =
            parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (method) {
      case TOKEN:
        return tokenCredentials(parameterSet);
      default:
        throw new IllegalArgumentException(
                "Unrecognized authentication method: " + method);
    }
  }

  private static HashiCredentials tokenCredentials(ParameterSet parameterSet) {
    // (1) Try parameter
    String paramToken = parameterSet.getOptional(HashiVaultSecretsManagerFactory.VAULT_TOKEN);
    if (paramToken != null && !paramToken.isEmpty()) {
      return new HashiCredentials(paramToken);
    }

    // (2) Fallback to system property or env
    String vaultToken = System.getProperty("VAULT_TOKEN", System.getenv("VAULT_TOKEN"));
    if (vaultToken == null || vaultToken.isEmpty()) {
      throw new IllegalStateException("Vault token not provided in tokenCredentials()");
    }

    return new HashiCredentials(vaultToken);
  }

}
