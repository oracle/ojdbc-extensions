package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.provider.aws.authentication.AwsAuthenticationMethod;
import oracle.jdbc.provider.aws.authentication.AwsCredentialsFactory;
import oracle.jdbc.provider.resource.AbstractResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;

import java.util.stream.Stream;

import static oracle.jdbc.provider.aws.authentication.AwsAuthenticationMethod.DEFAULT;
import static oracle.jdbc.provider.aws.authentication.AwsCredentialsFactory.AUTHENTICATION_METHOD;
import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.REGION;
import static oracle.jdbc.provider.aws.resource.AwsParameterStoreResourceParameterNames.AWS_REGION;

/**
 * Super class of all {@code OracleResourceProvider} implementations
 * that request a resource from AWS. This super class defines parameters for
 * authentication with AWS.
 */
public abstract class AwsResourceProvider extends AbstractResourceProvider {

  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter("authenticationMethod", AUTHENTICATION_METHOD,
                  "aws-default",
                  AwsResourceProvider::parseAuthenticationMethod),
          new ResourceParameter(AWS_REGION, REGION)
  };

  /**
   * Constructs a provider identified by the name:
   * <pre>{@code
   *   ojdbc-provider-aws-{resourceType}
   * }</pre>
   * @param resourceType The resource type identifier used in the provider name.
   * @param parameters Additional parameters specific to the subclass provider.
   */
  protected AwsResourceProvider(String resourceType, ResourceParameter... parameters) {
    super("aws", resourceType,
            Stream.concat(Stream.of(PARAMETERS), Stream.of(parameters))
                    .toArray(ResourceParameter[]::new));
  }

  /**
   * Parses the "authenticationMethod" parameter as an
   * {@link AwsAuthenticationMethod} recognized by {@link AwsCredentialsFactory}.
   *
   * @param authenticationMethod The value to parse.
   * @return An {@link AwsAuthenticationMethod} enum.
   * @throws IllegalArgumentException if the value is unrecognized.
   */
  private static AwsAuthenticationMethod parseAuthenticationMethod(String authenticationMethod) {
    switch (authenticationMethod) {
      case "aws-default": return DEFAULT;
      default:
        throw new IllegalArgumentException("Unrecognized authentication method: " + authenticationMethod);
    }
  }
}