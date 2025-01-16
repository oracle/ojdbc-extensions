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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.secrets.DedicatedVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.secrets.DedicatedVaultSecretsManagerFactory.*;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getOptionalOrFallback;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

/**
 * <p>
 * Factory for creating {@link DedicatedVaultCredentials} objects for authenticating
 * with Dedicated HashiCorp Vault.
 * </p><p>
 * This factory determines the appropriate authentication method based on the provided
 * {@link ParameterSet} and creates credentials accordingly.
 * </p>
 */
public final class DedicatedVaultCredentialsFactory implements ResourceFactory<DedicatedVaultCredentials> {

  /**
   * <p>
   * Parameter for configuring the authentication method.
   * Must be provided in the {@link ParameterSet}.
   * </p>
   */
  public static final Parameter<DedicatedVaultAuthenticationMethod> AUTHENTICATION_METHOD =
          Parameter.create(REQUIRED);

  // 1 minutes buffer for token expiration (in ms)
  private static final long TOKEN_TTL_BUFFER = 60_000;
  private static volatile VaultToken vaultToken;

  // Default TTL fallback in seconds
  private static long lastTokenTTL = 3_600;

  private static final DedicatedVaultCredentialsFactory INSTANCE =
          new DedicatedVaultCredentialsFactory();

  private DedicatedVaultCredentialsFactory() { }

  /**
   * Returns a singleton instance of {@code DedicatedVaultCredentialsFactory}.
   *
   * @return a singleton instance. Not null.
   */
  public static DedicatedVaultCredentialsFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<DedicatedVaultCredentials> request(ParameterSet parameterSet) {
    DedicatedVaultCredentials credentials = getCredential(parameterSet);
    return Resource.createPermanentResource(credentials, true);
  }

  /**
   * Determines the appropriate credentials based on the provided parameters.
   *
   * @param parameterSet the set of parameters configuring the request. Must
   * not be null.
   * @return the created {@code DedicatedVaultCredentials} instance.
   */
  private static DedicatedVaultCredentials getCredential(ParameterSet parameterSet) {
    // Check which authentication method is requested
    DedicatedVaultAuthenticationMethod method =
            parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (method) {
      case VAULT_TOKEN:
        return createTokenCredentials(parameterSet);
      case USERPASS:
        return createUserpassCredentials(parameterSet);
      default:
        throw new IllegalArgumentException(
                "Unrecognized authentication method: " + method);
    }
  }

  /**
   * Creates {@link DedicatedVaultCredentials} using the Vault token.
   *
   * @param parameterSet the set of parameters containing the Vault token. Must not be null.
   * @return the created {@code DedicatedVaultCredentials} instance.
   */
  private static DedicatedVaultCredentials createTokenCredentials(ParameterSet parameterSet) {
    String vaultToken = getRequiredOrFallback(
      parameterSet,
      DedicatedVaultSecretsManagerFactory.VAULT_TOKEN,
      "VAULT_TOKEN"
    );

    if (vaultToken == null || vaultToken.isEmpty()) {
      throw new IllegalStateException("Vault Token not found in parameters, " +
              "system properties, or environment variables");
    }
    return new DedicatedVaultCredentials(vaultToken);
  }

  /**
   * Creates {@link DedicatedVaultCredentials} using the Userpass
   * authentication method.
   * <p>
   * This method uses a username and password to obtain a temporary Vault token.
   * The token is cached for reuse until it expires.
   * </p>
   *
   * @param parameterSet the set of parameters containing the Userpass credentials.
   * @return the created {@code DedicatedVaultCredentials} instance.
   */
  private static DedicatedVaultCredentials createUserpassCredentials(ParameterSet parameterSet) {
    String vaultAddr = getRequiredOrFallback(parameterSet, VAULT_ADDR, "VAULT_ADDR");
    String authPath = getOptionalOrFallback(parameterSet, AUTH_PATH, "VAULT_AUTH_PATH");
    String namespace = getOptionalOrFallback(parameterSet, NAMESPACE, "VAULT_NAMESPACE");
    String username = getRequiredOrFallback(parameterSet, USERNAME, "VAULT_USERNAME");
    String password = getRequiredOrFallback(parameterSet, PASSWORD,
            "VAULT_PASSWORD");

    synchronized (DedicatedVaultCredentialsFactory.class) {
      long currentTime = System.currentTimeMillis();
      if (vaultToken != null && currentTime < vaultToken.getExpiryTime()) {
        return new DedicatedVaultCredentials(vaultToken.getToken());
      }

      String token = authenticateWithUserpass(vaultAddr, authPath, namespace, username, password);

      vaultToken = new VaultToken(token,
              currentTime + lastTokenTTL * 1000 - TOKEN_TTL_BUFFER);
      return new DedicatedVaultCredentials(token);
    }

  }

  /**
   * A class representing a Bearer Token and its expiration time.
   */
  private static class VaultToken {
    private final String token;
    private final long expiryTime;

    VaultToken(String token, long expiryTime) {
      this.token = token;
      this.expiryTime = expiryTime;
    }

    public String getToken() {
      return token;
    }

    public long getExpiryTime() {
      return expiryTime;
    }
  }

  /**
   * Authenticates with the HashiCorp Vault using the Userpass authentication method.
   * <p>
   * This method sends a POST request to the Vault's Userpass authentication
   * endpoint and retrieves a client token that can be used for subsequent
   * API requests.
   * </p>
   *
   * @param vaultAddr the base URL of the HashiCorp Vault instance
   * (e.g., "https://vault.example.com"). Must not be null or empty.
   * @param authPath  the path of the Userpass authentication mount in Vault
   * (e.g., "userpass"). default value is "userpass".
   * @param namespace the namespace for the Vault request.
   * @param username  the username for Userpass authentication. Must not be null or empty.
   * @param password  the password for Userpass authentication. Must not be null or empty.
   * @return the client token as a {@link String}, which can be used for making authenticated
   * requests to the Vault. Never null or empty if the request succeeds.
   * @throws IllegalStateException if the authentication fails or if the Vault returns an error.
   */

  private static String authenticateWithUserpass(String vaultAddr, String authPath, String namespace, String username, String password) {
    try {
      // Construct the Userpass authentication endpoint
      String authEndpoint = vaultAddr + "/v1/auth/" + authPath + "/login/" + username;
      URL url = new URL(authEndpoint);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      // Add the namespace header if provided
      if (namespace!=null && !namespace.isEmpty()) {
        conn.setRequestProperty("X-Vault-Namespace", namespace);
      }
      conn.setDoOutput(true);

      // Construct the request payload with the password
      String payload = String.format("{\"password\": \"%s\"}", password);
      try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.getBytes(StandardCharsets.UTF_8));
      }

      if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
        try (InputStream in = conn.getInputStream();
             Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
          scanner.useDelimiter("\\A");
          String jsonResponse = scanner.hasNext() ? scanner.next() : "";

          // Parse the JSON response to extract the client token and TTL
          OracleJsonObject response = new OracleJsonFactory()
                  .createJsonTextValue(new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)))
                  .asJsonObject();

          // Extract and cache the token's TTL
          lastTokenTTL = response.getObject("auth").getLong("lease_duration");
          // Return the client token
          return response.getObject("auth").getString("client_token");
        }
      } else {
        throw new IllegalStateException("Failed to authenticate with Userpass. HTTP=" + conn.getResponseCode());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to authenticate using Userpass", e);
    }
  }
}
