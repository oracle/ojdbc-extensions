/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.hashicorp.dedicated.secrets;

import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.dedicated.DedicatedVaultResourceFactory;
import oracle.jdbc.provider.hashicorp.dedicated.authentication.DedicatedVaultCredentials;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

public final class DedicatedVaultSecretsManagerFactory extends DedicatedVaultResourceFactory<String> {

  /** The path of the secret in Vault. Required. */
  public static final Parameter<String> SECRET_PATH = Parameter.create(REQUIRED);

  /**
   * The name of the key if the secret is a JSON with multiple fields.
   * This is optional.
   */
  public static final Parameter<String> KEY = Parameter.create();

  /**
   * The Vault address. If not specified, fallback to system property or environment var.
   */
  public static final Parameter<String> VAULT_ADDR = Parameter.create(REQUIRED);

  /**
   * The Vault token. If not specified, fallback to system property or environment var.
   */
  public static final Parameter<String> VAULT_TOKEN = Parameter.create(REQUIRED);

  public static final Parameter<String> FIELD_NAME = Parameter.create();

  private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();

  /**
   * The single instance of this factory, cached for performance.
   */
  private static final ResourceFactory<String> INSTANCE =
          CachedResourceFactory.create(new DedicatedVaultSecretsManagerFactory());

  private DedicatedVaultSecretsManagerFactory() {}

  public static ResourceFactory<String> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<String> request(DedicatedVaultCredentials credentials, ParameterSet parameterSet) {
    String secretPath = parameterSet.getRequired(SECRET_PATH);
    String vaultAddr = getRequiredOrFallback(parameterSet, VAULT_ADDR, "VAULT_ADDR");
    String key = parameterSet.getOptional(KEY);

    if (vaultAddr == null || vaultAddr.isEmpty()) {
      throw new IllegalStateException("Vault address not found in parameters, system properties, or environment variables");
    }

    String vaultUrl = vaultAddr + secretPath;

    String secretString = fetchSecretFromVault(vaultUrl, credentials.getVaultToken());

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

  /**
   * Fetches a secret from the Vault using the given Vault URL and authentication token.
   *
   * @param vaultUrl The complete Vault URL including the secret path.
   * @param token    The Vault token for authentication.
   * @return The raw secret as a JSON string.
   * @throws IllegalArgumentException If there is an error during the Vault request.
   */
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

      try (InputStream in = conn.getInputStream()) {
        return readStream(in);
      } finally {
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
   * Extracts a specific key's value from a JSON-formatted secret.
   *
   * @param secretJson The JSON string representing the secret.
   * @param key The key to extract from the JSON.
   * @return The value corresponding to the key.
   * @throws IllegalArgumentException If the key is not found or the JSON is invalid.
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
}
