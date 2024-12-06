package oracle.provider.aws.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AwsS3ConfigurationProviderTest {

  static {
    OracleConfigurationProvider.allowedProviders.add("awss3");
  }

  private static final OracleConfigurationProvider PROVIDER =
      OracleConfigurationProvider.find("awss3");

  /**
   * Verifies if AWS S3 Configuration Provider works with default authentication
   * @throws SQLException
   */
  @Test
  public void testDefaultAuthentication() throws SQLException {
    String location =
        TestProperties.getOrAbort(
            AwsTestProperty.AWS_S3_URI);
    Properties properties = PROVIDER
        .getConnectionProperties(location);
    assertTrue(properties.containsKey("URL"), "Contains property URL");
    assertTrue(properties.containsKey("user"), "Contains property user");
    assertTrue(properties.containsKey("password"), "Contains property password");
  }
}
