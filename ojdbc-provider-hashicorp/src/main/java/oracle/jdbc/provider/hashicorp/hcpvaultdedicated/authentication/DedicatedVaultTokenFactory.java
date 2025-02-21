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

import oracle.jdbc.driver.oauth.OpaqueAccessToken;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.HttpUtil;
import oracle.jdbc.provider.hashicorp.JsonUtil;
import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.secrets.DedicatedVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetBuilder;
import oracle.sql.json.OracleJsonObject;
import java.net.HttpURLConnection;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.secrets.DedicatedVaultSecretsManagerFactory.*;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getOptionalOrFallback;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

/**
 * <p>
 * Factory for creating {@link DedicatedVaultToken} objects for authenticating
 * with Dedicated HashiCorp Vault.
 * </p><p>
 * This factory determines the appropriate authentication method based on the provided
 * {@link ParameterSet} and creates credentials accordingly.
 * </p>
 */
public final class DedicatedVaultTokenFactory
        implements ResourceFactory<DedicatedVaultToken> {

  /**
   * <p>
   * Parameter for configuring the authentication method.
   * Must be provided in the {@link ParameterSet}.
   * </p>
   */
  public static final Parameter<DedicatedVaultAuthenticationMethod> AUTHENTICATION_METHOD =
          Parameter.create(REQUIRED);

  // Map to cache tokens based on generated cache keys
  private static final ConcurrentHashMap<ParameterSet, CachedToken> tokenCache = new ConcurrentHashMap<>();

  // 1 minutes buffer for token expiration (in ms)
  private static final long TOKEN_TTL_BUFFER = 60_000;

  private static final DedicatedVaultTokenFactory INSTANCE =
          new DedicatedVaultTokenFactory();

  private DedicatedVaultTokenFactory() {
  }

  /**
   * Returns a singleton instance of {@code DedicatedVaultCredentialsFactory}.
   *
   * @return a singleton instance. Not null.
   */
  public static DedicatedVaultTokenFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<DedicatedVaultToken> request(ParameterSet parameterSet) {
    DedicatedVaultToken credentials = getCredential(parameterSet);
    return Resource.createPermanentResource(credentials, true);
  }

  /**
   * Determines the appropriate credentials based on the provided parameters.
   *
   * @param parameterSet the set of parameters configuring the request. Must
   *                     not be null.
   * @return the created {@code DedicatedVaultCredentials} instance.
   */
  private static DedicatedVaultToken getCredential(ParameterSet parameterSet) {
    // Check which authentication method is requested
    DedicatedVaultAuthenticationMethod method =
            parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (method) {
      case VAULT_TOKEN:
        return createTokenCredentials(parameterSet);
      case USERPASS:
        return createScopedToken(parameterSet, method, DedicatedVaultTokenFactory::createUserpassToken);
      case APPROLE:
        return createScopedToken(parameterSet, method, DedicatedVaultTokenFactory::createAppRoleToken);
      case GITHUB:
        return createScopedToken(parameterSet, method, DedicatedVaultTokenFactory::createGitHubToken);
      case AUTO_DETECT:
        return autoDetectAuthentication(parameterSet);
      default:
        throw new IllegalArgumentException(
                "Unrecognized authentication method: " + method);
    }
  }

  /**
   * Creates {@link DedicatedVaultToken} using the Vault token.
   *
   * @param parameterSet the set of parameters containing the Vault token. Must not be null.
   * @return the created {@code DedicatedVaultCredentials} instance.
   */
  private static DedicatedVaultToken createTokenCredentials(ParameterSet parameterSet) {
    String vaultToken = getRequiredOrFallback(
            parameterSet,
            DedicatedVaultSecretsManagerFactory.VAULT_TOKEN,
            "VAULT_TOKEN"
    );

    if (vaultToken == null || vaultToken.isEmpty()) {
      throw new IllegalStateException("Vault Token not found in parameters, " +
              "system properties, or environment variables");
    }
    return new DedicatedVaultToken(vaultToken);
  }

  /**
   * Creates or retrieves a cached {@link DedicatedVaultToken} using
   * a scoped token.
   *
   * @param parameterSet the set of parameters for the request.
   * @param method the authentication method being used.
   * @param generator the token generator function.
   * @return a {@code DedicatedVaultCredentials} instance.
   */
  private static DedicatedVaultToken createScopedToken(
          ParameterSet parameterSet,
          DedicatedVaultAuthenticationMethod method,
          TokenGenerator generator
  ) {
    ParameterSet cacheKey = generateCacheKey(parameterSet, method);
    long currentTime = System.currentTimeMillis();

    CachedToken validCachedToken = tokenCache.compute(cacheKey, (k, cachedToken) -> {
      if (cachedToken == null || !cachedToken.isValid(currentTime, TOKEN_TTL_BUFFER)) {
        return generator.generate(parameterSet);
      }
      return cachedToken;
    });

    if (validCachedToken.getToken() instanceof OpaqueAccessToken) {
      OpaqueAccessToken opaqueToken = (OpaqueAccessToken) validCachedToken.getToken();
      return new DedicatedVaultToken(opaqueToken.token().get());
    } else {
      throw new IllegalStateException("Cached token is not an instance of OpaqueAccessToken");
    }
  }

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
   * </p>
   *
   * @param authEndpoint the full URL of the Vault authentication endpoint.
   * @param payload the JSON payload to send in the request.
   * @param namespace the Vault namespace to include in the request headers.
   * @param authToken an optional token to include in the "Authorization" header (used for GitHub authentication).
   * @param failureMessage a descriptive error message used if authentication fails.
   * @return a new {@link CachedToken} containing the client token and its expiration details.
   * @throws IllegalStateException if the authentication request fails or if the response is malformed.
   */
  private static CachedToken performAuthentication(
          String authEndpoint,
          String payload,
          String namespace,
          String authToken,
          String failureMessage) {
    try {
      HttpURLConnection conn = HttpUtil.createConnection(authEndpoint, "POST",
              "application/json", authToken, namespace);
      String jsonResponse = HttpUtil.sendPayloadAndGetResponse(conn, payload);
      OracleJsonObject response = JsonUtil.parseJsonResponse(jsonResponse);
      OracleJsonObject authObj = response.getObject("auth");
      String clientToken = authObj.getString("client_token");
      long leaseDurationInSeconds = authObj.getLong("lease_duration");
      OffsetDateTime expiration = OffsetDateTime.now().plusSeconds(leaseDurationInSeconds);
      return new CachedToken(OpaqueAccessToken.create(clientToken.toCharArray(), expiration), leaseDurationInSeconds);
    } catch (Exception e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }

  /**
   * Authenticates with the Vault using the Userpass method.
   *
   * @param parameterSet the set of parameters for the request.
   * @return a {@link CachedToken} containing the client token and its expiration details.
   * @throws IllegalStateException if authentication fails.
   */
  private static CachedToken createUserpassToken(ParameterSet parameterSet) {
    String vaultAddr = getRequiredOrFallback(parameterSet, VAULT_ADDR, "VAULT_ADDR");
    String authPath = getOptionalOrFallback(parameterSet, USERPASS_AUTH_PATH,
            "USERPASS_AUTH_PATH", "userpass");
    String namespace = getOptionalOrFallback(parameterSet, NAMESPACE,
            "VAULT_NAMESPACE", "admin");
    String username = getRequiredOrFallback(parameterSet, USERNAME, "VAULT_USERNAME");
    String password = getRequiredOrFallback(parameterSet, PASSWORD, "VAULT_PASSWORD");

    String authEndpoint = vaultAddr + "/v1/auth/" + authPath + "/login/" + username;
    String payload = String.format("{\"password\": \"%s\"}", password);
    return performAuthentication(authEndpoint, payload, namespace, null, "Failed to authenticate using Userpass");  }

  /**
   * Authenticates with the Vault using the AppRole method.
   *
   * @param parameterSet the set of parameters for the request.
   * @return a {@link CachedToken} containing the client token and its expiration details.
   * @throws IllegalStateException if authentication fails.
   */
  private static CachedToken createAppRoleToken(ParameterSet parameterSet) {
    String vaultAddr = getRequiredOrFallback(parameterSet, VAULT_ADDR, "VAULT_ADDR");
    String namespace = getOptionalOrFallback(parameterSet, NAMESPACE,
            "VAULT_NAMESPACE", "admin");
    String roleId = getRequiredOrFallback(parameterSet, ROLE_ID, "VAULT_ROLE_ID");
    String secretId = getRequiredOrFallback(parameterSet, SECRET_ID, "VAULT_SECRET_ID");
    String authPath = getOptionalOrFallback(parameterSet, APPROLE_AUTH_PATH,
            "APPROLE_AUTH_PATH", "approle");

    String authEndpoint = vaultAddr + "/v1/auth/" + authPath + "/login";
    String payload = String.format("{\"role_id\":\"%s\", \"secret_id\":\"%s\"}", roleId, secretId);
    return performAuthentication(authEndpoint, payload, namespace, null, "Failed to authenticate with AppRole");
  }

  /**
   * Authenticates with the Vault using the GitHub method.
   *
   * @param parameterSet the set of parameters for the request.
   * @return a {@link CachedToken} containing the client token and its expiration details.
   * @throws IllegalStateException if authentication fails.
   */
  private static CachedToken createGitHubToken(ParameterSet parameterSet) {
    String vaultAddr = getRequiredOrFallback(parameterSet, VAULT_ADDR, "VAULT_ADDR");
    String githubToken = getRequiredOrFallback(parameterSet, GITHUB_TOKEN, "GITHUB_TOKEN");
    String namespace = getOptionalOrFallback(parameterSet, NAMESPACE,
            "VAULT_NAMESPACE", "admin");
    String githubAuthPath = getOptionalOrFallback(parameterSet,
            GITHUB_AUTH_PATH, "GITHUB_AUTH_PATH", "github");

    String authEndpoint = vaultAddr + "/v1/auth/" + githubAuthPath + "/login";
    String payload = String.format("{\"token\": \"%s\"}", githubToken);
    return performAuthentication(authEndpoint, payload, namespace, githubToken, "Failed to authenticate with GitHub");
  }

  /**
   * Generates a unique cache key based on the provided {@link ParameterSet}
   * and authentication method.
   *
   * @param parameterSet the parameters for the request.
   * @param method the authentication method.
   * @return a {@code ParameterSet} representing the cache key.
   */
  private static ParameterSet generateCacheKey(ParameterSet parameterSet, DedicatedVaultAuthenticationMethod method) {
    ParameterSetBuilder keyBuilder = ParameterSet.builder();
    String vaultAddr = getRequiredOrFallback(parameterSet, VAULT_ADDR, "VAULT_ADDR");
    keyBuilder.add("VAULT_ADDR", VAULT_ADDR, vaultAddr);

    String namespace = getOptionalOrFallback(parameterSet, NAMESPACE,
            "VAULT_NAMESPACE", "admin");

    keyBuilder.add("VAULT_NAMESPACE", NAMESPACE, namespace);

    switch (method) {
      case APPROLE: {
        String roleId = getRequiredOrFallback(parameterSet, ROLE_ID, "VAULT_ROLE_ID");
        String secretId = getRequiredOrFallback(parameterSet, SECRET_ID, "VAULT_SECRET_ID");
        String authPath = getOptionalOrFallback(parameterSet,
                APPROLE_AUTH_PATH, "APPROLE_AUTH_PATH", "approle");
        if (authPath != null && !authPath.isEmpty()) {
          keyBuilder.add("APPROLE_AUTH_PATH", APPROLE_AUTH_PATH, authPath);
        }
        keyBuilder.add("VAULT_ROLE_ID", ROLE_ID, roleId);
        keyBuilder.add("VAULT_SECRET_ID", SECRET_ID, secretId);
        break;
      }
      case USERPASS: {
        String authPath = getOptionalOrFallback(parameterSet,
                USERPASS_AUTH_PATH, "USERPASS_AUTH_PATH", "userpass");
        String username = getRequiredOrFallback(parameterSet, USERNAME, "VAULT_USERNAME");
        String password = getRequiredOrFallback(parameterSet, PASSWORD, "VAULT_PASSWORD");
        if (authPath != null && !authPath.isEmpty()) {
          keyBuilder.add("USERPASS_AUTH_PATH", USERPASS_AUTH_PATH, authPath);
        }
        keyBuilder.add("VAULT_USERNAME", USERNAME, username);
        keyBuilder.add("VAULT_PASSWORD", PASSWORD, password);
        break;
      }
      case GITHUB: {
        String githubToken = getRequiredOrFallback(parameterSet, GITHUB_TOKEN, "GITHUB_TOKEN");
        String githubAuthPath = getOptionalOrFallback(parameterSet,
                GITHUB_AUTH_PATH, "GITHUB_AUTH_PATH", "github");
        if (githubAuthPath != null && !githubAuthPath.isEmpty()) {
          keyBuilder.add("GITHUB_AUTH_PATH", GITHUB_AUTH_PATH, githubAuthPath);
        }
        keyBuilder.add("GITHUB_TOKEN", GITHUB_TOKEN, githubToken);
        break;
      }
      default: {
        break;
      }
    }
    return keyBuilder.build();
  }

  /**
   * Automatically detects the best authentication method based on available
   * parameters.
   *
   * <p>Priority order:</p>
   * <ol>
   *   <li>Uses the Vault token if available.</li>
   *   <li>Falls back to Userpass authentication.</li>
   *   <li>Then attempts AppRole authentication.</li>
   *   <li>Finally, tries GitHub authentication.</li>
   * </ol>
   *
   * @param parameterSet The parameter set containing authentication details.
   * @return The detected authentication token.
   */
  private static DedicatedVaultToken autoDetectAuthentication(ParameterSet parameterSet) {
    IllegalStateException previousFailure;

    // Attempt VAULT_TOKEN authentication first
    try {
      return createTokenCredentials(parameterSet);
    } catch (RuntimeException noVaultToken) {
      previousFailure = new IllegalStateException(
              "Failed to authenticate using a Vault token",
              noVaultToken);
    }

    // Attempt USERPASS authentication
    try {
      return createScopedToken(parameterSet, DedicatedVaultAuthenticationMethod.USERPASS, DedicatedVaultTokenFactory::createUserpassToken);
    } catch (RuntimeException noUserpass) {
      previousFailure.addSuppressed(new IllegalStateException(
              "Failed to authenticate using Userpass credentials",
              noUserpass));
    }

    // Attempt APPROLE authentication
    try {
      return createScopedToken(parameterSet, DedicatedVaultAuthenticationMethod.APPROLE, DedicatedVaultTokenFactory::createAppRoleToken);
    } catch (RuntimeException noAppRole) {
      previousFailure.addSuppressed(new IllegalStateException(
              "Failed to authenticate using AppRole credentials",
              noAppRole));
    }

    // Attempt GITHUB authentication
    try {
      return createScopedToken(parameterSet, DedicatedVaultAuthenticationMethod.GITHUB, DedicatedVaultTokenFactory::createGitHubToken);
    } catch (RuntimeException noGitHub) {
      previousFailure.addSuppressed(new IllegalStateException(
              "Failed to authenticate using GitHub credentials",
              noGitHub));
    }

    // If all methods fail, throw an error
    throw previousFailure;
  }

  @FunctionalInterface
  private interface TokenGenerator {
    CachedToken generate(ParameterSet parameterSet);
  }

}
