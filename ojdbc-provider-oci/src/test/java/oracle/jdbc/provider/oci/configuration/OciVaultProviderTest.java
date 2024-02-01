package oracle.jdbc.provider.oci.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class OciVaultProviderTest {
  private static final OracleConfigurationProvider PROVIDER =
    OracleConfigurationProvider.find("ocivault");

  @Test
  public void testConfigFile() throws SQLException {
    verifyProperties("AUTHENTICATION=OCI_DEFAULT");
  }

  private static void verifyProperties(String... options) throws SQLException {
    Properties properties = PROVIDER
      .getConnectionProperties(composeUrl(options));

    assertNotNull(properties);
  }

  private static String composeUrl(String... options) {
    return String.format("%s?%s",
      TestProperties.getOrAbort(OciTestProperty.OCI_PASSWORD_PAYLOAD_OCID),
      String.join("&", options));
  }
}