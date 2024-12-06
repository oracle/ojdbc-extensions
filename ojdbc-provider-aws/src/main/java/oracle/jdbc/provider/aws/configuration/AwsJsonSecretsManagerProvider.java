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
