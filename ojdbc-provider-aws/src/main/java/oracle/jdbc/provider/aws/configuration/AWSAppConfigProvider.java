package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.aws.authentication.AWSCredentialsFactory;
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

public class AWSAppConfigProvider extends OracleConfigurationJsonProvider
    implements OracleConfigurationCachableProvider {
  private AWSAppConfigurationURLParser urlParser;

  @Override
  public InputStream getJson(String location) throws SQLException {
    // TODO: make sure the parameters thread safe
    // Get basic credentials
    ParameterSet parameters = urlParser.getParameters();
    AwsCredentials credentials = AWSCredentialsFactory.getInstance()
        .request(parameters)
        .getContent();
    // Get region
    String region = parameters.getRequired(AWSAppConfigurationURLParser.REGION);
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
   * Overrides the method in parent class to determine the type of AppConfig in
   * the first place. If the AppConfig is a Freeform, we assume it's a JSON
   * payload and parse it by calling
   * {@link OracleConfigurationJsonProvider#getConnectionProperties(String)}.
   * If it's a Feature Flag, throws {@link UnsupportedOperationException}.
   * @param location
   * @return Properties retrieved from {@code location}
   * @throws SQLException
   */
  @Override
  public Properties getConnectionProperties(String location)
      throws SQLException {
    // Parse parameters
    urlParser =
        new AWSAppConfigurationURLParser(location);
    ParameterSet parameters = urlParser.getParameters();

    // Get basic credentials
    AwsCredentials credentials =
        AWSCredentialsFactory.getInstance()
            .request(parameters)
            .getContent();
    // Get region
    String region = parameters.getOptional(AWSAppConfigurationURLParser.REGION);
    // Get the identifiers of app config
    String applicationIdentifier = urlParser.applicationIdentifier();
    String configurationProfileIdentifier = urlParser
        .configurationProfileIdentifier();

    // Is Feature Flags or Freeform?
    AppConfigClient appConfigClient = AppConfigClient.builder()
        .credentialsProvider(() -> credentials)
        .region(Region.of(region))
        .build();

    String type = appConfigClient.getConfigurationProfile(
        GetConfigurationProfileRequest
            .builder()
            .applicationId(applicationIdentifier)
            .configurationProfileId(configurationProfileIdentifier)
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
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
