package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.aws.secrets.SecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AwsSecretsManagerConfigurationProvider extends OracleConfigurationJsonProvider {

  /**
   * Parser that recognizes the named parameters which appear in the query
   * section of a URL.
   */
  static final ParameterSetParser PARAMETER_SET_PARSER =
      AwsConfigurationParameters.configureBuilder(
              ParameterSetParser.builder()
                  .addParameter("value", SecretsManagerFactory.SECRET_NAME)
                  .addParameter("REGION", SecretsManagerFactory.REGION)
                  .addParameter("key", SecretsManagerFactory.KEY))
          .build();

  @Override
  public InputStream getJson(String secretId) {
    final String valueFieldName = "value";
    Map<String, String> optionsWithSecret = new HashMap<>(options);
    optionsWithSecret.put(valueFieldName, secretId);

    ParameterSet parameters = PARAMETER_SET_PARSER.parseNamedValues(optionsWithSecret);

    String secretString = SecretsManagerFactory.getInstance()
        .request(parameters)
        .getContent();

    return new ByteArrayInputStream(secretString.getBytes());
  }

  @Override
  public String getType() {
    return "awssecret";
  }
}
