package oracle.jdbc.provider.aws.s3;

import oracle.jdbc.provider.aws.AwsResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.REGION;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

public class S3Factory extends AwsResourceFactory<InputStream> {

  /** The URL of an object stored in S3. This is a required parameter. */
  public static final Parameter<String> S3_URL = Parameter.create(REQUIRED);


  private static final ResourceFactory<InputStream> INSTANCE = new S3Factory();

  private S3Factory() {}

  /**
   * @return a singleton of {@code ObjectFactory}
   */
  public static ResourceFactory<InputStream> getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Requests the content of an Object in {@code InputStream} type from the
   * S3 service.
   * </p><p>
   * The {@code parameterSet} is required to include an {@link #S3_URL}.
   * </p>
   * @param awsCredentials Authentication details configured by the
   * {@code parameterSet}. Not null.
   * @param parameterSet Set of parameters that configure the request. Not null.
   * @return InputStream which represents the content of an Object
   * @throws IllegalArgumentException If the parser cannot find matching strings
   * in the URL Path.
   * @throws IllegalStateException If the S3 URL in {@code parameterSet} cannot
   * be parsed as a URI reference.
   */
  @Override
  public Resource<InputStream> request(
      AwsCredentials awsCredentials, ParameterSet parameterSet) {
    String s3Url = parameterSet.getRequired(S3_URL);
    String region = parameterSet.getOptional(REGION);

    URI uri = null;
    try {
      uri = new URI(s3Url);
    } catch (URISyntaxException uriSyntaxException) {
      throw new IllegalStateException(
          "Failed to parse the string as a URI reference: " + S3_URL,
          uriSyntaxException);
    }

    try (S3Client client = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(() -> awsCredentials)
        .build()) {

      String bucketName = uri.getHost();
      String objectKey = uri.getPath()
          .substring(1);

      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(objectKey)
          .build();

      return Resource.createPermanentResource(
          client
              .getObjectAsBytes(getObjectRequest)
              .asInputStream(),
          false);
    }
  }
}
