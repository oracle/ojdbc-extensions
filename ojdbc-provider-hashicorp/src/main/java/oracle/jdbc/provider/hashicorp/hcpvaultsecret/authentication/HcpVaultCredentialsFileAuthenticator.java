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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication;

import oracle.jdbc.provider.hashicorp.HttpUtil;
import oracle.jdbc.provider.hashicorp.JsonUtil;
import oracle.sql.json.OracleJsonNumber;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles authentication using the HashiCorp CLI credentials cache.
 * <p>
 * This class reads the authentication details from the CLI-generated credentials file
 * (`creds-cache.json`) and manages the token lifecycle, including:
 * </p>
 * <ul>
 *   <li>Validating the access token's expiration.</li>
 *   <li>Refreshing the token using the stored refresh token when expired.</li>
 *   <li>Updating the credentials file with the new token details.</li>
 * </ul>
 * <p>
 * By default, the credentials file is expected at:
 * <code>System.getProperty("user.home") + "/.config/hcp/creds-cache.json"</code>.
 * However, users can provide a custom file path through configuration.
 * </p>
 */
public final class HcpVaultCredentialsFileAuthenticator {
  private static final String TOKEN_URL = "https://auth.idp.hashicorp.com/oauth2/token";
  private static final String GRANT_TYPE = "refresh_token";
  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String TOKEN_REFRESH_PAYLOAD_FORMAT = "grant_type=%s&refresh_token=%s&client_id=%s";
  private static final String CREDENTIALS_JSON_FORMAT =
          "{ \"login\": { \"access_token\": \"%s\", \"refresh_token\": \"%s\", \"access_token_expiry\": \"%s\" } }";

  // JSON field constants
  public static final String ACCESS_TOKEN_FIELD = "access_token";
  private static final String REFRESH_TOKEN_FIELD = "refresh_token";
  private static final String ACCESS_TOKEN_EXPIRY_FIELD = "access_token_expiry";
  private static final String EXPIRES_IN_FIELD = "expires_in";
  private static final String CLIENT_ID_FIELD = "client_id";
  private static final String LOGIN_FIELD = "login";

  private final ReentrantLock lock = new ReentrantLock();

  private volatile String accessToken;
  private volatile String refreshToken;
  private volatile Instant tokenExpiry;

  private final Path credsFilePath;

  /**
   * Creates an instance of {@link HcpVaultCredentialsFileAuthenticator} to handle authentication
   * via the HCP CLI credentials cache file.
   *
   * @param credentialsFilePath The path to the credentials file.
   */
  public HcpVaultCredentialsFileAuthenticator(String credentialsFilePath) {
    this.credsFilePath = Paths.get(credentialsFilePath);
  }

  /**
   * Retrieves a valid access token, refreshing it if expired.
   *
   * @return A valid access token.
   * @throws IOException if authentication fails.
   */
  public String getValidAccessToken() throws Exception {
    lock.lock();
    try {
      if (accessToken == null || isTokenExpired()) {
        loadCredentials();
        if (isTokenExpired()) {
          refreshAccessToken();
        }
      }
      return accessToken;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Loads credentials from the CLI cache file.
   *
   * @throws IOException if there is an error reading the file
   */
  private void loadCredentials() throws IOException {
    if (!Files.exists(credsFilePath)) {
      throw new IOException("HCP Vault credentials file not found: " + credsFilePath);
    }

    String content = new String(Files.readAllBytes(credsFilePath), StandardCharsets.UTF_8);

    OracleJsonObject rootObject = JsonUtil.convertJsonToOracleJsonObject(content);
    if (rootObject == null) {
      throw new IOException("Failed to parse credentials file: invalid JSON format");
    }

    OracleJsonObject loginObject;
    try {
      loginObject = rootObject.getObject(LOGIN_FIELD);
    } catch (NullPointerException e) {
      throw new IOException("Invalid credentials file format: missing 'login'" +
              " object", e);
    }
    accessToken = JsonUtil.extractField(loginObject, ACCESS_TOKEN_FIELD);
    refreshToken = JsonUtil.extractField(loginObject, REFRESH_TOKEN_FIELD);

    String expiryStr = JsonUtil.extractField(loginObject, ACCESS_TOKEN_EXPIRY_FIELD);
    if (expiryStr != null && !expiryStr.isEmpty()) {
      tokenExpiry = OffsetDateTime.parse(expiryStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
    }
  }

  /**
   * Checks if the current token is expired.
   *
   * @return true if the token is expired
   */
  private boolean isTokenExpired() {
    return tokenExpiry == null || Instant.now().isAfter(tokenExpiry);
  }

  /**
   * Refreshes the access token using the refresh token.
   *
   * @throws IOException if the refresh operation fails
   */
  private void refreshAccessToken() throws Exception {
    String clientId = extractClientIdFromToken(accessToken);
    if (clientId == null || refreshToken == null) {
      throw new IllegalStateException("Missing required parameters for token refresh.");
    }

    String payload = String.format(TOKEN_REFRESH_PAYLOAD_FORMAT, GRANT_TYPE, refreshToken, clientId);
    String jsonResponse = HttpUtil.sendPostRequest(TOKEN_URL, payload, CONTENT_TYPE, null
            , null);

    OracleJsonObject response = JsonUtil.convertJsonToOracleJsonObject(jsonResponse);
    updateTokensFromResponse(response);
    updateCredsFile();
  }

  /**
   * Updates tokens and expiry from the refresh response.
   *
   * @param response The JSON response from the refresh request
   */
  private void updateTokensFromResponse(OracleJsonObject response) {
    accessToken = JsonUtil.extractField(response, ACCESS_TOKEN_FIELD);

    try {
      long expiresInSeconds = response.getLong(EXPIRES_IN_FIELD);
      tokenExpiry = Instant.now().plusSeconds(expiresInSeconds);
    } catch (NullPointerException e) {
      throw new IllegalStateException("Missing '" + EXPIRES_IN_FIELD + "' field in token response", e);
    }

    // Update refresh token if provided
    String newRefreshToken = JsonUtil.extractField(response, REFRESH_TOKEN_FIELD);
    if (newRefreshToken != null && !newRefreshToken.isEmpty()) {
      refreshToken = newRefreshToken;
    }
  }

  /**
   * Updates the credentials cache file with new token information.
   *
   * @throws IOException if file writing fails
   */
  private void updateCredsFile() throws IOException {
    String updatedContent = String.format(CREDENTIALS_JSON_FORMAT, accessToken,
            refreshToken, OffsetDateTime.ofInstant(tokenExpiry, ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    );

    Files.write(credsFilePath, updatedContent.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Extracts the client ID from a JWT token.
   *
   * @param token The JWT token
   * @return The extracted client ID
   * @throws IllegalArgumentException if the token is invalid or client_id extraction fails.
   */
  private static String extractClientIdFromToken(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        throw new IllegalArgumentException("Invalid JWT token format.");
      }
      String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      OracleJsonObject payload = JsonUtil.convertJsonToOracleJsonObject(payloadJson);
      return JsonUtil.extractField(payload, CLIENT_ID_FIELD);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to extract client_id from JWT token.", e);
    }
  }
}
