/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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

import oracle.jdbc.driver.oauth.OpaqueAccessToken;
import oracle.jdbc.provider.hashicorp.HttpUtil;
import oracle.jdbc.provider.hashicorp.JsonUtil;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetBuilder;
import oracle.sql.json.OracleJsonObject;
import java.time.OffsetDateTime;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.*;

/**
 * Enumeration of authentication methods supported by Dedicated HashiCorp Vault.
 * <p>
 * The enum handles token generation and caching to improve performance and
 * reduce the number of authentication requests to the Vault server.
 * </p>
 * */
public enum DedicatedVaultAuthenticationMethod {

  /**
   * Authentication using a Vault token.
   * <p>
   * The Vault token is a secure string used to authenticate API requests
   * to the HashiCorp Vault. It can be provided through configuration parameters,
   * environment variables, or system properties.
   * </p>
   */
  VAULT_TOKEN{
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      String vaultToken = parameterSet.getRequiredWithFallback(
              DedicatedVaultParameters.VAULT_TOKEN, ENV_VAULT_TOKEN);

      OpaqueAccessToken opaqueToken = OpaqueAccessToken.create(vaultToken.toCharArray(), null);
      return new CachedToken(opaqueToken);
    }

    @Override
    public ParameterSet generateCacheKey(ParameterSet parameterSet) {
      ParameterSetBuilder keyBuilder = createCommonCacheKeyBuilder(parameterSet);

      String vaultToken = parameterSet.getRequiredWithFallback(DedicatedVaultParameters.VAULT_TOKEN,
              ENV_VAULT_TOKEN);
      keyBuilder.add(ENV_VAULT_TOKEN, DedicatedVaultParameters.VAULT_TOKEN, vaultToken);

      return keyBuilder.build();
    }
  },

  /**
   * Authentication using the Userpass method.
   * <p>
   * The Userpass method allows authentication using a username and password.
   * It is suitable for scenarios where user credentials are managed directly
   * by Vault. For more information, see the HashiCorp Vault documentation:
   * <a href="https://developer.hashicorp.com/vault/api-docs/auth/userpass">
   * Userpass Authentication API</a>.
   * </p>
   */
  USERPASS {
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      String vaultAddr = parameterSet.getRequiredWithFallback(VAULT_ADDR,
              ENV_VAULT_ADDR);
      String authPath = parameterSet.getOptionalWithFallback(
              USERPASS_AUTH_PATH, ENV_USERPASS_AUTH_PATH, DEFAULT_USERPASS_PATH);
      String namespace = parameterSet.getOptionalWithFallback(NAMESPACE,
              ENV_VAULT_NAMESPACE, DEFAULT_NAMESPACE);
      String username = parameterSet.getRequiredWithFallback(USERNAME, ENV_VAULT_USERNAME);
      String password = parameterSet.getRequiredWithFallback(PASSWORD, ENV_VAULT_PASSWORD);

      String authEndpoint = buildAuthEndpoint(vaultAddr, USERPASS_LOGIN_TEMPLATE, authPath, username);
      String payload = createJsonPayload(USERPASS_PAYLOAD_TEMPLATE, password);
      return performAuthentication(authEndpoint, payload, namespace, null, "Failed to authenticate using Userpass");
    }

    @Override
    public ParameterSet generateCacheKey(ParameterSet parameterSet) {
      ParameterSetBuilder keyBuilder = createCommonCacheKeyBuilder(parameterSet);

      String authPath = parameterSet.getOptionalWithFallback(
              USERPASS_AUTH_PATH, ENV_USERPASS_AUTH_PATH, DEFAULT_USERPASS_PATH);
      String username = parameterSet.getRequiredWithFallback(USERNAME, ENV_VAULT_USERNAME);
      String password = parameterSet.getRequiredWithFallback(PASSWORD, ENV_VAULT_PASSWORD);

      addNonDefaultPath(keyBuilder, authPath, DEFAULT_USERPASS_PATH, ENV_USERPASS_AUTH_PATH, USERPASS_AUTH_PATH);
      keyBuilder.add(ENV_VAULT_USERNAME, USERNAME, username);
      keyBuilder.add(ENV_VAULT_PASSWORD, PASSWORD, password);

      return keyBuilder.build();
    }
  },

  /**
   * Authentication using the AppRole method.
   * <p>
   * The AppRole method allows authentication using a Role ID and Secret ID.
   * This method is designed for machine-to-machine authentication or
   * service-based applications. For more information, see the HashiCorp Vault
   * documentation:
   * <a href="https://developer.hashicorp.com/vault/api-docs/auth/approle">
   * AppRole Authentication API</a>.
   * </p>
   */
  APPROLE{
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      String vaultAddr = parameterSet.getRequiredWithFallback(VAULT_ADDR,
              ENV_VAULT_ADDR);
      String namespace = parameterSet.getOptionalWithFallback(NAMESPACE,
              ENV_VAULT_NAMESPACE, DEFAULT_NAMESPACE);
      String roleId = parameterSet.getRequiredWithFallback(ROLE_ID, ENV_VAULT_ROLE_ID);
      String secretId = parameterSet.getRequiredWithFallback(SECRET_ID,
              ENV_VAULT_SECRET_ID);
      String authPath = parameterSet.getOptionalWithFallback(APPROLE_AUTH_PATH,
              ENV_APPROLE_AUTH_PATH, DEFAULT_APPROLE_PATH);

      String authEndpoint = buildAuthEndpoint(vaultAddr, APPROLE_LOGIN_TEMPLATE, authPath);
      String payload = createJsonPayload(APPROLE_PAYLOAD_TEMPLATE, roleId, secretId);
      return performAuthentication(authEndpoint, payload, namespace, null, "Failed to authenticate with AppRole");
    }

    @Override
    public ParameterSet generateCacheKey(ParameterSet parameterSet) {
      ParameterSetBuilder keyBuilder = createCommonCacheKeyBuilder(parameterSet);

      String roleId = parameterSet.getRequiredWithFallback(ROLE_ID, ENV_VAULT_ROLE_ID);
      String secretId = parameterSet.getRequiredWithFallback(SECRET_ID, ENV_VAULT_SECRET_ID);
      String authPath = parameterSet.getOptionalWithFallback(APPROLE_AUTH_PATH,
              ENV_APPROLE_AUTH_PATH, DEFAULT_APPROLE_PATH);

      addNonDefaultPath(keyBuilder, authPath, DEFAULT_APPROLE_PATH, ENV_APPROLE_AUTH_PATH, APPROLE_AUTH_PATH);
      keyBuilder.add(ENV_VAULT_ROLE_ID, ROLE_ID, roleId);
      keyBuilder.add(ENV_VAULT_SECRET_ID, SECRET_ID, secretId);

      return keyBuilder.build();
    }
  },

  /**
   * Authentication using the GitHub method.
   * <p>
   * The GitHub method allows authentication using a GitHub personal access token.
   * This is particularly useful for applications or developers using GitHub
   * as an identity provider for Vault. For more information, see:
   * <a href="https://developer.hashicorp.com/vault/docs/auth/github">
   * GitHub Authentication API</a>.
   * </p>
   */
  GITHUB{
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      String vaultAddr = parameterSet.getRequiredWithFallback(VAULT_ADDR, ENV_VAULT_ADDR);
      String githubToken = parameterSet.getRequiredWithFallback(GITHUB_TOKEN, ENV_GITHUB_TOKEN);
      String namespace = parameterSet.getOptionalWithFallback(NAMESPACE,
              ENV_VAULT_NAMESPACE, DEFAULT_NAMESPACE);
      String githubAuthPath = parameterSet.getOptionalWithFallback(
              GITHUB_AUTH_PATH, ENV_GITHUB_AUTH_PATH, DEFAULT_GITHUB_PATH);

      String authEndpoint = buildAuthEndpoint(vaultAddr, GITHUB_LOGIN_TEMPLATE, githubAuthPath);
      String payload = createJsonPayload(GITHUB_PAYLOAD_TEMPLATE, githubToken);
      return performAuthentication(authEndpoint, payload, namespace, githubToken, "Failed to authenticate with GitHub");
    }

    @Override
    public ParameterSet generateCacheKey(ParameterSet parameterSet) {
      ParameterSetBuilder keyBuilder = createCommonCacheKeyBuilder(parameterSet);

      String githubToken = parameterSet.getRequiredWithFallback( GITHUB_TOKEN, ENV_GITHUB_TOKEN);
      String githubAuthPath = parameterSet.getOptionalWithFallback(
              GITHUB_AUTH_PATH, ENV_GITHUB_AUTH_PATH, DEFAULT_GITHUB_PATH);

      addNonDefaultPath(keyBuilder, githubAuthPath, DEFAULT_GITHUB_PATH, ENV_GITHUB_AUTH_PATH, GITHUB_AUTH_PATH);
      keyBuilder.add(ENV_GITHUB_TOKEN, GITHUB_TOKEN, githubToken);

      return keyBuilder.build();
    }
  },

  /**
   * Automatically selects the best authentication method based on available parameters.
   *
   * <p>Priority order:</p>
   * <ol>
   *   <li>Uses the Vault token if available.</li>
   *   <li>Falls back to Userpass authentication.</li>
   *   <li>Then attempts AppRole authentication.</li>
   *   <li>Finally, tries GitHub authentication.</li>
   * </ol>
   */
  AUTO_DETECT{
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      IllegalStateException previousFailure;

      // Attempt VAULT_TOKEN authentication first
      try {
        return VAULT_TOKEN.generateToken(parameterSet);
      } catch (RuntimeException noVaultToken) {
        previousFailure = new IllegalStateException(
                "Failed to authenticate using a Vault token", noVaultToken);
      }

      // Attempt USERPASS authentication
      try {
        return USERPASS.generateToken(parameterSet);
      } catch (RuntimeException noUserpass) {
        previousFailure.addSuppressed(new IllegalStateException(
                "Failed to authenticate using Userpass credentials", noUserpass));
      }

      // Attempt APPROLE authentication
      try {
        return APPROLE.generateToken(parameterSet);
      } catch (RuntimeException noAppRole) {
        previousFailure.addSuppressed(new IllegalStateException(
                "Failed to authenticate using AppRole credentials", noAppRole));
      }

      // Attempt GITHUB authentication
      try {
        return GITHUB.generateToken(parameterSet);
      } catch (RuntimeException noGitHub) {
        previousFailure.addSuppressed(new IllegalStateException(
                "Failed to authenticate using GitHub credentials", noGitHub));
      }

      // If all methods fail, throw an error
      throw previousFailure;
    }

    @Override
    public ParameterSet generateCacheKey(ParameterSet parameterSet) {
      ParameterSetBuilder keyBuilder = createCommonCacheKeyBuilder(parameterSet);
      // VAULT_TOKEN
      try {
        String vaultToken = parameterSet.getRequiredWithFallback(DedicatedVaultParameters.VAULT_TOKEN, ENV_VAULT_TOKEN);
        keyBuilder.add(ENV_VAULT_TOKEN, DedicatedVaultParameters.VAULT_TOKEN, vaultToken);
      } catch (Exception ignored) {}

      // USERPASS
      try {
        String authPath = parameterSet.getOptionalWithFallback(USERPASS_AUTH_PATH,
                ENV_USERPASS_AUTH_PATH, DEFAULT_USERPASS_PATH);
        addNonDefaultPath(keyBuilder, authPath, DEFAULT_USERPASS_PATH, ENV_USERPASS_AUTH_PATH, USERPASS_AUTH_PATH);

        String username = parameterSet.getRequiredWithFallback(USERNAME, ENV_VAULT_USERNAME);
        String password = parameterSet.getRequiredWithFallback(PASSWORD, ENV_VAULT_PASSWORD);
        keyBuilder.add(ENV_VAULT_USERNAME, USERNAME, username);
        keyBuilder.add(ENV_VAULT_PASSWORD, PASSWORD, password);
      } catch (Exception ignored) {}

      // APPROLE
      try {
        String authPath = parameterSet.getOptionalWithFallback(APPROLE_AUTH_PATH,
                ENV_APPROLE_AUTH_PATH, DEFAULT_APPROLE_PATH);
        addNonDefaultPath(keyBuilder, authPath, DEFAULT_APPROLE_PATH, ENV_APPROLE_AUTH_PATH, APPROLE_AUTH_PATH);

        String roleId = parameterSet.getRequiredWithFallback(ROLE_ID, ENV_VAULT_ROLE_ID);
        String secretId = parameterSet.getRequiredWithFallback(SECRET_ID, ENV_VAULT_SECRET_ID);
        keyBuilder.add(ENV_VAULT_ROLE_ID, ROLE_ID, roleId);
        keyBuilder.add(ENV_VAULT_SECRET_ID, SECRET_ID, secretId);
      } catch (Exception ignored) {}

      // GITHUB
      try {
        String authPath = parameterSet.getOptionalWithFallback(GITHUB_AUTH_PATH,
                ENV_GITHUB_AUTH_PATH, DEFAULT_GITHUB_PATH);
        addNonDefaultPath(keyBuilder, authPath, DEFAULT_GITHUB_PATH, ENV_GITHUB_AUTH_PATH, GITHUB_AUTH_PATH);

        String githubToken = parameterSet.getRequiredWithFallback(GITHUB_TOKEN, ENV_GITHUB_TOKEN);
        keyBuilder.add(ENV_GITHUB_TOKEN, GITHUB_TOKEN, githubToken);
      } catch (Exception ignored) {}

      return keyBuilder.build();
    }
  };

  // Common constants for System Properties and environment variable names
  public static final String ENV_VAULT_ADDR = "VAULT_ADDR";
  private static final String ENV_VAULT_TOKEN = "VAULT_TOKEN";
  private static final String ENV_VAULT_NAMESPACE = "VAULT_NAMESPACE";
  private static final String ENV_VAULT_USERNAME = "VAULT_USERNAME";
  private static final String ENV_VAULT_PASSWORD = "VAULT_PASSWORD";
  private static final String ENV_GITHUB_TOKEN = "GITHUB_TOKEN";
  private static final String ENV_USERPASS_AUTH_PATH = "USERPASS_AUTH_PATH";
  private static final String ENV_APPROLE_AUTH_PATH = "APPROLE_AUTH_PATH";
  private static final String ENV_GITHUB_AUTH_PATH = "GITHUB_AUTH_PATH";
  private static final String ENV_VAULT_ROLE_ID = "VAULT_ROLE_ID";
  private static final String ENV_VAULT_SECRET_ID = "VAULT_SECRET_ID";

  // Default values
  private static final String DEFAULT_NAMESPACE = "admin";
  private static final String DEFAULT_USERPASS_PATH = "userpass";
  private static final String DEFAULT_APPROLE_PATH = "approle";
  private static final String DEFAULT_GITHUB_PATH = "github";

  // Path templates
  private static final String AUTH_PATH_TEMPLATE = "/v1/auth/%s";
  private static final String USERPASS_LOGIN_TEMPLATE = AUTH_PATH_TEMPLATE + "/login/%s";
  private static final String APPROLE_LOGIN_TEMPLATE = AUTH_PATH_TEMPLATE + "/login";
  private static final String GITHUB_LOGIN_TEMPLATE = AUTH_PATH_TEMPLATE + "/login";

  // JSON Payload templates
  private static final String USERPASS_PAYLOAD_TEMPLATE = "{\"password\": \"%s\"}";
  private static final String APPROLE_PAYLOAD_TEMPLATE = "{\"role_id\":\"%s\", \"secret_id\":\"%s\"}";
  private static final String GITHUB_PAYLOAD_TEMPLATE = "{\"token\": \"%s\"}";

  // Content types
  private static final String JSON_CONTENT_TYPE = "application/json";

  /**
   * Creates a ParameterSetBuilder with common cache key parameters.
   *
   * @param parameterSet the source parameter set
   * @return a builder with common parameters added
   */
  protected static ParameterSetBuilder createCommonCacheKeyBuilder(ParameterSet parameterSet) {
    ParameterSetBuilder keyBuilder = ParameterSet.builder();

    String vaultAddr = parameterSet.getRequiredWithFallback(VAULT_ADDR, ENV_VAULT_ADDR);
    keyBuilder.add(ENV_VAULT_ADDR, VAULT_ADDR, vaultAddr);

    String namespace = parameterSet.getOptionalWithFallback(NAMESPACE,
            ENV_VAULT_NAMESPACE, DEFAULT_NAMESPACE);
    keyBuilder.add(ENV_VAULT_NAMESPACE, NAMESPACE, namespace);

    return keyBuilder;
  }

  /**
   * Builds an authentication endpoint URL.
   *
   * @param vaultAddr The Vault server address
   * @param template The URL template to use
   * @param args Arguments to fill into the template
   * @return The complete endpoint URL
   */
  protected static String buildAuthEndpoint(String vaultAddr, String template, Object... args) {
    return vaultAddr + String.format(template, args);
  }

  /**
   * Creates a JSON payload string.
   *
   * @param format The format string with JSON structure
   * @param args The values to insert into the format string
   * @return A JSON payload string
   */
  protected static String createJsonPayload(String format, Object... args) {
    return String.format(format, args);
  }

  /**
   * Adds a non-default path parameter to the cache key if it differs from the default.
   *
   * @param keyBuilder The key builder to add to
   * @param path The path value
   * @param defaultPath The default path value
   * @param envVar The environment variable name
   * @param parameter The parameter object
   */
  protected static void addNonDefaultPath(
          ParameterSetBuilder keyBuilder, String path, String defaultPath,
          String envVar, Parameter<String> parameter) {
    if (path != null && !path.isEmpty() && !path.equals(defaultPath)) {
      keyBuilder.add(envVar, parameter, path);
    }
  }

  /**
   * Generates a token for the authentication method.
   *
   * @param parameterSet the set of parameters for the request.
   * @return the generated token.
   */
  public abstract CachedToken generateToken(ParameterSet parameterSet);

  /**
   * Generates a cache key for the authentication method based on the provided parameters.
   *
   * @param parameterSet the set of parameters for the request.
   * @return a ParameterSet to be used as a cache key.
   */
  public abstract ParameterSet generateCacheKey(ParameterSet parameterSet);

  /**
   * Helper method that consolidates the common authentication steps.
   * <p>
   * This method performs the following:
   * <ol>
   *   <li>Creates an HTTP connection to the specified authentication endpoint.</li>
   *   <li>Sends the provided JSON payload.</li>
   *   <li>Parses the JSON response to extract the client token and lease duration.</li>
   *   <li>Constructs and returns a new {@link CachedToken} based on the response.</li>
   * </ol>
   *
   * @param authEndpoint the full URL of the Vault authentication endpoint.
   * @param payload the JSON payload to send in the request.
   * @param namespace the Vault namespace to include in the request headers.
   * @param authToken an optional token to include in the "Authorization" header (used for GitHub authentication).
   * @param failureMessage a descriptive error message used if authentication fails.
   * @return a new {@link CachedToken} containing the client token and its expiration details.
   * @throws IllegalStateException if the authentication request fails or if the response is malformed.
   */
  protected static CachedToken performAuthentication(
          String authEndpoint,
          String payload,
          String namespace,
          String authToken,
          String failureMessage) {
    try {
      String jsonResponse = HttpUtil.sendPostRequest(authEndpoint, payload, JSON_CONTENT_TYPE, authToken, namespace);
      OracleJsonObject response = JsonUtil.convertJsonToOracleJsonObject(jsonResponse);
      OracleJsonObject authObj = response.getObject("auth");
      String clientToken = JsonUtil.extractField(authObj,"client_token");
      long leaseDurationInSeconds = authObj.getLong("lease_duration");

      OffsetDateTime expiration = OffsetDateTime.now().plusSeconds(leaseDurationInSeconds);
      OpaqueAccessToken opaqueToken = OpaqueAccessToken.create(clientToken.toCharArray(), expiration);

      return new CachedToken(opaqueToken);
    } catch (Exception e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }
}
