package oracle.jdbc.provider.gcp.configuration;

import com.google.cloud.secretmanager.v1.Secret;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import oracle.jdbc.driver.OracleConfigurationJsonProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * A provider for JSON payload which contains configuration from GCP Secret Manager.
 * See {@link #getJson(String)} for the spec of the JSON payload.
 **/
public class GcpSecretProvider extends OracleConfigurationJsonProvider {
  @Override
  public InputStream getJson(String secretName) throws SQLException {
    try {
      SecretManagerServiceClient client = SecretManagerServiceClient.create();
      Secret secret = client.getSecret(secretName);
      return new ByteArrayInputStream(secret.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getType() {
    return "gcpsecret";
  }
}