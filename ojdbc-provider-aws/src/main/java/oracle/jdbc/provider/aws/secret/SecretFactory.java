package oracle.jdbc.provider.aws.secret;

import oracle.jdbc.provider.aws.AwsResourceFactory;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

public final class SecretFactory
    extends AwsResourceFactory<String> {

  /** The Identifier of a secret. This is a required parameter. */
  public static final Parameter<String> SECRET_ID =
      Parameter.create(REQUIRED);

  /** The Region of a secret. This is an optional parameter. */
  public static final Parameter<String> REGION =
      Parameter.create();

  /**
   * The single instance of {@code CachedResourceFactory} for requesting key
   * vault secrets
   */
  private static final ResourceFactory<String> INSTANCE =
      CachedResourceFactory.create(new SecretFactory());

  private SecretFactory() { }

  /**
   * Returns a singleton of {@code KeyVaultSecretFactory}.
   * @return a singleton of {@code KeyVaultSecretFactory}
   */
  public static ResourceFactory<String> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<String> request(
      AwsCredentials awsCredentials, ParameterSet parameterSet) {

    String secretId = parameterSet.getRequired(SECRET_ID);
    String region = parameterSet.getOptional(REGION);

    SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
        .credentialsProvider(() -> awsCredentials);
    if (region != null)
      builder.region(Region.of(region));

    SecretsManagerClient client = builder.build();
    GetSecretValueRequest request = GetSecretValueRequest.builder()
        .secretId(secretId).build();
    GetSecretValueResponse response = client.getSecretValue(request);

    return Resource.createPermanentResource(response.secretString(), true);
  }
}