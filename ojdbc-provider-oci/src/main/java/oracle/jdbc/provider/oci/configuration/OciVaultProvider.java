package oracle.jdbc.provider.oci.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.configuration.JsonSecretUtil;
import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.sql.json.OracleJsonObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * A provider for JSON payload which contains configuration from OCI Vault.
 * See {@link #getJson(String)} for the spec of the JSON payload.
 **/
public class OciVaultProvider extends OracleConfigurationJsonProvider {

  /**
   * {@inheritDoc}
   * <p>
   * Returns the JSON payload stored in OCI Vault Secret.
   * </p><p>The {@code secretOcid} is a OCID of Vault Secret which can be
   * acquired on the OCI Web Console. The Json payload is stored in the Secret
   * Contents of Vault Secret.
   * </p>
   * @param secretOcid the OCID of secret used by this provider to retrieve
   *                   JSON payload from OCI
   * @return JSON payload
   **/
  @Override
  public InputStream getJson(String secretOcid) throws SQLException {
    final String valueFieldName = "value";
    Map<String, String> options = new HashMap<>();
    options.put(valueFieldName, secretOcid);

    ParameterSet parameters =
      OciConfigurationParameters.getParser()
        .parseNamedValues(options);

    String secretContent = SecretFactory.getInstance()
      .request(parameters)
      .getContent()
      .getBase64Secret();

    InputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(secretContent));
    return inputStream;
  }

  /***/
  @Override
  public String getType() {
    return "vaultoci";
  }
}
