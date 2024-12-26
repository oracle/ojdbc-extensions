package oracle.jdbc.provider.hashicorp.dedicated.authentication;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.dedicated.secrets.DedicatedVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

/**
 * A factory for creating {@link DedicatedVaultCredentials} objects for Dedicated Vault.
 */
public final class DedicatedVaultCredentialsFactory implements ResourceFactory<DedicatedVaultCredentials> {

  public static final Parameter<DedicatedVaultAuthenticationMethod> AUTHENTICATION_METHOD =
          Parameter.create(REQUIRED);

  private static final DedicatedVaultCredentialsFactory INSTANCE =
          new DedicatedVaultCredentialsFactory();

  private DedicatedVaultCredentialsFactory() { }

  public static DedicatedVaultCredentialsFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<DedicatedVaultCredentials> request(ParameterSet parameterSet) {
    DedicatedVaultCredentials credentials = getCredential(parameterSet);
    return Resource.createPermanentResource(credentials, true);
  }

  private static DedicatedVaultCredentials getCredential(ParameterSet parameterSet) {
    // Check which authentication method is requested
    DedicatedVaultAuthenticationMethod method =
            parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (method) {
      case TOKEN:
        return createTokenCredentials(parameterSet);
      default:
        throw new IllegalArgumentException(
                "Unrecognized authentication method: " + method);
    }
  }

  private static DedicatedVaultCredentials createTokenCredentials(ParameterSet parameterSet) {
    String vaultToken = getRequiredOrFallback(
            parameterSet,
            DedicatedVaultSecretsManagerFactory.VAULT_TOKEN,
            "VAULT_TOKEN"
    );

    if (vaultToken == null || vaultToken.isEmpty()) {
      throw new IllegalStateException("Vault Token not found in parameters, " +
              "system properties, or environment variables");
    }

    return new DedicatedVaultCredentials(vaultToken);
  }

}
