package oracle.jdbc.provider.hashicorp.configuration;

import oracle.jdbc.provider.configuration.JsonSecretUtil;
import oracle.jdbc.provider.hashicorp.secrets.HashiVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationJsonSecretProvider;
import oracle.sql.json.OracleJsonObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static oracle.jdbc.provider.hashicorp.configuration.HashiVaultSecretsManagerConfigurationProvider.PARAMETER_SET_PARSER;
import static oracle.jdbc.provider.hashicorp.secrets.HashiVaultSecretsManagerFactory.FIELD_NAME;

/**
 * Mirrors the AWS pattern for retrieving a single secret
 * field from HashiCorp Vault, base64-encoding it.
 *
 * Example JSON input might look like:
 * {
 *   "password": {
 *       "type": "hashicorpvault",
 *       "value": "/v1/secret/data/test-config2"
 *   }
 * }
 *
 * The provider will retrieve the secret from Vault, then
 * base64-encode it and return as a char[].
 */
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


    // 3) Parse that JSON to find "myPassword"
    //    Using the Oracle JSON library, for example:
    OracleJsonObject secretJsonObj =
            new oracle.sql.json.OracleJsonFactory()
                    .createJsonTextValue(inputStream)
                    .asJsonObject();

    System.out.println(secretJsonObj);

    // 4) Retrieve the field we want
    //String myPasswordValue = secretJsonObj.getString("myPassword");
    String  myPasswordValue = parameterSet.getOptional(FIELD_NAME);
    System.out.println(myPasswordValue);

    // 5) Base64-encode just that field
    return Base64.getEncoder()
            .encodeToString(myPasswordValue.getBytes())
            .toCharArray();
  }

  @Override
  public String getSecretType() {
    // Must match the "type" field in your JSON.
    // E.g. "hashicorpvault" or "hashicorsecret"—your choice.
    return "hashicorpvault";
  }
}
