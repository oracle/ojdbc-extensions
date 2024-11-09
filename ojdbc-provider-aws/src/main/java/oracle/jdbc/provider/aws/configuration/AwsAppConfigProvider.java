package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.aws.authentication.AwsCredentialsFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationCachableProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationProfileRequest;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.REGION;

/**
 * <p>
 *   A provider of App Configuration values from AWS.
 * </p>
 */
public class AwsAppConfigProvider extends OracleConfigurationJsonProvider
    implements OracleConfigurationCachableProvider {
  private AwsAppConfigurationURLParser urlParser;

  @Override
  public InputStream getJson(String location) throws SQLException {
    // TODO: make sure the parameters thread safe
    ParameterSet parameters = urlParser.getParameters();

    // Get credentials
    AwsCredentials credentials = AwsCredentialsFactory.getInstance()
        .request(parameters)
        .getContent();

    // Get region
    String region = parameters.getOptional(REGION);

    // Get the identifiers of app config
    String applicationIdentifier = urlParser.applicationIdentifier();
    String environmentIdentifier = urlParser.environmentIdentifier();
    String configurationProfileIdentifier =
        urlParser.configurationProfileIdentifier();

    // Get the configuration data
    AppConfigDataClient appConfigDataClient = AppConfigDataClient.builder()
        .credentialsProvider(() -> credentials)
        .region(Region.of(region))
        .build();

    StartConfigurationSessionRequest request =
        StartConfigurationSessionRequest.builder()
            .applicationIdentifier(applicationIdentifier)
            .environmentIdentifier(environmentIdentifier)
            .configurationProfileIdentifier(configurationProfileIdentifier)
            .build();

    String sessionToken = appConfigDataClient.startConfigurationSession(request)
        .initialConfigurationToken();

    GetLatestConfigurationRequest latestConfigurationRequest =
        GetLatestConfigurationRequest.builder()
            .configurationToken(sessionToken)
            .build();

    GetLatestConfigurationResponse latestConfigurationResponse =
        appConfigDataClient.getLatestConfiguration(latestConfigurationRequest);
    String data =  latestConfigurationResponse.configuration().asUtf8String();
    return new ByteArrayInputStream(data.getBytes());
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns the connection properties configured in Azure App Configuration.
   * </p><p>
   * Override the parent method to Determine if the AppConfig is a Flag type or
   * a Freeform type in the first hand. Flags type is not supported yet.
   * If it's a Freeform type, the config should be considered as a json payload
   * and will be parsed by calling getConnectionProperties(String) in the parent
   * class.
   * </p>
   * @param location the value used by this provider to retrieve configuration
   *                 from AWS
   * @return connection properties that are stored in AWS App Configuration
   * @throws SQLException
   */
  @Override
  public Properties getConnectionProperties(String location)
      throws SQLException {
    urlParser =
        new AwsAppConfigurationURLParser(location);
    ParameterSet parameters = urlParser.getParameters();

    // Get credentials
    AwsCredentials credentials =
        AwsCredentialsFactory.getInstance()
            .request(parameters)
            .getContent();

    // Get region
    String region = parameters.getOptional(REGION);

    // Is Feature Flags or Freeform?
    AppConfigClient appConfigClient = AppConfigClient.builder()
        .credentialsProvider(() -> credentials)
        .region(Region.of(region))
        .build();

    String type = appConfigClient.getConfigurationProfile(
            GetConfigurationProfileRequest
                .builder()
                .applicationId(urlParser.applicationIdentifier())
                .configurationProfileId(
                    urlParser.configurationProfileIdentifier())
                .build())
        .type();

    Properties properties;
    if (type.contains("FeatureFlags")) {
      throw new UnsupportedOperationException("Not yet implemented");
    } else if(type.contains("Freeform")) {
      properties = super.getConnectionProperties(location);
    } else {
      throw new IllegalArgumentException("Unknown App Config type: " + type);
    }

    return properties;
  }

  @Override
  public String getType() {
    return "awsappconfig";
  }

  @Override
  public Properties removeProperties(String location) {
    throw new UnsupportedOperationException("To be removed");
  }
}