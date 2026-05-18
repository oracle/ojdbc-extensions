package oracle.jdbc.provider.oci.configuration;

import oracle.jdbc.driver.configuration.OracleConfigurationParsableProvider;
import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.util.OracleConfigurationCache;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * A provider for JSON payload which contains configuration from OCI Vault.
 * See {@link #getInputStream(String)} for the spec of the JSON payload.
 **/
public class OciVaultJsonProvider extends OracleConfigurationParsableProvider {

  private static final OracleConfigurationCache CACHE = OracleConfigurationCache.create(100);

  /**
   * {@inheritDoc}
   * <p>
   * Returns the JSON payload stored in OCI Vault Secret.
   * </p><p>The {@code secretOcid} is an OCID of Vault Secret which can be
   * acquired on the OCI Web Console. The Json payload is stored in the Secret
   * Contents of Vault Secret.
   * </p>
   * @param secretOcid the OCID of secret used by this provider to retrieve
   *                   JSON payload from OCI
   * @return JSON payload
   **/
  @Override
  public InputStream getInputStream(String secretOcid) {
    final String valueFieldName = "value";
    Map<String, String> optionsWithOcid = new HashMap<>(options);
    optionsWithOcid.put(valueFieldName, secretOcid);

    ParameterSet parameters =
      OciConfigurationParameters.getParser()
        .parseNamedValues(optionsWithOcid);

    String secretContent = SecretFactory.getInstance()
      .request(parameters)
      .getContent()
      .getBase64Secret();

    InputStream inputStream = new ByteArrayInputStream(
        Base64.getDecoder().decode(secretContent));
    return inputStream;
  }

  /**
   * {@inheritDoc}
   * Returns type of this provider, which is a unique identifier for the
   * Service Provider Interface.
   *
   * @return type of this provider
   */
  @Override
  public String getType() {
    return "ocivault";
  }

  @Override
  public String getParserType(String arg0) {
    return "json";
  }

  /**
   * {@inheritDoc}
   * @return cache of this provider which is used to store configuration
   */
  @Override
  public OracleConfigurationCache getCache() {
    return CACHE;
  }
}
