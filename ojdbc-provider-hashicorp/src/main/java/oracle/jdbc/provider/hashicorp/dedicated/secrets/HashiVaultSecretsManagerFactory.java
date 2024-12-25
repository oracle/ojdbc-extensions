package oracle.jdbc.provider.hashicorp.dedicated.secrets;

import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.dedicated.HashiVaultResourceFactory;
import oracle.jdbc.provider.hashicorp.dedicated.authentication.HashiCredentials;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

public final class HashiVaultSecretsManagerFactory extends HashiVaultResourceFactory<String> {

  /** The path of the secret in Vault. Required. */
  public static final Parameter<String> SECRET_PATH = Parameter.create(REQUIRED);

  /**
   * The name of the key if the secret is a JSON with multiple fields.
   * This is optional.
   */
  public static final Parameter<String> KEY = Parameter.create();

  /**
   * (Optional) The Vault address. If not specified, fallback to system property or environment var.
   */
  public static final Parameter<String> VAULT_ADDR = Parameter.create();

  /**
   * (Optional) The Vault token. If not specified, fallback to system property or environment var.
   */
  public static final Parameter<String> VAULT_TOKEN = Parameter.create();

  public static final Parameter<String> FIELD_NAME = Parameter.create();

  private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();

  private static final String ERROR_VAULT_ADDR = "Vault address not found in parameters, system properties, or environment variables";
  private static final String ERROR_VAULT_TOKEN = "Vault token not found in parameters, system properties, or environment variables";


  /**
   * The single instance of this factory, cached for performance.
   */
  private static final ResourceFactory<String> INSTANCE =
          CachedResourceFactory.create(new HashiVaultSecretsManagerFactory());

  private HashiVaultSecretsManagerFactory() {}

  public static ResourceFactory<String> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<String> request(HashiCredentials credentials, ParameterSet parameterSet) {
    String secretPath = parameterSet.getRequired(SECRET_PATH);
    // Get required parameters with fallback
    String vaultAddr = getRequiredOrFallback(parameterSet, VAULT_ADDR, "VAULT_ADDR");
    String vaultToken = getRequiredOrFallback(parameterSet, VAULT_TOKEN, "VAULT_TOKEN");
    String key = parameterSet.getOptional(KEY);

    System.out.println("secretPath: " + secretPath + ", vaultAddr: " + vaultAddr + ", vaultToken: " + vaultToken + ", key: " + key);


    if (vaultAddr == null || vaultAddr.isEmpty()) {
      throw new IllegalStateException(ERROR_VAULT_ADDR);
    }
    if (vaultToken == null || vaultToken.isEmpty()) {
      throw new IllegalStateException(ERROR_VAULT_TOKEN);
    }


    String vaultUrl = vaultAddr + secretPath;

    // Make the REST call
    String secretString = fetchSecretFromVault(vaultUrl, vaultToken);

    /*
     * If KEY is specified, we only want a single field from the nested JSON.
     * If KEY is not specified, we return the entire
     * {
     *   "data": { ...all fields... }
     * }
     * portion from the second-level data node.
     */
    secretString = extractValueFromJson(secretString, key, secretPath);

    return Resource.createPermanentResource(secretString, true);
  }

  private static String fetchSecretFromVault(String vaultUrl, String token) {
    try {
      URL url = new URL(vaultUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("X-Vault-Token", token);
      conn.setRequestProperty("Accept", "application/json");

      if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
        throw new IllegalStateException(
                "Failed to fetch secret. HTTP error code: " + conn.getResponseCode());
      }

      // Use try-with-resources for the InputStream
      try (InputStream in = conn.getInputStream()) {
        return readStream(in);
      } finally {
        // Explicitly disconnect the connection
        conn.disconnect();
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(
              "Failed to read secret from Vault at " + vaultUrl, e);
    }
  }

  private static String readStream(InputStream in) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
      baos.write(buffer, 0, bytesRead);
    }
    return new String(baos.toByteArray(), UTF_8);
  }


  /**
   * If {@code key} is non-null, parse the JSON from Vault and return only that key's value.
   * Otherwise, return just the nested JSON:
   *
   * {
   *   "data": {
   *       "connect_descriptor": "...",
   *       "jdbc": {...},
   *       "password": {"type":"base64","value":"SWN6ZzU5NDQ1OTQ0JA=="},
   *       "user": "ADMIN"
   *   }
   * }
   */
  private static String extractValueFromJson(String secretJson, String key, String secretPath) {
    try (InputStream is = new ByteArrayInputStream(secretJson.getBytes(UTF_8))) {
      OracleJsonObject rootObject = JSON_FACTORY.createJsonTextValue(is).asJsonObject();
      OracleJsonObject dataNode = rootObject.getObject("data");
      OracleJsonObject nestedData = dataNode.getObject("data");

      if (key == null) {
        return nestedData.toString();
      }

      OracleJsonValue value = nestedData.get(key);
      if (value == null) {
        throw new IllegalArgumentException(
                "Key \"" + key + "\" not found in secret at path: " + secretPath);
      }

      return value.toString();
    } catch (IOException e) {
      throw new IllegalArgumentException(
              "Failed to parse JSON for secret at path: " + secretPath, e);
    }
  }

  private static String getEnvOrProperty(String key) {
    return System.getProperty(key, System.getenv(key));
  }

  private static String getRequiredOrFallback(ParameterSet parameterSet, Parameter<String> parameter, String envKey) {
    // Try to get the required parameter value
    String value = parameterSet.getOptional(parameter);
    if (value != null) {
      return value;
    }

    // Fallback to environment variables or system properties
    return getEnvOrProperty(envKey);
  }



}
