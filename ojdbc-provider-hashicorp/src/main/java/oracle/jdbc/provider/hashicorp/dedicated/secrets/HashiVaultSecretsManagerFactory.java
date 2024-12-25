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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

public final class HashiVaultSecretsManagerFactory extends HashiVaultResourceFactory<String> {

  /** The path of the secret in Vault. Required. */
  public static final Parameter<String> SECRET_PATH = Parameter.create(REQUIRED);

  /**
   * The name of the key if the secret is a JSON with multiple fields.
   * This is optional.
   */
  public static final Parameter<String> KEY_NAME = Parameter.create();

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
    String key = parameterSet.getOptional(KEY_NAME);
    String vaultAddrParam = parameterSet.getOptional(VAULT_ADDR);
    String vaultTokenParam = parameterSet.getOptional(VAULT_TOKEN);

    String vaultAddr = (vaultAddrParam != null)
            ? vaultAddrParam
            : System.getProperty("VAULT_ADDR", System.getenv("VAULT_ADDR"));

    String vaultToken = (vaultTokenParam != null)
            ? vaultTokenParam
            : System.getProperty("VAULT_TOKEN", System.getenv("VAULT_TOKEN"));

    if (vaultAddr == null || vaultAddr.isEmpty()) {
      throw new IllegalStateException(
              "Vault address not found in URL parameters, system properties, or environment variables");
    }
    if (vaultToken == null || vaultToken.isEmpty()) {
      throw new IllegalStateException(
              "Vault token not found in URL parameters, system properties, or environment variables");
    }

    String vaultUrl = vaultAddr + secretPath;

    // Make the REST call
    String secretString = fetchSecretFromVault(vaultUrl, vaultToken);

    /*
     * If KEY_NAME is specified, we only want a single field from the nested JSON.
     * If KEY_NAME is not specified, we return the entire
     * {
     *   "data": { ...all fields... }
     * }
     * portion from the second-level data node.
     */
    secretString = extractValueFromJson(secretString, key, secretPath);

    return Resource.createPermanentResource(secretString, true);
  }

  private static String fetchSecretFromVault(String vaultUrl, String token) {
    HttpURLConnection conn = null;
    try {
      URL url = new URL(vaultUrl);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("X-Vault-Token", token);
      conn.setRequestProperty("Accept", "application/json");

      if (conn.getResponseCode() != 200) {
        throw new IllegalStateException(
                "Failed to fetch secret. HTTP error code: " + conn.getResponseCode());
      }
      try (InputStream in = conn.getInputStream()) {
        return readStream(in);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(
              "Failed to read secret from Vault at " + vaultUrl, e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
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
    InputStream is = null;
    try {
      is = new ByteArrayInputStream(secretJson.getBytes(UTF_8));
      OracleJsonObject rootObject = JSON_FACTORY.createJsonTextValue(is).asJsonObject();

      // Navigate to the second-level "data" object: rootObject.data.data
      // Example root:
      // {
      //   "request_id": "...",
      //   "data": {
      //       "data": {
      //           "connect_descriptor": "...",
      //           "jdbc": {...},
      //           "password": {...},
      //           "user": "ADMIN"
      //       },
      //       "metadata": {...}
      //   },
      //   ...
      // }

      OracleJsonObject dataNode = rootObject.getObject("data");  // top-level "data"

      OracleJsonObject dataData = dataNode.getObject("data");    // nested "data"


      if (key != null) {
        // Return only that key's value
        if (!dataData.containsKey(key)) {
          throw new IllegalArgumentException(
                  "Failed to find key " + key + " in secret: " + secretPath);
        }
        return dataData.getString(key);

      } else {
        // Return just the nested data as:
        // {
        //   "data": {
        //     "connect_descriptor": "...",
        //     "jdbc": {...},
        //     "password": {...},
        //     "user": "ADMIN"
        //   }
        // }

        return dataData.toString();
      }
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
          // ignore
        }
      }
    }
  }
}
