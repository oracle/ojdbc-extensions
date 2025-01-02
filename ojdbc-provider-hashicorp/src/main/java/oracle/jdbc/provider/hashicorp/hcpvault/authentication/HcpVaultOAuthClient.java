package oracle.jdbc.provider.hashicorp.hcpvault.authentication;

import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HcpVaultOAuthClient {

  private HcpVaultOAuthClient() {}

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

      if (conn.getResponseCode() == 200) {
        try (InputStream in = conn.getInputStream()) {
          OracleJsonObject response = new OracleJsonFactory()
                  .createJsonTextValue(new ByteArrayInputStream(readAll(in).getBytes(StandardCharsets.UTF_8)))
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

  private static String readAll(InputStream in) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = in.read(buffer)) != -1) {
      baos.write(buffer, 0, len);
    }
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }
}
