package oracle.jdbc.provider.oci.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the {@link OciVaultJsonProvider} as implementing behavior
 * specified by its JavaDoc.
 */

public class OciVaultJsonProviderTest {
  private static final OracleConfigurationProvider PROVIDER =
    OracleConfigurationProvider.find("ocivault");

  /**
   * Verifies the AUTHENTICATION=OCI_DEFAULT parameter setting.
   * This test will fail if (~/.oci/config) or (~/.oraclebmc/config) or the
   * environmental variable OCI_CONFIG_FILE is not set.
   */
  @Test
  public void testDefaultAuthentication() throws SQLException {
    verifyProperties("AUTHENTICATION=OCI_DEFAULT");
  }

  /** verifies a properties object returned with a URL with the given options **/
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