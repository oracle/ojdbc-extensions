package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.aws.authentication.AwsBasicCredentialsFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationCachableProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
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
  private ParameterSet parameters;

  @Override
  public InputStream getJson(String location) throws SQLException {
    // TODO: make sure the parameters thread safe
    // Get basic credentials
    AwsBasicCredentials credentials =
        AwsBasicCredentialsFactory.getInstance()
            .request(parameters)
            .getContent();
    // Get region
    String region = parameters.getOptional(AWSAppConfigurationURLParser.REGION);
    // Get the identifiers of app config
    String applicationIdentifier = parameters.getRequired(
        AWSAppConfigurationURLParser.APPLICATION_IDENTIFIER);
    String environmentIdentifier = parameters.getRequired(
        AWSAppConfigurationURLParser.ENVIRONMENT_IDENTIFIER);
    String configurationProfileIdentifier = parameters.getRequired(
        AWSAppConfigurationURLParser.CONFIGURATION_PROFILE_IDENTIFIER);

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

  @Override
  public Properties getConnectionProperties(String location)
      throws SQLException {
    // Parse parameters
    AWSAppConfigurationURLParser appConfigUrlParser =
        new AWSAppConfigurationURLParser(location);
    parameters = appConfigUrlParser.getParameters();

    // Get basic credentials
    AwsBasicCredentials credentials =
        AwsBasicCredentialsFactory.getInstance()
            .request(parameters)
            .getContent();
    // Get region
    String region = parameters.getOptional(AWSAppConfigurationURLParser.REGION);
    // Get the identifiers of app config
    String applicationIdentifier = parameters.getRequired(
        AWSAppConfigurationURLParser.APPLICATION_IDENTIFIER);
    String configurationProfileIdentifier = parameters.getRequired(
        AWSAppConfigurationURLParser.CONFIGURATION_PROFILE_IDENTIFIER);



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
