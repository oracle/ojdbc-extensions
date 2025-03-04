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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.secrets;

import oracle.jdbc.provider.hashicorp.HttpUtil;
import oracle.sql.json.OracleJsonException;
import oracle.sql.json.OracleJsonObject;

import static oracle.jdbc.provider.hashicorp.JsonUtil.convertJsonToOracleJsonObject;

/**
 * <p>
 * Utility class for interacting with the HCP Vault Secrets API. Provides
 * methods to fetch secrets and extract specific fields from JSON responses.
 * </p>
 * <p>
 * This class is responsible for making HTTP requests to the HCP Vault API
 * and parsing the JSON responses using the Oracle JSON library.
 * </p>
 */
public final class HcpVaultApiClient {

  private static final String SECRET_FIELD = "secret";
  private static final String STATIC_VERSION_FIELD = "static_version";
  private static final String VALUE_FIELD = "value";

  private HcpVaultApiClient() {
  }

  /**
   * Fetches the secret value from the HCP Vault Secrets API.
   * <p>
   * The API response contains metadata along with the secret. The expected format is:
   * <pre>
   * {
   *   "secret": {
   *     "static_version": {
   *       "value": "OUR_SECRET"
   *     }
   *   }
   * }
   * </pre>
   * This method extracts and returns the `value` field.
   *
   * @param urlStr The HCP Vault API endpoint.
   * @param token  The Bearer token for authentication.
   * @return The extracted secret value. Never null.
   * @throws IllegalStateException If the request fails or JSON is invalid.
   */
  public static String fetchSecret(String urlStr, String token) {
    try {
      String jsonResponse = HttpUtil.sendGetRequest(urlStr, token, null);
      OracleJsonObject jsonObject = convertJsonToOracleJsonObject(jsonResponse);

      return jsonObject.getObject(SECRET_FIELD)
              .getObject(STATIC_VERSION_FIELD)
              .getString(VALUE_FIELD);

    } catch (OracleJsonException e) {
      throw new IllegalStateException("Invalid JSON structure or missing fields in response", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to fetch HCP secrets from URL: " + urlStr, e);
    }
  }
}
