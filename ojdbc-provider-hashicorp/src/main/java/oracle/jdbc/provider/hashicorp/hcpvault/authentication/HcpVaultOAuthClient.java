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

package oracle.jdbc.provider.hashicorp.hcpvault.authentication;

import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * A client for performing OAuth2 operations with HCP Vault Secrets.
 * <p>
 * This class implements the client_credentials flow to obtain an API token
 * required for interacting with HCP Vault Secrets.
 * </p>
 */
public final class HcpVaultOAuthClient {

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
    HttpURLConnection conn = null;
    try {
      URL url = new URL("https://auth.idp.hashicorp.com/oauth/token");
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setDoOutput(true);

      String body = "grant_type=client_credentials"
              + "&client_id=" + clientId
              + "&client_secret=" + clientSecret
              + "&audience=https://api.hashicorp.cloud";

      try (OutputStream os = conn.getOutputStream()) {
        os.write(body.getBytes(StandardCharsets.UTF_8));
      }

      if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
        try (InputStream in = conn.getInputStream();
             Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
          scanner.useDelimiter("\\A");
          String jsonResponse = scanner.hasNext() ? scanner.next() : "";

          OracleJsonObject response = new OracleJsonFactory()
                  .createJsonTextValue(new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)))
                  .asJsonObject();

          return response.getString("access_token");
        }
      } else {
        throw new IllegalStateException("Failed to obtain HCP token. HTTP=" + conn.getResponseCode());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch HCP access token", e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }
}
