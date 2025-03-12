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

import oracle.jdbc.provider.hashicorp.util.HttpUtil;
import oracle.jdbc.provider.hashicorp.util.JsonUtil;
import oracle.sql.json.OracleJsonObject;

import static oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication.HcpVaultCredentialsFileAuthenticator.ACCESS_TOKEN_FIELD;

/**
 * A client for performing OAuth2 operations with HCP Vault Secrets.
 * <p>
 * This class implements the client_credentials flow to obtain an API token
 * required for interacting with HCP Vault Secrets.
 * </p>
 */
public final class HcpVaultOAuthClient {

  private static final String OAUTH_TOKEN_URL = "https://auth.idp.hashicorp.com/oauth/token";
  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String GRANT_TYPE = "client_credentials";
  private static final String AUDIENCE = "https://api.hashicorp.cloud";
  private static final String CLIENT_CREDENTIALS_PAYLOAD_FORMAT =
          "grant_type=%s&client_id=%s&client_secret=%s&audience=%s";

  private HcpVaultOAuthClient() {}

  /**
   * Fetches an access token from HCP Vault Secrets using the client_credentials flow.
   *
   * @param clientId the OAuth2 client ID. Must not be null or empty.
   * @param clientSecret the OAuth2 client secret. Must not be null or empty.
   * @return the access token as a {@code String}. Never null or empty.
   * @throws IllegalStateException if the token cannot be obtained.
   */
  public static String fetchHcpAccessToken(String clientId, String clientSecret) {
    try {
      String payload = String.format(
              CLIENT_CREDENTIALS_PAYLOAD_FORMAT, GRANT_TYPE, clientId, clientSecret, AUDIENCE);

      String jsonResponse = HttpUtil.sendPostRequest(
              OAUTH_TOKEN_URL,
              payload,
              CONTENT_TYPE,
              null,
              null
      );
      OracleJsonObject response = JsonUtil.convertJsonToOracleJsonObject(jsonResponse);
      return JsonUtil.extractField(response, ACCESS_TOKEN_FIELD);

    } catch (Exception e) {
      throw new IllegalStateException("Failed to fetch HCP access token", e);
    }
  }

}
