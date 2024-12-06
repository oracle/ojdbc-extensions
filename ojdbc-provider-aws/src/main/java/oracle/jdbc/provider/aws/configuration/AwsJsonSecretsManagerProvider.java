package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.provider.aws.secrets.SecretsManagerFactory;
import oracle.jdbc.provider.configuration.JsonSecretUtil;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationJsonSecretProvider;
import oracle.sql.json.OracleJsonObject;

import java.util.Base64;

import static oracle.jdbc.provider.aws.configuration.AwsSecretsManagerConfigurationProvider.PARAMETER_SET_PARSER;

public class AwsJsonSecretsManagerProvider
    implements OracleConfigurationJsonSecretProvider {
  /**
   * {@inheritDoc}
   * <p>
   * Returns the password of the Secret that is retrieved from AWS Secrets
   * Manager secret.
   * </p>
   * <p>
   * The {@code jsonObject} has the following form:
   * </p>
   *
   * <pre>{@code
   *   "password": {
   *       "type": "awssecretsmanager",
   *       "value": "<secret-name>"
   *   }
   * }</pre>
   *
   * @param jsonObject json object to be parsed
   * @return encoded char array in base64 format that represents the retrieved
   *         Secret.
   */
  @Override
  public char[] getSecret(OracleJsonObject jsonObject) {
    ParameterSet parameterSet =
        PARAMETER_SET_PARSER
            .parseNamedValues(
                JsonSecretUtil.toNamedValues(jsonObject));

    String secretString = SecretsManagerFactory.getInstance()
        .request(parameterSet)
        .getContent();

    return Base64.getEncoder()
        .encodeToString(secretString.getBytes())
        .toCharArray();
  }

  @Override
  public String getSecretType() {
    return "awssecretsmanager";
  }
}
