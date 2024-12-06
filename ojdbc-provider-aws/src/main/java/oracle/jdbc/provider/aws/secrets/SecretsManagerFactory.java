package oracle.jdbc.provider.aws.secrets;

import oracle.jdbc.provider.aws.AwsResourceFactory;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

public final class SecretsManagerFactory
    extends AwsResourceFactory<String> {

  /** The name of a secret. This is a required parameter. */
  public static final Parameter<String> SECRET_NAME =
      Parameter.create(REQUIRED);

  /** The Region of a secret. This is an optional parameter. */
  public static final Parameter<String> REGION =
      Parameter.create();

  /**
   * The name of the key if the secret contains key-value pairs.
   * This is an optional parameter.
   * */
  public static final Parameter<String> KEY_NAME =
      Parameter.create();

  private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();

  /**
   * The single instance of {@code CachedResourceFactory} for requesting key
   * vault secrets
   */
  private static final ResourceFactory<String> INSTANCE =
      CachedResourceFactory.create(new SecretsManagerFactory());

  private SecretsManagerFactory() { }

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

    String secretName = parameterSet.getRequired(SECRET_NAME);
    String region = parameterSet.getOptional(REGION);
    String key = parameterSet.getOptional(KEY_NAME);

    SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
        .credentialsProvider(() -> awsCredentials);
    if (region != null)
      builder.region(Region.of(region));

    SecretsManagerClient client = builder.build();
    GetSecretValueRequest request = GetSecretValueRequest.builder()
        .secretId(secretName).build();
    GetSecretValueResponse response = client.getSecretValue(request);

    String secretString = response.secretString();
    if (key != null) {
      // If key is provided, assume the secret contains key-value pairs
      try {
        try (InputStream secretInputStream =
                 new ByteArrayInputStream(secretString.getBytes(UTF_8))) {

          OracleJsonObject secretJsonObject = JSON_FACTORY
              .createJsonTextValue(secretInputStream)
              .asJsonObject();

          if (secretJsonObject.containsKey(key)) {
            secretString = secretJsonObject.getString(key);
          } else {
            throw new IllegalArgumentException(
                "Failed to find key \"" + key + "\" in " + secretName);
          }
        }
      } catch (IOException ioException) {
        throw new IllegalArgumentException(
            "Failed to read Secret: " + secretName, ioException);
      }
    }

    return Resource.createPermanentResource(secretString, true);
  }
}