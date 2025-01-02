package oracle.jdbc.provider.hashicorp.hcpvault.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.hashicorp.hcpvault.secrets.HcpVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.util.OracleConfigurationCache;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class HcpVaultSecretsManagerConfigurationProvider extends OracleConfigurationJsonProvider {

  static final ParameterSetParser PARAMETER_SET_PARSER =
          HcpVaultConfigurationParameters.configureBuilder(
                  ParameterSetParser.builder()
                          .addParameter("value", HcpVaultSecretsManagerFactory.APP_NAME)
                          .addParameter("ORG_ID", HcpVaultSecretsManagerFactory.ORG_ID)
                          .addParameter("PROJECT_ID", HcpVaultSecretsManagerFactory.PROJECT_ID)
                          .addParameter("SECRET_NAME", HcpVaultSecretsManagerFactory.SECRET_NAME)
                          .addParameter("KEY", HcpVaultSecretsManagerFactory.KEY)
          ).build();

  @Override
  public InputStream getJson(String appName) {
    // 'appName' is the part after config-hcpvault://
    final String valueField = "value";

    Map<String, String> optionsWithAppName = new HashMap<>(options);
    optionsWithAppName.put(valueField, appName);

    ParameterSet parameterSet = PARAMETER_SET_PARSER.parseNamedValues(optionsWithAppName);

    // Call the factory
    String secretsJson = HcpVaultSecretsManagerFactory
            .getInstance()
            .request(parameterSet)
            .getContent();

    return new ByteArrayInputStream(secretsJson.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String getType() {
    // The provider name that appears in the JDBC URL after "config-"
    return "hcpvault";
  }


  @Override
  public OracleConfigurationCache getCache() {
    return CACHE;
  }

}
