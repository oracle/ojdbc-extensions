package oracle.jdbc.provider.hashicorp.dedicated.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.hashicorp.dedicated.secrets.HashiVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.util.OracleConfigurationCache;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HashiVaultSecretsManagerConfigurationProvider extends OracleConfigurationJsonProvider {

  static final ParameterSetParser PARAMETER_SET_PARSER =
          HashicorpConfigurationParameters.configureBuilder(
                  ParameterSetParser.builder()
                          .addParameter("value", HashiVaultSecretsManagerFactory.SECRET_PATH)
                          .addParameter("key_name", HashiVaultSecretsManagerFactory.KEY_NAME)
                          .addParameter(
                                  "VAULT_ADDR",
                                  HashiVaultSecretsManagerFactory.VAULT_ADDR
                          )
                          .addParameter(
                                  "VAULT_TOKEN",
                                  HashiVaultSecretsManagerFactory.VAULT_TOKEN
                          )
                          .addParameter("FILED_NAME",
                                  HashiVaultSecretsManagerFactory.FIELD_NAME)
          ).build();

  @Override
  public InputStream getJson(String secretPath) {
    final String valueFieldName = "value";

    // Build a map of user-provided options
    Map<String, String> optionsWithSecret = new HashMap<>(options);
    optionsWithSecret.put(valueFieldName, secretPath);

    // Parse into a ParameterSet
    ParameterSet parameters = PARAMETER_SET_PARSER.parseNamedValues(optionsWithSecret);

    // Fetch the secret from Vault
    String secretString = HashiVaultSecretsManagerFactory
            .getInstance()
            .request(parameters)
            .getContent();

    // Return the JSON as an InputStream
    return new ByteArrayInputStream(secretString.getBytes());
  }

  @Override
  public String getType() {
    // We'll reference this in our JDBC URL, e.g. "jdbc:oracle:thin:@config-hashicorpvault://..."
    return "hashicorpvault";
  }

  @Override
  public OracleConfigurationCache getCache() {
    return CACHE;
  }
}
