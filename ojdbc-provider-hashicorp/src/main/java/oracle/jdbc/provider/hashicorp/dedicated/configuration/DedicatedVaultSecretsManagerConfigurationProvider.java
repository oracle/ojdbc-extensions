package oracle.jdbc.provider.hashicorp.dedicated.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.hashicorp.dedicated.secrets.DedicatedVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.util.OracleConfigurationCache;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DedicatedVaultSecretsManagerConfigurationProvider extends OracleConfigurationJsonProvider {

  static final ParameterSetParser PARAMETER_SET_PARSER =
          DedicatedVaultConfigurationParameters.configureBuilder(
                  ParameterSetParser.builder()
                          .addParameter("value", DedicatedVaultSecretsManagerFactory.SECRET_PATH)
                          .addParameter("key", DedicatedVaultSecretsManagerFactory.KEY)
                          .addParameter(
                                  "VAULT_ADDR",
                                  DedicatedVaultSecretsManagerFactory.VAULT_ADDR
                          )
                          .addParameter(
                                  "VAULT_TOKEN",
                                  DedicatedVaultSecretsManagerFactory.VAULT_TOKEN
                          )
                          .addParameter("FILED_NAME",
                                  DedicatedVaultSecretsManagerFactory.FIELD_NAME)
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
    String secretString = DedicatedVaultSecretsManagerFactory
            .getInstance()
            .request(parameters)
            .getContent();

    return new ByteArrayInputStream(secretString.getBytes());
  }

  @Override
  public String getType() {
    return "hashicorpvault";
  }

  @Override
  public OracleConfigurationCache getCache() {
    return CACHE;
  }
}
