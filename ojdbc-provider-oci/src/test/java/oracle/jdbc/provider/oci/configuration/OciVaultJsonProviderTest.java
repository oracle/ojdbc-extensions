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

  static {
    OracleConfigurationProvider.allowedProviders.add("ocivault");
  }

  private static final OracleConfigurationProvider PROVIDER =
    OracleConfigurationProvider.find("ocivault");

  /**
   * Verifies the AUTHENTICATION=OCI_DEFAULT parameter setting.
   * This test will fail if (~/.oci/config) or (~/.oraclebmc/config) or the
   * environmental variable OCI_CONFIG_FILE is not set.
   */
  @Test
  public void testDefaultAuthentication() throws SQLException {
    String baseUrl =
      TestProperties.getOrAbort(OciTestProperty.OCI_PASSWORD_PAYLOAD_OCID);
    String[] options = new String[] {"AUTHENTICATION=OCI_DEFAULT"};
    String url = composeUrl(baseUrl, options);
    verifyProperties(url);
  }

  /**
   * Verifies the AUTHENTICATION=OCI_DEFAULT parameter setting with key option.
   */
  @Test
  public void testDefaultAuthenticationWithKeyOption() throws SQLException {
    String baseUrl =
      TestProperties.getOrAbort(OciTestProperty.OCI_PASSWORD_PAYLOAD_OCID_MULTIPLE_KEYS);
    String[] options = new String[] {
      "AUTHENTICATION=OCI_DEFAULT",
      "key=" + TestProperties.getOrAbort(OciTestProperty.OCI_PASSWORD_PAYLOAD_OCID_KEY)};
    String url = composeUrl(baseUrl, options);
    verifyProperties(url);
  }

  /** Verifies a properties object returned with a given URL. */
  private static void verifyProperties(String url) throws SQLException {
    Properties properties = PROVIDER.getConnectionProperties(url);

    assertNotNull(properties);
  }

  private static String composeUrl(String baseUrl, String... options) {
    return String.format("%s?%s", baseUrl, String.join("&", options));
  }
}