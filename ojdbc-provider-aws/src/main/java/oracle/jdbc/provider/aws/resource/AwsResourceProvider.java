/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.provider.aws.authentication.AwsAuthenticationMethod;
import oracle.jdbc.provider.aws.authentication.AwsCredentialsFactory;
import oracle.jdbc.provider.resource.AbstractResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;

import java.util.stream.Stream;

import static oracle.jdbc.provider.aws.authentication.AwsAuthenticationMethod.DEFAULT;
import static oracle.jdbc.provider.aws.authentication.AwsCredentialsFactory.AUTHENTICATION_METHOD;
import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.REGION;
import static oracle.jdbc.provider.aws.resource.AwsSecretsManagerResourceParameterNames.AWS_REGION;

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
