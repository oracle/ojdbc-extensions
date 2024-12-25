package oracle.jdbc.provider.hashicorp;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.authentication.HashiCredentials;
import oracle.jdbc.provider.hashicorp.authentication.HashicorpCredentialsFactory;
import oracle.jdbc.provider.parameter.ParameterSet;

/**
 * Common super class for ResourceFactory implementations that request
 * a resource from Vault using HashiCredentials (Vault token).
 */
public abstract class HashiVaultResourceFactory<T> implements ResourceFactory<T> {

  @Override
  public final Resource<T> request(ParameterSet parameterSet) {
    // Retrieve the Vault credentials (token) from the credentials factory
    HashiCredentials credentials = HashicorpCredentialsFactory.getInstance()
            .request(parameterSet)
            .getContent();

    try {
      return request(credentials, parameterSet);
    } catch (Exception e) {
      throw new IllegalStateException(
              "Request failed with parameters: " + parameterSet, e);
    }
  }

  /**
   * Subclasses implement to request the resource from Vault using
   * the given credentials and parameters.
   */
  public abstract Resource<T> request(
          HashiCredentials credentials, ParameterSet parameterSet);
}
