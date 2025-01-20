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
package oracle.jdbc.provider.aws.authentication;

import oracle.jdbc.provider.aws.AwsResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * <p>
 * A factory for creating {@link AwsCredentials} objects from the Azure SDK
 * for Java.
 * </p><p>
 * This class is implemented using the azure-identity module of the Azure SDK
 * for Java. The Azure SDK defines environment variables that configure its
 * behavior. This class will attempt use these environment variables whenever
 * a {@link ParameterSet} has not configured a value otherwise. This is done to
 * accommodate programmers who may already be using the environment variables,
 * and do not wish to re-apply their configuration as Oracle JDBC connection
 * properties and URL parameters.
 * </p>
 */
public final class AwsCredentialsFactory
    implements ResourceFactory<AwsCredentials> {

  /** Method of authentication supported by the AWS SDK */
  public static final Parameter<AwsAuthenticationMethod>
      AUTHENTICATION_METHOD = Parameter.create(REQUIRED);

  private static final AwsCredentialsFactory INSTANCE
      = new AwsCredentialsFactory();

  private AwsCredentialsFactory() { }

  /**
   * Returns a singleton of {@code AwsCredentialsFactory}.
   * @return a singleton of {@code AwsCredentialsFactory}
   */
  public static AwsCredentialsFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<AwsCredentials> request(ParameterSet parameterSet) {
    AwsCredentials awsCredentials = getCredential(parameterSet);
    // TODO: Access tokens expire. Does a TokenCredential internally cache one?
    //   If so, then return an expiring resource.
    return Resource.createPermanentResource(awsCredentials, true);
  }

  /**
   * Returns credentials for requesting an access token. The type of credentials
   * used are configured by the parameters of the given {@code parameterSet}.
   * Supported parameters are defined by the class variables in
   * {@link AwsCredentialsFactory}.
   * @param parameterSet parameters that configure credentials. Not null.
   * @return Credentials configured by parameters
   */
  private static AwsCredentials getCredential(ParameterSet parameterSet) {

    AwsAuthenticationMethod authenticationMethod =
        parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (authenticationMethod) {
      case DEFAULT:
        return defaultCredentials();
      default :
        throw new IllegalArgumentException(
            "Unrecognized authentication method: " + authenticationMethod);
    }
  }

  /**
   * Returns credentials resolved by {@link AwsResourceFactory}.
   * @return
   */
  private static AwsCredentials defaultCredentials() {
    return DefaultCredentialsProvider
        .builder()
        .build().resolveCredentials();
  }
}
