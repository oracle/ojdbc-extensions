package oracle.jdbc.provider.hashicorp.hcpvault.authentication;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

/**
 * A factory for creating {@link HcpVaultCredentials} objects for HCP.
 */
public final class HcpVaultCredentialsFactory implements ResourceFactory<HcpVaultCredentials> {

  // The param that indicates which HCP auth method to use (only one for now).
  public static final Parameter<HcpVaultAuthenticationMethod> AUTHENTICATION_METHOD = Parameter.create(REQUIRED);

  // The OAuth2 client_id and client_secret
  public static final Parameter<String> CLIENT_ID = Parameter.create(REQUIRED);
  public static final Parameter<String> CLIENT_SECRET = Parameter.create(REQUIRED);

  private static final HcpVaultCredentialsFactory INSTANCE = new HcpVaultCredentialsFactory();

  private HcpVaultCredentialsFactory() {}

  public static HcpVaultCredentialsFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<HcpVaultCredentials> request(ParameterSet parameterSet) {
    HcpVaultCredentials credentials = getCredential(parameterSet);
    return Resource.createPermanentResource(credentials, true);
  }

  private HcpVaultCredentials getCredential(ParameterSet parameterSet) {
    HcpVaultAuthenticationMethod method = parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (method) {
      case CLIENT_CREDENTIALS:
        return createClientCredentials(parameterSet);
      default:
        throw new IllegalArgumentException("Unrecognized HCP auth method: " + method);
    }
  }

  private HcpVaultCredentials createClientCredentials(ParameterSet parameterSet) {
    String clientId = getRequiredOrFallback(parameterSet, CLIENT_ID, "CLIENT_ID");
    String clientSecret = getRequiredOrFallback(parameterSet, CLIENT_SECRET, "CLIENT_SECRET");

    // Call OAuth endpoint to fetch the token
    String apiToken = HcpVaultOAuthClient.fetchHcpAccessToken(clientId, clientSecret);
    if (apiToken == null || apiToken.isEmpty()) {
      throw new IllegalStateException("Failed to obtain HCP token using client_credentials flow");
    }

    return new HcpVaultCredentials(apiToken);
  }
}
