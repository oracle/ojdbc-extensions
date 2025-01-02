package oracle.jdbc.provider.hashicorp.hcpvault.secrets;

import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonParser;
import oracle.sql.json.OracleJsonValue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HcpVaultApiClient {

  private HcpVaultApiClient() {}

  static String fetchSecrets(String urlStr, String token) {
    HttpURLConnection conn = null;
    try {
      URL url = new URL(urlStr);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", "Bearer " + token);
      conn.setRequestProperty("Accept", "application/json");

      int statusCode = conn.getResponseCode();
      if (statusCode != 200) {
        String errBody = "";
        try (InputStream es = conn.getErrorStream()) {
          if (es != null) {
            errBody = readAll(es);
          }
        }
        throw new IllegalStateException(
                "Failed to retrieve HCP secrets. HTTP=" + statusCode + " Body=" + errBody);
      }

      try (InputStream in = conn.getInputStream()) {
        String jsonResponse = readAll(in);

        // Parse the JSON response using Oracle JSON
        OracleJsonFactory factory = new OracleJsonFactory();
        ByteArrayInputStream jsonInputStream = new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8));
        OracleJsonParser parser = factory.createJsonTextParser(jsonInputStream);

        OracleJsonValue value = null;

        // Navigate the JSON to extract the "value" field
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

        // Check if the value is a string and return it
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

  /**
   * Utility method
   */
  private static String readAll(InputStream in) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = in.read(buffer)) != -1) {
      baos.write(buffer, 0, len);
    }
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  public static String extractKeyFromJson(String json, String key) {
    try (InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
      OracleJsonObject rootObject = new OracleJsonFactory()
              .createJsonTextValue(jsonInputStream)
              .asJsonObject();

      // Check if the key exists in the root JSON object
      if (!rootObject.containsKey(key)) {
        throw new IllegalArgumentException("Key '" + key + "' not found in the JSON response.");
      }

      OracleJsonValue value = rootObject.get(key);

      // Handle different types of values
      switch (value.getOracleJsonType()) {
        case STRING:
          return value.asJsonString().getString();
        case OBJECT:
          // Convert nested JSON object to string
          return value.asJsonObject().toString();
        default:
          throw new IllegalArgumentException("Unsupported JSON type for key '" + key + "'.");
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse JSON while extracting key: " + key, e);
    }
  }

}
