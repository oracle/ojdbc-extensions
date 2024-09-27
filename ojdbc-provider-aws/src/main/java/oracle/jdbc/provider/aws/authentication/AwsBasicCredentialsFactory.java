/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

import static java.lang.String.format;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.SENSITIVE;

/**
 * <p>
 * A factory for creating {@link TokenCredential} objects from the Azure SDK
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
public final class AwsBasicCredentialsFactory
    implements ResourceFactory<AwsBasicCredentials> {

  /** Method of authentication supported by the Azure SDK */
  public static final Parameter<AwsAuthenticationMethod>
    AUTHENTICATION_METHOD = Parameter.create(REQUIRED);

  public static final Parameter<String> ACCESS_KEY_ID = Parameter.create(REQUIRED);

  public static final Parameter<String> SECRET_ACCESS_KEY = Parameter.create(SENSITIVE, REQUIRED);

  private static final AwsBasicCredentialsFactory INSTANCE
      = new AwsBasicCredentialsFactory();

  private AwsBasicCredentialsFactory() { }

  /**
   * Returns a singleton of {@code TokenCredentialFactory}.
   * @return a singleton of {@code TokenCredentialFactory}
   */
  public static AwsBasicCredentialsFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<AwsBasicCredentials> request(ParameterSet parameterSet) {
    AwsBasicCredentials awsBasicCredentials = getCredential(parameterSet);
    // TODO: Access tokens expire. Does a TokenCredential internally cache one?
    //   If so, then return an expiring resource.
    return Resource.createPermanentResource(awsBasicCredentials, true);
  }

  /**
   * Returns credentials for requesting an access token. The type of credentials
   * used are configured by the parameters of the given {@code parameters}.
   * Supported parameters are defined by the class variables in
   * {@link AwsBasicCredentialsFactory}.
   * @param parameters parameters that configure credentials. Not null.
   * @return Credentials configured by parameters
   */
  private static AwsBasicCredentials getCredential(ParameterSet parameterSet) {

    AwsAuthenticationMethod authenticationMethod =
      parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (authenticationMethod) {
      case DEFAULT:
        return defaultCredentials(parameterSet);
      default :
        throw new IllegalArgumentException(
          "Unrecognized authentication method: " + authenticationMethod);
    }
  }

  /**
   * Returns credentials resolved by {@link DefaultAzureCredential}, and
   * {@link #TENANT_ID} or {@link #CLIENT_ID} as optional parameters.
   * @param parameterSet
   * @return
   */
  private static AwsBasicCredentials defaultCredentials(ParameterSet parameterSet) {
    return AwsBasicCredentials.create(
        requireParameter(parameterSet, ACCESS_KEY_ID, "ACCESS_KEY_ID"),
        requireParameter(parameterSet, SECRET_ACCESS_KEY, "SECRET_ACCESS_KEY")
    );
  }

  /**
   * Returns the value of a required parameter that may be configured by a
   * provider or an SDK environment variable.
   * @param parameters Parameters provided by the provider. Not null
   * @param parameter Parameter that may be configured by the provider. Not null
   * @param sdkName Name of the SDK environment variable, or null if there is
   * none.
   * @return The configured value of the parameter. Not null.
   * @throws IllegalStateException If the parameter is not configured by the
   * provider or the SDK.
   */
  private static String requireParameter(
    ParameterSet parameters, Parameter<String> parameter, String sdkName) {
    try {
      return parameters.getRequired(parameter);
    }
    catch (IllegalStateException parameterNotConfigured) {
      throw new IllegalArgumentException(format(
        "No value is configured for parameter \"%s\"," +
          " or SDK variable \"%s\"",
        parameters.getName(parameter), sdkName), parameterNotConfigured);
    }
  }

  /**
   * Returns the value of an optional parameter which may be configured by a
   * provider or an SDK environment variable.
   * @param parameters Parameters provided by the provider. Not null
   * @param parameter Parameter that may be configured by the provider. Not null
   * @param sdkName Name of the SDK environment variable, or null if there is
   * none.
   * @return The configured value of the parameter, or null if not configured.
   */
  private static String optionalParameter(
    ParameterSet parameters, Parameter<String> parameter, String sdkName) {
    String value = parameters.getOptional(parameter);

    return value != null ? value : null;
  }

}
