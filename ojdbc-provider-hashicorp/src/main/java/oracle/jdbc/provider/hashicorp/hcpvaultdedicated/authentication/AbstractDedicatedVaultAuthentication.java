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
 * Base class for Dedicated Vault authentication.
 * <p>
 * Provides helper methods and shared constants for building endpoints, creating JSON payloads,
 * and performing HTTP authentication requests to Vault.
 * </p>
 */
public abstract class AbstractDedicatedVaultAuthentication {

  // Shared constants
  protected static final String AUTH_PATH_TEMPLATE = "/v1/auth/%s";
  protected static final String USERPASS_LOGIN_TEMPLATE = AUTH_PATH_TEMPLATE + "/login/%s";
  protected static final String APPROLE_LOGIN_TEMPLATE = AUTH_PATH_TEMPLATE + "/login";
  protected static final String GITHUB_LOGIN_TEMPLATE = AUTH_PATH_TEMPLATE + "/login";

  protected static final String USERPASS_PAYLOAD_TEMPLATE = "{\"password\": \"%s\"}";
  protected static final String APPROLE_PAYLOAD_TEMPLATE = "{\"role_id\":\"%s\", \"secret_id\":\"%s\"}";
  protected static final String GITHUB_PAYLOAD_TEMPLATE = "{\"token\": \"%s\"}";

  protected static final String JSON_CONTENT_TYPE = "application/json";
  private static final String AUTH_FIELD = "auth";
  private static final String CLIENT_TOKEN_FIELD = "client_token";
  private static final String LEASE_DURATION_FIELD = "lease_duration";

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
   * Creates a ParameterSetBuilder with common cache key parameters.
   *
   * @param parameterSet the source parameter set
   * @return a builder with common parameters added
   */
  protected ParameterSetBuilder createCommonCacheKeyBuilder(ParameterSet parameterSet) {
    ParameterSetBuilder keyBuilder = ParameterSet.builder();

    String vaultAddr = DedicatedVaultParameters.getVaultAddress(parameterSet);
    keyBuilder.add(ENV_VAULT_ADDR, DedicatedVaultParameters.VAULT_ADDR, vaultAddr);

    String namespace = DedicatedVaultParameters.getNamespace(parameterSet);
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
  protected String buildAuthEndpoint(String vaultAddr, String template, Object... args) {
    return vaultAddr + String.format(template, args);
  }

  /**
   * Creates a JSON payload string.
   *
   * @param format The format string with JSON structure
   * @param args The values to insert into the format string
   * @return A JSON payload string
   */
  protected String createJsonPayload(String format, Object... args) {
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
  protected void addNonDefaultPath(ParameterSetBuilder keyBuilder, String path, String defaultPath,
                                   String envVar, Parameter<String> parameter) {
    if (path != null && !path.isEmpty() && !path.equals(defaultPath)) {
      keyBuilder.add(envVar, parameter, path);
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
   *
   * @param authEndpoint the full URL of the Vault authentication endpoint.
   * @param payload the JSON payload to send in the request.
   * @param namespace the Vault namespace to include in the request headers.
   * @param authToken an optional token to include in the "Authorization" header (used for GitHub authentication).
   * @param failureMessage a descriptive error message used if authentication fails.
   * @return a new {@link CachedToken} containing the client token and its expiration details.
   * @throws IllegalStateException if the authentication request fails or if the response is malformed.
   */
  protected CachedToken performAuthentication(String authEndpoint,
                                              String payload,
                                              String namespace,
                                              String authToken,
                                              String failureMessage) {
    try {
      String jsonResponse = HttpUtil.sendPostRequest(authEndpoint, payload, JSON_CONTENT_TYPE, authToken, namespace);
      OracleJsonObject response = JsonUtil.convertJsonToOracleJsonObject(jsonResponse);
      OracleJsonObject authObj = response.getObject(AUTH_FIELD);
      String clientToken = JsonUtil.extractField(authObj, CLIENT_TOKEN_FIELD);
      long leaseDurationInSeconds = authObj.getLong(LEASE_DURATION_FIELD);

      OffsetDateTime expiration = OffsetDateTime.now().plusSeconds(leaseDurationInSeconds);
      OpaqueAccessToken opaqueToken = OpaqueAccessToken.create(clientToken.toCharArray(), expiration);

      return new CachedToken(opaqueToken);
    } catch (Exception e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }
}

