package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.aws.secret.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetBuilder;
import oracle.jdbc.provider.parameter.ParameterSetParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AwsSecretJsonProvider extends OracleConfigurationJsonProvider {

  /**
   * Parser that recognizes the named parameters which appear in the query
   * section of a URL.
   */
  private static final ParameterSetParser PARAMETER_SET_PARSER =
      AwsConfigurationParameters.configureBuilder(
              ParameterSetParser.builder()
                  .addParameter("value", AwsSecretJsonProvider::parseSecretId))
          .build();

  /**
   * Parses the "value" field of a JSON object as a Secret ID.
   * This parser configures the given {@code builder} with two distinct
   * parameters accepted by {@link SecretFactory}: One parameter for the
   * Secret ID, and another for the secret name.
   * @param secretId Secret Identifier. Not null.
   * @param builder Builder to configure with parsed parameters. Not null.
   */
  private static void parseSecretId(
      String secretId, ParameterSetBuilder builder) {
    builder.add("value", SecretFactory.SECRET_ID, secretId);
  }

  @Override
  public InputStream getJson(String secretId) {
    final String valueFieldName = "value";
    Map<String, String> optionsWithSecret = new HashMap<>(options);
    optionsWithSecret.put(valueFieldName, secretId);

    ParameterSet parameters = PARAMETER_SET_PARSER.parseNamedValues(optionsWithSecret);

    String secretString = SecretFactory.getInstance()
        .request(parameters)
        .getContent();

    return new ByteArrayInputStream(secretString.getBytes());
  }

  @Override
  public String getType() {
    return "awssecret";
  }
}
