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

/**
 * Enumeration of authentication methods supported by HCP Vault Secrets.
 * <p>
 * This represents the different ways to authenticate with the HCP Vault Secrets API.
 * </p>
 */
public enum HcpVaultAuthenticationMethod {

  /**
   * Authentication using client credentials via the OAuth2 client_credentials flow.
   * <p>
   * This method requires the following:
   * </p>
   * <ul>
   *   <li>A <b>Client ID</b> provided by the HCP Vault console or associated
   *      with an HCP Service Principal.
   *   </li>
   *   <li>A <b>Client Secret</b> corresponding to the Client ID, ensuring
   *   secure access.
   *   </li>
   * </ul>
   * <p>
   * By using these credentials, the method retrieves a short-lived API token
   * by calling the HCP OAuth2 endpoint.
   * </p>
   */
  CLIENT_CREDENTIALS,

  /**
   * Authentication using the credentials file generated by the HCP CLI.
   * <p>
   * This method retrieves an access token from the standard CLI-generated
   * credentials file located at
   * <code>System.getProperty("user.home") + "/.config/hcp/creds-cache.json"</code>.
   * If the token is expired,
   * it will be automatically refreshed using the stored refresh token.
   * </p>
   * <p>
   * The credentials file must follow the standard JSON structure containing:
   * </p>
   * <pre>
   * {
   *   "login": {
   *     "access_token": "...",
   *     "refresh_token": "...",
   *     "access_token_expiry": "..."
   *   }
   * }
   * </pre>
   * <p>
   * The user can provide a custom path to the credentials file if needed.
   * </p>
   */
  CLI_CREDENTIALS_FILE,

  /**
   * Automatically selects the best authentication method based on available parameters.
   *
   * <p>Priority order:</p>
   * <ol>
   *   <li>Uses the credentials file if present and valid.</li>
   *   <li>Falls back to client credentials authentication.</li>
   * </ol>
   */
  AUTO_DETECT;
}