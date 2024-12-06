package oracle.provider.aws.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AwsSecretsManagerConfigurationProviderTest {

  static {
    OracleConfigurationProvider.allowedProviders.add("awssecretsmanager");
  }

  private static final OracleConfigurationProvider PROVIDER =
      OracleConfigurationProvider.find("awssecretsmanager");

  /**
   * Verifies if AWS Secrets Manager Configuration Provider works with default authentication
   * @throws SQLException
   */
  @Test
  public void testDefaultAuthentication() throws SQLException {
    String location =
        TestProperties.getOrAbort(
            AwsTestProperty.AWS_SECRET_NAME);
    Properties properties = PROVIDER
        .getConnectionProperties(location);
    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }
}
