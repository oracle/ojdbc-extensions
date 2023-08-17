package oracle.jdbc.provider.oci.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

/**
 * Verifies the {@link OciDatabaseToolsConnectionProvider} as implementing
 * behavior specified by its JavaDoc.
 */

public class OciDatabaseToolsConnectionProviderTest {
  private static final OracleConfigurationProvider PROVIDER =
      OracleConfigurationProvider.find("ocidbtools");

  /**
   * Verifies the properties can be obtained using the provided Database Tools
   * Connection OCID.
   */
  @Test
  public void testGetProperties() throws SQLException {
    String ocid =
        TestProperties.getOrAbort(OciTestProperty.OCI_DB_TOOLS_CONNECTION_OCID);
    Properties props = PROVIDER.getConnectionProperties(ocid);
    Assertions.assertNotEquals(0, props.size());
  }
}
