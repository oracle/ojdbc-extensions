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

package oracle.jdbc.provider.aws.appconfig;

import oracle.jdbc.provider.aws.AwsResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.*;

/**
 * A factory for retrieving <b>Freeform Configurations</b> data from AWS
 * AppConfig using the AppConfig Data API. This factory establishes a
 * configuration session and fetches the latest configuration data based on
 * the provided application, environment, and configuration profile identifiers.
 */
public class AppConfigFactory extends AwsResourceFactory<InputStream> {

  /**
   * Parameter for the AWS AppConfig application identifier.
   * This is a required parameter and can be either the application ID or the
   * application name (e.g., "my-app") as defined in AWS AppConfig.
   */
  public static final Parameter<String> APP_CONFIG_APPLICATION = Parameter.create();

  /**
   * Parameter for the AWS AppConfig environment identifier.
   * This is a required parameter and can be either the environment ID or the
   * environment name as defined in AWS AppConfig.
   */
  public static final Parameter<String> APP_CONFIG_ENVIRONMENT = Parameter.create();

  /**
   * Parameter for the AWS AppConfig configuration profile identifier.
   * This is a required parameter and can be either the configuration profile ID
   * or the configuration profile name as defined in AWS AppConfig.
   */
  public static final Parameter<String> APP_CONFIG_PROFILE = Parameter.create();

  // System property and environment variable keys for fallback values
  private static final String SYS_PROP_ENVIRONMENT = "aws.appconfig.environment";
  private static final String ENV_VAR_ENVIRONMENT = "AWS_APP_CONFIG_ENVIRONMENT";
  private static final String SYS_PROP_PROFILE = "aws.appconfig.profile";
  private static final String ENV_VAR_PROFILE = "AWS_APP_CONFIG_PROFILE";


  private static final ResourceFactory<InputStream> INSTANCE = new AppConfigFactory();

  private AppConfigFactory() {}

  /**
   * @return a singleton of {@code AppConfigFactory}
   */
  public static ResourceFactory<InputStream> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<InputStream> request(AwsCredentials awsCredentials, ParameterSet parameterSet) {
    String applicationId = parameterSet.getRequired(APP_CONFIG_APPLICATION);
    String environmentId = getParameterWithFallback(APP_CONFIG_ENVIRONMENT, SYS_PROP_ENVIRONMENT, ENV_VAR_ENVIRONMENT, parameterSet);
    String configurationProfileId = getParameterWithFallback(APP_CONFIG_PROFILE, SYS_PROP_PROFILE, ENV_VAR_PROFILE, parameterSet);
    String region = parameterSet.getOptional(REGION);

    try (AppConfigDataClient client = AppConfigDataClient.builder()
      .credentialsProvider(() -> awsCredentials)
      .applyMutation(builder -> {
         if (region != null) builder.region(Region.of(region));
       })
       .build()) {

      final StartConfigurationSessionResponse sessionResponse = client.startConfigurationSession(
        StartConfigurationSessionRequest.builder()
          .applicationIdentifier(applicationId)
          .environmentIdentifier(environmentId)
          .configurationProfileIdentifier(configurationProfileId)
          .build());

      final String token = sessionResponse.initialConfigurationToken();

      final GetLatestConfigurationResponse configResponse = client.getLatestConfiguration(
        GetLatestConfigurationRequest.builder()
          .configurationToken(token)
          .build());
      return Resource.createPermanentResource(
              new ByteArrayInputStream(configResponse.configuration().asByteArray()),
              false);
    }
  }

  /**
   * Retrieves a parameter value with fallback to system property and environment variable.
   * @param parameter The Parameter object to retrieve from ParameterSet.
   * @param sysPropKey The system property key to check.
   * @param envVarKey The environment variable key to check.
   * @param paramSet The ParameterSet to check first.
   * @return The parameter value, or throws an exception if not found.
   * @throws IllegalArgumentException if the parameter is not found in any source.
   */
  private static String getParameterWithFallback(Parameter<String> parameter, String sysPropKey, String envVarKey, ParameterSet paramSet) {
    String value = paramSet.getOptional(parameter);
    if (value == null) {
      value = System.getProperty(sysPropKey);
      if (value == null) {
        value = System.getenv(envVarKey);
        if (value == null) {
          throw new IllegalArgumentException(
            String.format("Parameter '%s' is required and not found in ParameterSet, system property '%s', or environment variable '%s'",
              paramSet.getName(parameter), sysPropKey, envVarKey));
        }
      }
    }
    return value;
  }
}