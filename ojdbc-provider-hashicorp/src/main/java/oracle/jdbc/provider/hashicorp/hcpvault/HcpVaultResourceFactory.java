package oracle.jdbc.provider.hashicorp.hcpvault;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.hcpvault.authentication.HcpVaultCredentials;
import oracle.jdbc.provider.hashicorp.hcpvault.authentication.HcpVaultCredentialsFactory;
import oracle.jdbc.provider.parameter.ParameterSet;

public abstract class HcpVaultResourceFactory<T> implements ResourceFactory<T> {

  @Override
  public final Resource<T> request(ParameterSet parameterSet) {
    // Retrieve the HCP credentials (token) from the credentials factory
    HcpVaultCredentials credentials = HcpVaultCredentialsFactory
            .getInstance()
            .request(parameterSet)
            .getContent();

    try {
      return request(credentials, parameterSet);
    } catch (Exception e) {
      throw new IllegalStateException(
              "Request failed with parameters: " + parameterSet, e);
    }
  }

  public abstract Resource<T> request(
          HcpVaultCredentials credentials, ParameterSet parameterSet);
}
