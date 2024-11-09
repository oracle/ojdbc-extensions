package oracle.jdbc.provider.aws;

import oracle.jdbc.provider.aws.authentication.AwsCredentialsFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import software.amazon.awssdk.auth.credentials.AwsCredentials;


/**
 * <p>
 * Common super class for {@link ResourceFactory} implementations that request
 * a resource from AWS. Subclasses implement
 * {@link #request(AwsCredentials, ParameterSet)} to request a particular type
 * of resource. Subclasses must declare the {@link Parameter} objects they
 * recognize in a {@link ParameterSet}.
 * </p><p>
 * This super class is responsible for obtaining AWS credentials from the
 * {@link AwsCredentialsFactory}, and may be configured by the
 * {@link Parameter} objects declared in that class. A
 * {@link AwsCredentials} created by {@link AwsCredentialsFactory} is passed
 * to the {@link #request(AwsCredentials, ParameterSet)} method of a subclass.
 * </p>
 */
public abstract class AwsResourceFactory<T> implements ResourceFactory<T> {
  @Override
  public final Resource<T> request(ParameterSet parameterSet) {
    AwsCredentials awsCredential =
        AwsCredentialsFactory.getInstance()
            .request(parameterSet)
            .getContent();

    try {
      return request(awsCredential, parameterSet);
    }
    catch (Exception exception) {
      throw new IllegalStateException(
          "Request failed with parameters: " + parameterSet,
          exception);
    }
  }

  /**
   * <p>
   * Abstract method that subclasses implement to request a particular type of
   * resource from AWS. Subclasses must return a {@link Resource} that
   * implements {@link Resource#isValid()} to indicate when a cached resource is
   * no longer valid.
   * </p><p>
   * Subclasses should declare the {@link Parameter} objects they recognize in
   * the {@code parameterSet}.
   * </p>
   *
   * @param awsCredentials AWS credentials configured by the
   * {@code parameterSet}. Not null.
   * @param parameterSet Set of parameters that configure the request. Not null.
   * @return The resource requested based on configured of the
   *   {@code parameterSet}, and authenticated by the given
   *   {@code awsCredentials}. Not null.
   * @throws IllegalStateException If the request fails to return a resource.
   * @throws IllegalArgumentException If the {@code parameterSet} does not
   * include a required parameter, or does not represent a valid configuration.
   */
  public abstract Resource<T> request(
      AwsCredentials awsCredentials, ParameterSet parameterSet);
}