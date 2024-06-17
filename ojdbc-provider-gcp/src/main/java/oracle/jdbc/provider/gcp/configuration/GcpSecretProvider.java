package oracle.jdbc.provider.gcp.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * A provider for JSON payload which contains configuration from GCP Secret Manager.
 * See {@link #getJson(String)} for the spec of the JSON payload.
 **/
public class GcpSecretProvider extends OracleConfigurationJsonProvider {
  @Override
  public InputStream getJson(String s) throws SQLException {
    return null;
  }

  @Override
  public String getType() {
    return "gcpsecret";
  }
}