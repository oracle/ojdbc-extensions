package oracle.jdbc.provider.hashicorp.dedicated.configuration;

import oracle.jdbc.provider.configuration.JsonSecretUtil;
import oracle.jdbc.provider.hashicorp.dedicated.secrets.HashiVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationJsonSecretProvider;
import oracle.sql.json.OracleJsonObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static oracle.jdbc.provider.hashicorp.dedicated.configuration.HashiVaultSecretsManagerConfigurationProvider.PARAMETER_SET_PARSER;
import static oracle.jdbc.provider.hashicorp.dedicated.secrets.HashiVaultSecretsManagerFactory.FIELD_NAME;

public class HashiJsonVaultProvider implements OracleConfigurationJsonSecretProvider {

  @Override
  public char[] getSecret(OracleJsonObject jsonObject) {
    // 1) Convert the JSON object to named key-value pairs
    ParameterSet parameterSet =
            PARAMETER_SET_PARSER.parseNamedValues(
                    JsonSecretUtil.toNamedValues(jsonObject)
            );

    // 2) Call the Vault factory to fetch the raw secret string
    String secretString = HashiVaultSecretsManagerFactory
            .getInstance()
            .request(parameterSet)
            .getContent();

    ByteArrayInputStream inputStream = new ByteArrayInputStream(secretString.getBytes(StandardCharsets.UTF_8));


    OracleJsonObject secretJsonObj =
            new oracle.sql.json.OracleJsonFactory()
                    .createJsonTextValue(inputStream)
                    .asJsonObject();

    String  myPasswordValue = parameterSet.getOptional(FIELD_NAME);
    String a = String.valueOf(secretJsonObj.get(myPasswordValue));

    // 5) Base64-encode just that field
    return Base64.getEncoder()
            .encodeToString(a.getBytes())
            .toCharArray();
  }

  @Override
  public String getSecretType() {
    return "hashicorpvault";
  }
}
