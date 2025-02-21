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

package oracle.jdbc.provider.hashicorp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Utility class for handling HTTP requests and responses.
 */
public class HttpUtil {

  /**
   * Creates an HTTP connection to the specified URL with the given settings.
   *
   * @param urlStr The URL to connect to. Must not be null.
   * @param method The HTTP method (e.g., "GET", "POST"). Must not be null.
   * @param contentType The content type for the request (e.g., "application/json"). Must not be null.
   * @param authToken The optional Bearer token for authorization. Can be null or empty.
   * @param namespace The optional Vault namespace. Can be null or empty.
   * @return A configured {@link HttpURLConnection}. Never null.
   * @throws Exception if the connection cannot be established.
   */
  public static HttpURLConnection createConnection(String urlStr, String method, String contentType, String authToken, String namespace)
          throws Exception {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod(method);
    conn.setRequestProperty("Content-Type", contentType);
    conn.setRequestProperty("Accept", "application/json");
    if (authToken != null && !authToken.isEmpty()) {
      conn.setRequestProperty("Authorization", "Bearer " + authToken);
    }
    if (namespace != null && !namespace.isEmpty()) {
      conn.setRequestProperty("X-Vault-Namespace", namespace);
    }
    conn.setDoOutput(true);
    return conn;
  }

  /**
   * Reads the full response from an InputStream as a String.
   *
   * @param in the input stream to read.
   * @return the response string.
   */
  private static String readResponse(InputStream in) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read HTTP response", e);
    }
  }

  /**
   * Sends a payload via the provided {@link HttpURLConnection} and retrieves
   * the response as a string.
   *
   * @param conn The configured HTTP connection. Must not be null.
   * @param payload The payload to send. Must not be null.
   * @return The response as a string. Never null.
   * @throws Exception if the request fails or the response cannot be read.
   */
  public static String sendPayloadAndGetResponse(HttpURLConnection conn, String payload) throws Exception {
    try (OutputStream os = conn.getOutputStream()) {
      os.write(payload.getBytes(StandardCharsets.UTF_8));
    }
    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      String errorResponse = "";
      try {
        errorResponse = readResponse(conn.getErrorStream());
      } catch (Exception ignore) {
      }
      String errorMessage = String.format("HTTP request failed with status code %d. " +
                      "Please verify any provided parameters. Error Response:" +
              " %s ", responseCode, errorResponse);
      throw new IllegalStateException(errorMessage);
    }
    return readResponse(conn.getInputStream());
  }

  /**
   * Sends a GET request via the provided {@link HttpURLConnection} and
   * retrieves the response as a string.
   *
   * @param conn The configured HTTP connection. Must not be null.
   * @return The response as a string. Never null.
   * @throws Exception if the request fails or the response cannot be read.
   */
  public static String sendGetRequestAndGetResponse(HttpURLConnection conn) throws Exception {
    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      String errorResponse = "";
      try {
        errorResponse = readResponse(conn.getErrorStream());
      } catch (Exception ignore) {
      }
      String errorMessage = String.format("HTTP request failed with status code %d. " +
              "Please verify any provided parameters. Error Response:" +
              " %s ", responseCode, errorResponse);
      throw new IllegalStateException(errorMessage);
    }
    return readResponse(conn.getInputStream());
  }
}
