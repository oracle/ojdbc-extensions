package oracle.jdbc.provider.hashicorp.hcpvault.secrets;

import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.hcpvault.HcpVaultResourceFactory;
import oracle.jdbc.provider.hashicorp.hcpvault.authentication.HcpVaultCredentials;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;


public final class HcpVaultSecretsManagerFactory extends HcpVaultResourceFactory<String> {

  // The org / project / app for the secrets:open call
  public static final Parameter<String> ORG_ID      = Parameter.create(REQUIRED);
  public static final Parameter<String> PROJECT_ID  = Parameter.create(REQUIRED);
  public static final Parameter<String> APP_NAME    = Parameter.create(REQUIRED);
  public static final Parameter<String> SECRET_NAME = Parameter.create(REQUIRED);
  public static final Parameter<String> KEY = Parameter.create();

  private static final ResourceFactory<String> INSTANCE =
          CachedResourceFactory.create(new HcpVaultSecretsManagerFactory());

  private HcpVaultSecretsManagerFactory() {}

  public static ResourceFactory<String> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<String> request(HcpVaultCredentials credentials, ParameterSet parameterSet) {
    String orgId = getRequiredOrFallback(parameterSet, ORG_ID, "ORG_ID");
    String projectId = getRequiredOrFallback(parameterSet, PROJECT_ID, "PROJECT_ID");
    String appName = getRequiredOrFallback(parameterSet, APP_NAME, "APP_NAME");
    String secretName = getRequiredOrFallback(parameterSet, SECRET_NAME, "SECRET_NAME");
    String key = parameterSet.getOptional(KEY);


    // Build the URL
    String hcpUrl = String.format(
            "https://api.cloud.hashicorp" +
                    ".com/secrets/2023-11-28/organizations/%s/projects/%s" +
                    "/apps/%s/secrets/%s:open",
            orgId, projectId , appName, secretName
    );

    String secretsJson = HcpVaultApiClient.fetchSecrets(hcpUrl, credentials.getHcpApiToken());

    // If a KEY is specified, extract it from the JSON
    if (key != null) {
      return Resource.createPermanentResource(HcpVaultApiClient.extractKeyFromJson(secretsJson
              , key), true);
    }
    return Resource.createPermanentResource(secretsJson, true);
  }


}
