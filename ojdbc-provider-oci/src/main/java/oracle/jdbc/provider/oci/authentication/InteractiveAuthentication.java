/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.oci.authentication;

import com.oracle.bmc.Region;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.util.JsonWebTokenParser;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * <p>
 * Static methods for interactive authentication with OCI.
 * </p><p>
 * A web browser sends a login request to OCI, and the response is a session
 * token. The session token response is redirected to an HTTP server running
 * on the local host. This class starts the HTTP server and writes the token
 * to a file. The OCI SDK is configured to read from the file to create an
 * authentication details object.
 * </p><p>
 * The implementation of this class is a rewrite of the OCI CLI implementation
 * for the {@code oci session authenticate} command. The OCI development team
 * was unable to provide any specification or technical documentation that
 * describes the behavior of their CLI tool.
 * </p><p>
 * The behavior involves HTTP exchanges between a web browser and both the OCI
 * login service, and an HTTP server running on the localhost.
 * </p><pre>
 * [BROWSER] -> (GET login.oci.oracle.com/v1/oauth2/authorize) -> [LOCAL SERVER]
 * [BROWSER] <- (HTML + main.js) <- [OCI SERVER]
 *
 * [BROWSER] -> (Login Click -> ???) -> [OCI SERVER]
 * [BROWSER] <- (???) <- [OCI SERVER]
 * (Response triggers a redirect function call in JS. This function does GET localhost:8181/)
 *
 * [BROWSER] -> (GET localhost:8181/) -> [LOCAL SERVER]
 * [BROWSER] <- (JAVASCRIPT)         <- [LOCAL SERVER]
 *
 * [BROWSER] -> (GET localhost:8181/token?...security_token=...) -> [LOCAL SERVER]
 * [BROWSER] <- (400 OK)         <- [LOCAL SERVER]
 * </pre>
 */
final class InteractiveAuthentication {

  private InteractiveAuthentication() {}

  /**
   * <p>
   * Returns authentication details for a session token. The token is obtained
   * by interactive authentication with a web browser.
   * </p><p>
   * Unlike the OCI CLI tool, this method does not interact with the default
   * path configuration file located at {@code $HOME/.oci/config}. This is done
   * to avoid overwriting information when that file already exists and already
   * contains the specified {@code profile}.
   * </p>
   */
  static InteractiveAuthenticationDetails getSessionToken(
    ParameterSet parameterSet) {

    Region region = parameterSet.getOptional(AuthenticationDetailsFactory.REGION);

    // The OCI CLI listens on port 8181, so this method will do the same. It is
    // believed that the redirect URI of "http://localhost:8181" is registered
    // with the login endpoint of Oracle Cloud, and so it would reject an
    // attempt to use any other redirect URI.
    InetSocketAddress redirectAddress =
      new InetSocketAddress("localhost", 8181);

    // Asynchronously receive the session token from the browser
    CompletableFuture<LoginResult> loginFuture =
      acceptRedirect(redirectAddress);
    try {
      // Open the browser to generate a session token. The token uses a key pair
      // for proof of possession.
      KeyPair keyPair = generateKeyPair();
      openBrowser(region, keyPair.getPublic(), redirectAddress);

      // Wait for the browser login to complete
      LoginResult loginResult = awaitLogin(loginFuture);

      // If a region has not been configured, then extract it from the
      // "issuer_region" claim of the ID token.
      if (region == null)
        region = loginResult.getIssuerRegion();

      return new InteractiveAuthenticationDetails(
        region, loginResult.securityToken, keyPair);
    }
    finally {
      loginFuture.cancel(true);
    }
  }

  /**
   * Generates a public/private key pair for proof of possession with a session
   * token.
   * @return Generated key pair. Not null.
   */
  private static KeyPair generateKeyPair() {
    try {
      return KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }
    catch (NoSuchAlgorithmException rsaNotAvailable) {
      throw new IllegalStateException(rsaNotAvailable);
    }
  }

  /**
   * <p>
   * Asynchronously accepts HTTP requests from the web browser to the given
   * {@code redirectAddress}. The HTTP server expects an initial GET request
   * for the root path "/", and responds with a script that has the browser
   * send a second request. The second request is expected to be a GET request
   * for the "/token" path with a "security_token" parameter.
   * </p><p>
   * The implementation of this method is derived from the
   * <a href="https://github.com/oracle/oci-cli/blob/ed9f755092a1ba9702cb1a133c152944da819df8/src/oci_cli/cli_setup_bootstrap.py#L271">
   * OCI CLI's Python implementation
   * </a>.
   * </p><p>
   * <em>
   *   Resources allocated by this method will only be released after the
   *   returned future completes or is cancelled.
   * </em>
   * </p>
   * @param redirectAddress Server redirectAddress. Not null.
   * @return A future that completes normally with the session token, or
   * exceptionally with an {@code IOException} if network I/O fails, or with
   * an {@code IllegalStateException} if the server receives an unexpected
   * request.
   */
  private static CompletableFuture<LoginResult> acceptRedirect(
    InetSocketAddress redirectAddress) {

    final HttpServer server;
    try {
      server = HttpServer.create(redirectAddress, 0);
    }
    catch (IOException ioException) {
      throw new IllegalStateException(
        "Failed to create an HTTP server", ioException);
    }

    CompletableFuture<LoginResult> loginFuture = new CompletableFuture<>();

    server.createContext("/", httpExchange -> {
      try {
        handleScriptRequest(httpExchange);
      }
      catch (Exception exception) {
        loginFuture.completeExceptionally(exception);
      }
    });

    server.createContext("/token", httpExchange -> {
      try {
        loginFuture.complete(handleTokenRequest(httpExchange));
      }
      catch (Exception exception) {
        loginFuture.completeExceptionally(exception);
      }
    });

    server.setExecutor(null); // Use default executor
    server.start();

    // Stop the server when the token is received, a request fails, or the
    // future is cancelled. The CompletionStage returned by whenComplete should
    // not be returned by this method. If that stage is cancelled, then the
    // whenComplete callback is not executed. However, the callback will be
    // executed if the upstream stage is cancelled, so return that one.
    loginFuture.whenComplete((result, error) -> server.stop(0));

    return loginFuture;
  }

  /**
   * <p>
   * Handles a GET request for the / (root) path by responding with a
   * {@linkplain #SCRIPT_RESPONSE HTML script element}.
   * </p><p>
   * The implementation of this method is derived from the
   * <a href="https://github.com/oracle/oci-cli/blob/ed9f755092a1ba9702cb1a133c152944da819df8/src/oci_cli/cli_setup_bootstrap.py#L277">
   * OCI CLI's Python implementation
   * </a>.
   * </p>
   * @param httpExchange The HTTP request. Not null.
   * @throws IllegalStateException If the request does not include a
   * "security_token" query parameter.
   */
  @SuppressWarnings("try")
  private static void handleScriptRequest(HttpExchange httpExchange) {
     try (AutoCloseable autoClose = httpExchange::close) {
       httpExchange.sendResponseHeaders(200, SCRIPT_RESPONSE.length);
       httpExchange.getResponseBody().write(SCRIPT_RESPONSE);
     }
     catch (Exception exception) {
       throw new IllegalStateException(
         "Failed to handle HTTP request", exception);
     }
  }

  /**
   * <p>
   * Handles a GET request for the /token path by extracting a "security_token"
   * parameter from the URI.
   * </p><p>
   * The implementation of this method is derived from the
   * <a href="https://github.com/oracle/oci-cli/blob/ed9f755092a1ba9702cb1a133c152944da819df8/src/oci_cli/cli_setup_bootstrap.py#L305">
   * OCI CLI's Python implementation
   * </a>.
   * </p>
   * @param httpExchange The HTTP request. Not null.
   * @return The security token sent in the request. Not null.
   * @throws IllegalStateException If the request does not include a
   * "security_token" query parameter.
   */
  @SuppressWarnings("try")
  private static LoginResult handleTokenRequest(HttpExchange httpExchange) {
    try (AutoCloseable autoClose = httpExchange::close) {

      String query = httpExchange.getRequestURI().getQuery();
      LoginResult loginResult = LoginResult.fromUriQuery(query);
      httpExchange.sendResponseHeaders(200, -1);
      return loginResult;
    }
    catch (Exception exception) {
      throw new IllegalStateException(
        "Failed to handle HTTP request", exception);
    }
  }

  /**
   * <p>
   * Opens a web browser that connects to an OCI login page, and redirects
   * the session token to the given {@code redirectAddress} upon successful
   * authentication.
   * </p><p>
   * The implementation of this method is derived from the
   * <a href="https://github.com/oracle/oci-cli/blob/ed9f755092a1ba9702cb1a133c152944da819df8/src/oci_cli/cli_setup_bootstrap.py#L143">
   * OCI CLI's Python implementation
   * </a>.
   * </p><p>
   * The CLI implementation has a way to get a tenant name, and it includes this
   * as a 'tenant' parameter in the URI. This provider does not have a way to do
   * that; It can be configured with the OCID of tenant, but it is not clear how
   * to get the name from the OCID.
   * </p>
   * @param region OCI region to connect to. Not null.
   * @param publicKey Public key used for proof of possession with the session
   * token. Not null.
   * @param redirectAddress Address to redirect the session token to. Not null.
   * @throws IllegalStateException If a browser can not be opened.
   */
  private static void openBrowser(
      Region region, PublicKey publicKey, InetSocketAddress redirectAddress) {
    try {
      Desktop.getDesktop().browse(URI.create(
        format("https://login.%s.%s/v1/oauth2/authorize",
          region == null ? "oci" : region.getRegionId(),
          region == null ? "oraclecloud.com" : region.getRealm().getSecondLevelDomain()) +
        "?action=login" +
        "&client_id=iaas_console" +
        "&response_type=" +
          encodeUrlParameter("token id_token") +
        "&nonce=" +
          encodeUrlParameter(UUID.randomUUID().toString()) +
        "&scope=openid" +
        "&public_key=" +
          encodeUrlParameter(Base64.getUrlEncoder().encodeToString(
            encodeJwk(publicKey).getBytes(UTF_8))) +
        "&redirect_uri=" +
          encodeUrlParameter(format("http://%s:%d",
            redirectAddress.getHostName(), redirectAddress.getPort()))));
    }
    catch (IOException ioException) {
      throw new IllegalStateException(
        "Failed to open a web browser", ioException);
    }
  }

  /**
   * <p>
   * Encodes a public key as a JSON Web Key (JWK), as specified by
   * <a href="https://www.rfc-editor.org/rfc/rfc7517">
   * RFC 7517
   * </a> and
   * <a href="https://www.rfc-editor.org/rfc/rfc7518">
   * RFC 7518
   * </a>.
   * </p><p>
   * The implementation of this method is derived from the
   * <a href="https://github.com/oracle/oci-cli/blob/acbb4b98d4c47d223135a20faf160b9f0fe6046b/src/oci_cli/cli_util.py#L2369">
   * OCI CLI's Python implementation
   * </a>. The JWK returned by this method only include fields which the CLI
   * would include: kty, n, e, and kid.
   * </p>
   * @param publicKey Key to encode. Not null.
   * @return JWK encoding of the key. Not null.
   * @implNote This implementation assumes the modulus (n) and exponent (e) of
   * the key are both positive integers.
   */
  private static String encodeJwk(PublicKey publicKey) {
    if (! (publicKey instanceof RSAPublicKey)) {
      throw new IllegalStateException(
        "Not an RSA public key: " + publicKey.getClass());
    }

    return format(
      "{ \"kty\": \"RSA\", \"n\": \"%s\", \"e\": \"%s\", \"kid\": \"Ignored\" }",
      Base64.getUrlEncoder().encodeToString(
        ((RSAPublicKey)publicKey).getModulus().toByteArray()),
      Base64.getUrlEncoder().encodeToString(
        ((RSAPublicKey)publicKey).getPublicExponent().toByteArray()));
  }

  /**
   * Encodes a URL parameter value, replacing reserved characters with a percent
   * encoding.
   * @param value Value to encode. Not null.
   * @return The encoded value. Not null.
   */
  private static String encodeUrlParameter(String value) {
    try {
      return URLEncoder.encode(value, UTF_8.name());
    }
    catch (UnsupportedEncodingException utf8NotSupported) {
      throw new IllegalStateException(utf8NotSupported);
    }
  }

  /**
   * Awaits the completion of a {@code loginFuture}.
   * @param loginFuture Future to await. Not null.
   * @return The result that {@code loginFuture} completes with.
   * @throws IllegalStateException If the current thread is interrupted, or
   * the timeout expires, or the {@code loginFuture} completes exceptionally.
   */
  private static LoginResult awaitLogin(
    CompletableFuture<LoginResult> loginFuture) {
    try {
      return loginFuture.get();
    }
    catch (InterruptedException interruptedException) {
      throw new IllegalStateException(
        "Interactive authentication interrupted", interruptedException);
    }
    catch (ExecutionException executionException) {
      throw new IllegalStateException(executionException);
    }
  }

  /**
   * The result of a successful login in the browser. This class stores the
   * "id_token" and "security_token" that sent to the local HTTP server from
   * the browser.
   */
  private static final class LoginResult {

    final String securityToken;

    final String idToken;

    private LoginResult(String securityToken, String idToken) {
      this.securityToken = securityToken;
      this.idToken = idToken;
    }

    /**
     * Returns the region of the issuer identified in the ID token of the
     * login result.
     * @return The region of the issuer. Not null.
     */
    Region getIssuerRegion() {

      Map<String,String> idTokenClaims =
        JsonWebTokenParser.parseClaims(idToken);
      String regionCode = idTokenClaims.get("issuer_region");

      if (regionCode == null) {
        throw new IllegalStateException(
          "id_token does not contain an issuer_region claim");
      }

      return Region.fromRegionCode(regionCode);
    }

    /**
     * Parses the result of a login from the query section of the URI for the
     * /token endpoint of the local HTTP server.
     * @param query Query section, composed as name=value[&name=value*] pairs.
     * Not null.
     * @return The parsed login result.
     */
    static LoginResult fromUriQuery(String query) {

      if (query == null) {
        throw new IllegalStateException(
          "Query section not included in request on /token endpoint");
      }

      Map<String, String> queryParameters =
        Arrays.stream(query.split("&"))
          .map(nameEqualsValue -> nameEqualsValue.split("="))
          .collect(Collectors.toMap(
            nameValue -> nameValue[0],
            nameValue -> nameValue.length == 1 ? "" : nameValue[1]));

      String securityToken = queryParameters.get("security_token");
      if (securityToken == null) {
        throw new IllegalStateException(
          "Query section does not include a security_token in request on" +
            " /token endpoint");
      }

      String idToken = queryParameters.get("id_token");
      if (idToken == null) {
        throw new IllegalStateException(
          "Query section does not include a id_token in request on" +
            " /token endpoint");
      }

      return new LoginResult(securityToken, idToken);
    }
  }

  /**
   * <p>
   * An HTML script element containing JavaScript code. The local HTTP server
   * sends this script in response to a GET request for the root path. The
   * script is derived from the
   * <a href="https://github.com/oracle/oci-cli/blob/acbb4b98d4c47d223135a20faf160b9f0fe6046b/src/oci_cli/cli_setup_bootstrap.py#L279">
   * OCI CLI's Python implementation
   * </a>.
   * </p><p>
   * This script executes a GET request to the local HTTP server's /token
   * endpoint. The query section of the request URL includes the value of
   * "window.location.hash". In JavaScript, {@code window.location.hash} returns
   * the fragment section of the URL for the current page. For example, if the
   * current page has a URL of:
   * "https://oracle.com/example?x=0#This-is-the-fragment", then
   * {@code window.location.hash} has a value of "This-is-the-fragment".
   * </p><p>
   * It is expected that the current page has a URL fragment section of:
   * "[name=value&...]{security_token=.+}[&name=value...]. That is, the fragment
   * is a "name=value" URL query section that specifies a "security_token"
   * parameter.
   * </p>
   */
  private static final byte[] SCRIPT_RESPONSE =
    ("<script type='text/javascript'>\n" +
    "  hash = window.location.hash\n" +
    "  window.location.hash = '';\n" +
    "  \n" +
    "  // Remove the leading '#' from the URL fragment\n" +
    "  if (hash[0] === '#') {\n" +
    "      hash = hash.substr(1)\n" +
    "  }\n" +
    "  \n" +
    "  function reqListener () {\n" +
    "      document.write('Authorization completed! Please close this window and return to your application.')\n" +
    "      document.close();\n" +
    "  }\n" +
    "  \n" +
    "  var oReq = new XMLHttpRequest();\n" +
    "  oReq.addEventListener(\"load\", reqListener);\n" +
    "  oReq.open(\"GET\", \"/token?\" + hash);\n" +
    "  oReq.send();\n" +
    "</script>").getBytes(UTF_8);

}
