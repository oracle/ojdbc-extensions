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
import oracle.jdbc.provider.parameter.ParameterSet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication.HcpVaultTokenFactory.HCP_CREDENTIALS_FILE;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

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

  private volatile String accessToken;
  private volatile String refreshToken;
  private volatile Instant tokenExpiry;

  private final Path credsFilePath;

  /**
   * Creates an instance of {@link HcpVaultCredentialsFileAuthenticator} to handle authentication
   * via the HCP CLI credentials cache file.
   *
   * @param parameterSet The set of parameters, including the path to the credentials file.
   */
  public HcpVaultCredentialsFileAuthenticator(ParameterSet parameterSet) {
    String credsPath = getRequiredOrFallback(parameterSet, HCP_CREDENTIALS_FILE, "HCP_CREDENTIALS_FILE");
    this.credsFilePath = Paths.get(credsPath);
  }

  /**
   * Retrieves a valid access token, refreshing it if expired.
   *
   * @return A valid access token.
   * @throws IOException if authentication fails.
   */
  public synchronized String getValidAccessToken() throws Exception {
    if (accessToken == null || isTokenExpired()) {
      loadCredentials();
      if (isTokenExpired()) {
        refreshAccessToken();
      }
    }
    return accessToken;
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
    OracleJsonObject jsonObject = JsonUtil.parseJsonResponse(content).getObject("login");

    accessToken = JsonUtil.extractField(jsonObject, "access_token");
    refreshToken = JsonUtil.extractField(jsonObject, "refresh_token");

    String expiryStr = JsonUtil.extractField(jsonObject, "access_token_expiry");
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

    String payload = String.format("grant_type=%s&refresh_token=%s&client_id=%s", GRANT_TYPE, refreshToken, clientId);
    HttpURLConnection conn = HttpUtil.createConnection(TOKEN_URL, "POST", "application/x-www-form-urlencoded", null, null);
    String jsonResponse = HttpUtil.sendPayloadAndGetResponse(conn, payload);

    OracleJsonObject response = JsonUtil.parseJsonResponse(jsonResponse);
    updateTokensFromResponse(response);
    updateCredsFile();
  }

  /**
   * Updates tokens and expiry from the refresh response.
   *
   * @param response The JSON response from the refresh request
   */
  private void updateTokensFromResponse(OracleJsonObject response) {
    accessToken = JsonUtil.extractField(response, "access_token");

    OracleJsonValue expiresInValue = response.get("expires_in");
    if (expiresInValue instanceof OracleJsonNumber) {
      tokenExpiry = Instant.now().plusSeconds(expiresInValue.asJsonNumber().longValue());
    } else {
      throw new IllegalStateException("Missing or invalid 'expires_in' field in token response.");
    }

    // Update refresh token if provided
    String newRefreshToken = JsonUtil.extractField(response, "refresh_token");
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
    String updatedContent = String.format(
            "{ \"login\": { \"access_token\": \"%s\", \"refresh_token\": \"%s\", \"access_token_expiry\": \"%s\" } }",
            accessToken, refreshToken,
            OffsetDateTime.ofInstant(tokenExpiry, ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    );

    Files.write(credsFilePath, updatedContent.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Extracts the client ID from a JWT token.
   *
   * @param token The JWT token
   * @return The extracted client ID
   */
  public static String extractClientIdFromToken(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        throw new IllegalArgumentException("Invalid JWT token format.");
      }
      String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      OracleJsonObject payload = JsonUtil.parseJsonResponse(payloadJson);
      return JsonUtil.extractField(payload, "client_id");
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to extract client_id from JWT token.", e);
    }
  }
}
