package oracle.jdbc.provider.hashicorp.dedicated.configuration;

import oracle.jdbc.provider.configuration.JsonSecretUtil;
import oracle.jdbc.provider.hashicorp.dedicated.secrets.DedicatedVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationJsonSecretProvider;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static oracle.jdbc.provider.hashicorp.dedicated.configuration.DedicatedVaultSecretsManagerConfigurationProvider.PARAMETER_SET_PARSER;
import static oracle.jdbc.provider.hashicorp.dedicated.secrets.DedicatedVaultSecretsManagerFactory.FIELD_NAME;

public class DedicatedVaultJsonSecretProvider implements OracleConfigurationJsonSecretProvider {

  @Override
  public char[] getSecret(OracleJsonObject jsonObject) {
    ParameterSet parameterSet =
            PARAMETER_SET_PARSER.parseNamedValues(
                    JsonSecretUtil.toNamedValues(jsonObject)
            );

    String secretString = DedicatedVaultSecretsManagerFactory
            .getInstance()
            .request(parameterSet)
            .getContent();

    ByteArrayInputStream inputStream = new ByteArrayInputStream(
            secretString.getBytes(StandardCharsets.UTF_8));

    OracleJsonObject secretJsonObj =
            new OracleJsonFactory()
                    .createJsonTextValue(inputStream)
                    .asJsonObject();

    String fieldName = parameterSet.getOptional(FIELD_NAME);
    String extractedPassword = String.valueOf(secretJsonObj.get(fieldName));

    // 5) Base64-encode just that field
    return Base64.getEncoder()
            .encodeToString(extractedPassword.getBytes())
            .toCharArray();
  }

  @Override
  public String getSecretType() {
    return "dedicatedvault";
  }
}
