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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.secrets;

import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonParser;
import oracle.sql.json.OracleJsonValue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

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

  private HcpVaultApiClient() {}

  /**
   * Fetches the secrets JSON from the HCP Vault Secrets API.
   *
   * @param urlStr The URL of the HCP Vault API endpoint. Not null.
   * @param token  The Bearer token for authentication. Not null.
   * @return The JSON response as a string. Not null.
   * @throws IllegalStateException If the HTTP request fails or the response
   * does not contain the required fields.
   */
  static String fetchSecrets(String urlStr, String token) {
    HttpURLConnection conn = null;
    try {
      URL url = new URL(urlStr);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", "Bearer " + token);
      conn.setRequestProperty("Accept", "application/json");

      int statusCode = conn.getResponseCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new IllegalStateException(
                "Failed to retrieve HCP secrets. HTTP status: " + statusCode
        );
      }

      try (InputStream in = conn.getInputStream();
           Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
        String jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";

        OracleJsonFactory factory = new OracleJsonFactory();
        ByteArrayInputStream jsonInputStream = new ByteArrayInputStream(
                jsonResponse.getBytes(StandardCharsets.UTF_8));
        OracleJsonParser parser = factory.createJsonTextParser(jsonInputStream);

        OracleJsonValue value = null;

        while (parser.hasNext()) {
          OracleJsonParser.Event event = parser.next();
          if (event == OracleJsonParser.Event.KEY_NAME && "value".equals(parser.getString())) {
            parser.next();
            value = parser.getValue();
            break;
          }
        }

        if (value == null) {
          throw new IllegalStateException("Missing 'value' field in the response JSON.");
        }

        if (value.getOracleJsonType() == OracleJsonValue.OracleJsonType.STRING) {
          return value.asJsonString().getString();
        }

        throw new IllegalStateException("The 'value' field is not a string.");
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to call HCP secrets endpoint: " + urlStr, e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

}
