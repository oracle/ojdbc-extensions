import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * This client sends a request to the api/employees endpoint of the Spring
 * application. The request is authenticated by an access token can be used
 * as a jwt-bearer or token-exchange grant by the Spring application. This
 * client uses an authorization code to obtain the token it sends to the
 * Spring application. Obtaining the authorization code involves an interactive
 * login with a web browser.
 */
public class FrontEndClient {

  public static void main(String[] args) throws IOException, InterruptedException {

    final String clientId;
    final String scope;
    final URI redirectUri;
    final URI authorizationUri;
    final URI tokenUri;
    try {
      clientId = requireConfig("HRAPP_FRONTEND_CLIENT_ID");
      scope = requireConfig("HRAPP_DELEGATED_SCOPE");

      redirectUri = URI.create(requireConfig("REDIRECT_URI"));

      authorizationUri = URI.create(
        requireConfig("HRAPP_AUTH_URI")
        + '?'
        + urlEncode(Map.of(
          "response_type", "code",
          "client_id", clientId,
          "redirect_uri", redirectUri.toString(),
          "scope", scope)));

      tokenUri = URI.create(requireConfig("HRAPP_TOKEN_URI"));
    }
    catch (IllegalStateException missingConfiguration) {
      System.out.println(missingConfiguration.getMessage());
      return;
    }

    final String code;
    try {
      code = requestAuthorizationCode(redirectUri, authorizationUri);
    }
    catch (Exception exception) {
      System.out.println(exception.getMessage());
      return;
    }

    final String token;
    try {
      token = requestToken(tokenUri, redirectUri, clientId, code);
    }
    catch (Exception exception) {
      System.out.println(exception.getMessage());
      return;
    }

    HttpClient httpClient = HttpClient.newHttpClient();

    HttpRequest employeesRequest =
      HttpRequest.newBuilder(
          URI.create("http://localhost:8080/api/employees"))
        .header("Authorization", "Bearer " + token)
        .build();

    HttpResponse<String> employeesResponse =
      httpClient.send(
        employeesRequest,
        BodyHandlers.ofString());
    System.out.println(employeesResponse.body());
  }

  /**
   * Returns the configured value for a given name, or throws
   * IllegalStateException if no value is configured.
   */
  private static String requireConfig(String name) {
    String value = System.getenv(name);

    if (value == null) {
      throw new IllegalStateException(
        "No value is configured for \"" + name + "\"."
        + " Set this environment variable, and then re-run the client.");
    }

    return value;
  }

  private static String urlEncode(Map<String, String> parameters) {
    return parameters.entrySet()
      .stream()
      .map(entry ->
        entry.getKey() + '=' + URLEncoder.encode(entry.getValue(), UTF_8))
      .collect(Collectors.joining("&"));
  }

  private static Map<String, String> urlDecode(String parameters) {
    return Arrays.stream(parameters.split("&"))
      .map(parameter -> parameter.split("="))
      .filter(parameter -> parameter.length == 2)
      .collect(Collectors.toMap(
        parameter -> URLDecoder.decode(parameter[0], UTF_8),
        parameter -> URLDecoder.decode(parameter[1], UTF_8)));
  }

  /**
   * Interactively requests an authorization code by having a user login with a
   * web browser.
   */
  private static String requestAuthorizationCode(
    URI redirectUri,
    URI authorizationUri)
    throws Exception {

    HttpServer server = HttpServer.create(
      new InetSocketAddress(
        redirectUri.getHost(),
        redirectUri.getPort()),
      1);

    try {
      server.setExecutor(runnable ->
        new Thread(runnable).start());
      server.start();

      Future<Map<String,String>> redirectFuture = awaitRedirect(server);

      Desktop.getDesktop().browse(authorizationUri);

      Map<String, String> parameters = redirectFuture.get(1, MINUTES);
      String code = parameters.get("code");

      if (code == null) {
        throw new Exception("No code: " + parameters);
      }

      return code;
    }
    finally {
      server.stop(3);
    }
  }

  /**
   * Requests an access token using an authorization code grant.
   */
  private static String requestToken(
    URI tokenUri,
    URI redirectUri,
    String clientId,
    String code)
    throws Exception {
    HttpClient httpClient = HttpClient.newHttpClient();

    HttpRequest tokenRequest =
      HttpRequest.newBuilder(tokenUri)
        .POST(BodyPublishers.ofString(
          urlEncode(Map.of(
            "grant_type", "authorization_code",
            "code", code,
            "redirect_uri", redirectUri.toString(),
            "client_id", clientId))))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .build();

    HttpResponse<String> tokenResponse =
      httpClient.send(tokenRequest, BodyHandlers.ofString());

    String body = tokenResponse.body();

    if (tokenResponse.statusCode() != 200) {
      throw new Exception(body);
    }

    // Attempt to match this JSON element: "access_token" : "{token}"
    Matcher tokenMatcher =
        Pattern.compile(
          "\"access_token\"\\s*:\\s*\"([^\"]+)\"")
          .matcher(body);

    if (!tokenMatcher.find()) {
      throw new Exception("No token: " + body);
    }

    return tokenMatcher.group(1);
  }

  /**
   * Starts a server that will wait for a request to arrive at the redirect URI.
   * When the request arrives, the server will complete a future with query
   * parameters from request the URL. This is meant to handle a redirect from
   * an authorization server when requesting an authorization code, as specified
   * here: https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2
   */
  private static CompletableFuture<Map<String, String>> awaitRedirect(
    HttpServer server)
    throws IOException {

    CompletableFuture<Map<String, String>> codeFuture =
      new CompletableFuture<>();

    server.createContext(
      "/",
      httpExchange -> {
        try (httpExchange) {

          byte[] response = (
            """
               <body style="margin:0;display:grid;place-items:center;height:100vh;font:48px sans-serif">
               Login complete. Close this tab and return to the client.
               </body>
            """).getBytes(UTF_8);

          httpExchange.sendResponseHeaders(200, response.length);
          try (OutputStream responseStream = httpExchange.getResponseBody()) {
            responseStream.write(response);
          }

          codeFuture.complete(
            urlDecode(httpExchange.getRequestURI().getRawQuery()));
        }
      });

    return codeFuture.whenComplete((result, throwable) -> server.stop(3));
  }

}