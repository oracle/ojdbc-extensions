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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.secrets;

import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.DedicatedVaultResourceFactory;
import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultCredentials;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

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

  /**
   *  The field name for extracting a specific value from the JSON. Required.
   */
  public static final Parameter<String> FIELD_NAME = Parameter.create(REQUIRED);

  /**
   * The username for Userpass authentication. Required for Userpass method.
   */
  public static final Parameter<String> USERNAME = Parameter.create(REQUIRED);

  /**
   *  The password for Userpass authentication. Required for Userpass method.
   */
  public static final Parameter<String> PASSWORD = Parameter.create(REQUIRED);

  /**
   *  The path for Userpass authentication. Optional.
   */
  public static final Parameter<String> AUTH_PATH = Parameter.create();

  /**
   *  The namespace for the Vault API request. Optional.
   */
  public static final Parameter<String> NAMESPACE = Parameter.create();


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

    if (credentials.getVaultToken() != null) {
      String secretString = fetchSecretFromVaultWithToken(vaultAddr + secretPath, credentials.getVaultToken());
      return Resource.createPermanentResource(parseSecretJson(secretString, secretPath), true);
    }

    throw new IllegalStateException("Invalid credentials: Vault token is missing.");
  }

  /**
   * Fetches a secret from the Vault using the given Vault URL and
   * authentication token.
   *
   * @param vaultUrl The complete Vault URL including the secret path.
   * @param token    The Vault token for authentication.
   * @return The raw secret as a JSON string.
   * @throws IllegalArgumentException If there is an error during the Vault request.
   */
  private static String fetchSecretFromVaultWithToken(String vaultUrl, String token) {
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

      try (InputStream in = conn.getInputStream();
        Scanner scanner = new Scanner(in, UTF_8.name())) {
          scanner.useDelimiter("\\A");
          return scanner.hasNext() ? scanner.next() : "";
      } finally {
        conn.disconnect();
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(
              "Failed to read secret from Vault at " + vaultUrl, e);
    }
  }

  /**
   * Parses the JSON secret to extract the nested data node.
   *
   * @param secretJson The raw secret JSON string.
   * @param secretPath The secret path (for error messages).
   * @return The extracted JSON data as a string.
   */
  private static String parseSecretJson(String secretJson, String secretPath) {
    try (InputStream is = new ByteArrayInputStream(secretJson.getBytes(UTF_8))) {
      OracleJsonObject rootObject = JSON_FACTORY.createJsonTextValue(is).asJsonObject();
      OracleJsonObject dataNode = rootObject.getObject("data");
      OracleJsonObject nestedData = dataNode.getObject("data");
      return nestedData.toString();
    } catch (IOException e) {
      throw new IllegalArgumentException(
              "Failed to parse JSON for secret at path: " + secretPath, e);
    }
  }
}
