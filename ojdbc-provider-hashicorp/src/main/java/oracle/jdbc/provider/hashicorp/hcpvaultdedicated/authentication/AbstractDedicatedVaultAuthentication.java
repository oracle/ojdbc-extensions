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
import oracle.jdbc.provider.hashicorp.util.HttpUtil;
import oracle.jdbc.provider.hashicorp.util.JsonUtil;
import oracle.jdbc.provider.hashicorp.util.VaultUrlBuilder;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.sql.json.OracleJsonObject;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class for Dedicated Vault authentication.
 * <p>
 * Provides helper methods and shared constants for building endpoints,
 * creating JSON payloads,
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
   * @return a map of key parts used as cache key material.
   */
  public abstract Map<String, Object> generateCacheKey(ParameterSet parameterSet);

  /**
   * Builds an authentication endpoint URL.
   *
   * @param vaultAddr The Vault server address
   * @param template The URL template to use
   * @param args Arguments to fill into the template
   * @return The complete endpoint URL
   */
  protected String buildAuthEndpoint(String vaultAddr, String template, Object... args) {
    if (vaultAddr == null || vaultAddr.isEmpty()) {
      throw new IllegalArgumentException("vaultAddr must not be null or empty");
    }
    if (template == null || template.isEmpty()) {
      throw new IllegalArgumentException("template must not be null or empty");
    }
    if (args == null || args.length == 0) {
      throw new IllegalArgumentException("Missing auth path argument");
    }

    URI baseUri = VaultUrlBuilder.parseBaseUri(vaultAddr);

    Object[] safeArgs = new Object[args.length];
    safeArgs[0] = sanitizeAndEncodeAuthMountPath(String.valueOf(args[0]));
    for (int i = 1; i < args.length; i++) {
      safeArgs[i] = sanitizeAndEncodePathSegment(
              String.valueOf(args[i]), "path segment");
    }

    String templatePath = String.format(template, safeArgs);
    String endpointPath = VaultUrlBuilder.joinPaths(baseUri.getPath(), templatePath);
    return VaultUrlBuilder.buildCanonicalUrl(
            baseUri, endpointPath, "Failed to build Vault auth endpoint");
  }

  /**
   * Validates and encodes mount path segments.
   *
   * @param authPath Authentication mount path. Not null/empty.
   * @return Encoded mount path suitable for safe URL composition.
   */
  private static String sanitizeAndEncodeAuthMountPath(String authPath) {
    if (authPath == null || authPath.isEmpty()) {
      throw new IllegalArgumentException("authPath must not be null or empty");
    }
    VaultUrlBuilder.rejectDangerousPathTokens(authPath, "authPath");

    String[] segments = authPath.split("/", -1);
    List<String> encodedSegments = new ArrayList<>(segments.length);
    for (String segment : segments) {
      encodedSegments.add(
              sanitizeAndEncodePathSegment(segment, "authPath segment"));
    }
    return String.join("/", encodedSegments);
  }

  /**
   * Validates and encodes one URI path segment.
   * Reused for both:
   * - segments derived from auth mount paths, and
   * - direct endpoint arguments (for example login path segments).
   *
   * @param value Segment value. Not null/empty.
   * @param name Field label for error messages.
   * @return Encoded segment value.
   */
  private static String sanitizeAndEncodePathSegment(
          String value, String name) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be null or empty");
    }
    if (".".equals(value) || "..".equals(value)) {
      throw new IllegalArgumentException("Invalid " + name + ": " + value);
    }
    VaultUrlBuilder.rejectDangerousPathTokens(value, name);
    if (value.indexOf('/') >= 0) {
      throw new IllegalArgumentException("Invalid " + name + ": " + value);
    }

    try {
      return new URI(null, null, "/" + value, null).getRawPath().substring(1);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid " + name + ": " + value, e);
    }
  }

  /**
   * Creates a JSON payload string.
   *
   * @param format The format string with JSON structure
   * @param args The values to insert into the format string
   * @return A JSON payload string
   */
  protected String createJsonPayload(String format, Object... args) {
    Object[] escapedArgs = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];
      escapedArgs[i] = (arg == null)
              ? null
              : escapeJsonString(String.valueOf(arg));
    }
    return String.format(format, escapedArgs);
  }

  /**
   * Escapes a value for safe embedding inside a JSON string literal.
   *
   * @param value Plain text to escape.
   * @return Escaped JSON-string-safe text.
   */
  private static String escapeJsonString(String value) {
    StringBuilder escaped = new StringBuilder(value.length() + 16);
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '"':
          escaped.append("\\\"");
          break;
        case '\\':
          escaped.append("\\\\");
          break;
        default:
          if (ch < 0x20) {
            escaped.append(String.format("\\u%04x", (int) ch));
          } else {
            escaped.append(ch);
          }
          break;
      }
    }
    return escaped.toString();
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
   * @param failureMessage a descriptive error message used if authentication fails.
   * @return a new {@link CachedToken} containing the client token and its expiration details.
   * @throws IllegalStateException if the authentication request fails or if the response is malformed.
   */
  protected CachedToken performAuthentication(String authEndpoint,
                                              String payload,
                                              String namespace,
                                              String failureMessage) {
    try {
      String jsonResponse = HttpUtil.sendPostRequest(authEndpoint, payload, JSON_CONTENT_TYPE, namespace);
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
