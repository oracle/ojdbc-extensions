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
import oracle.jdbc.provider.oauth.RedirectServer;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.util.JsonWebTokenParser;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

  private static final int RANDOM_VALUE_LENGTH = 64;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * <p>
   * The base of a URI where a local server is running. After a web browser
   * visits the OCI login page, it will be redirected to the host and port of
   * this URI. Query parameters may be added to this, such as the state
   * parameter.
   * </p><p>
   * The OCI CLI listens on port 8181, so this class will do the same. It is
   * believed that the redirect URI of "http://localhost:8181" is registered
   * with the login endpoint of Oracle Cloud, and so it would reject an
   * attempt to use any other redirect URI.
   * </p>
   */
  private static final URI REDIRECT_URI = URI.create("http://localhost:8181");

  /**
   * The OCI authorization server will redirect a web browser to this endpoint
   * of the local server. This endpoint returns a JS document that the web
   * browser will execute. The script has the web browser send the token issued
   * by the authorization server to the {@link #TOKEN_ENDPOINT} of the local
   * server.
   */
  private static final String SCRIPT_ENDPOINT = "/script/";

  /**
   * Web browser sends the issued token this endpoint of the local server. This
   * action occurs when the browser executes the script returned from the
   * {@link #SCRIPT_ENDPOINT}.
   */
  private static final String TOKEN_ENDPOINT = "/token/";

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

    // Since this is optional it can be null
    Integer parameterTimeOutValue = parameterSet.getOptional(
      AuthenticationDetailsFactory.INTERACTIVE_TIMEOUT);
    int timeoutMinutes = parameterTimeOutValue == null
      ? Integer.MAX_VALUE
      : parameterTimeOutValue.intValue();

    Region region = parameterSet.getOptional(AuthenticationDetailsFactory.REGION);
    try (
      RedirectServer<LoginResult> redirectServer =
        new RedirectServer<>(REDIRECT_URI);
    ) {
      String state = getRandomValue();

      // The HTTP server expects an initial GET request for the root path "/",
      // and responds with a script that has the browser send a second
      // request. The second request is expected to be a GET request
      // for the "/token" path with a "security_token" parameter.
      // This protocol is derived from the OCI CLI's Python implementation here:
      // https://github.com/oracle/oci-cli/blob/ed9f755092a1ba9702cb1a133c152944da819df8/src/oci_cli/cli_setup_bootstrap.py#L271
      redirectServer.setHandler(
        SCRIPT_ENDPOINT, exchange -> handleScriptRequest(exchange, state));
      redirectServer.setResultHandler(
        TOKEN_ENDPOINT, exchange -> handleTokenRequest(exchange, state));

      // The token uses a key pair for proof of possession.
      KeyPair keyPair = generateKeyPair();

      // Send a web browser to the authorization endpoint, and then wait for it
      // to be redirected back to us with a session token.
      LoginResult loginResult =
        redirectServer.awaitResult(
          createAuthorizationURI(region, keyPair.getPublic(), state),
          timeoutMinutes,
          TimeUnit.MINUTES);

      // If a region has not been configured, then extract it from the
      // "issuer_region" claim of the ID token.
      if (region == null) {
        region = loginResult.getIssuerRegion();
      }

      return new InteractiveAuthenticationDetails(
        region, loginResult.securityToken, keyPair);
    }
  }

  // Generates a random byte array of length RANDOM_VALUE_LENGTH
  private static String getRandomValue() {
    byte[] bytes = new byte[RANDOM_VALUE_LENGTH];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
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
   * Handles a GET request for the / (root) path by responding with a
   * {@linkplain #SCRIPT_RESPONSE HTML script element}.
   * </p><p>
   * The implementation of this method is derived from the
   * <a href="https://github.com/oracle/oci-cli/blob/ed9f755092a1ba9702cb1a133c152944da819df8/src/oci_cli/cli_setup_bootstrap.py#L277">
   * OCI CLI's Python implementation
   * </a>.
   * </p>
   * @param httpExchange The HTTP request. Not null.
   * @param expectedState The generated state to verify that the redirect url has
   *                      been generated by this application
   * @throws IllegalStateException If the request does not include the
   *                               expected state value.
   */
  @SuppressWarnings("try")
  private static void handleScriptRequest(
    HttpExchange httpExchange,  String expectedState
  ) {
    try (AutoCloseable autoClose = httpExchange::close) {
      Map<String, String> queryParameters =
        parseQueryParameters(httpExchange.getRequestURI().getRawQuery());

      String state = queryParameters.get("state");
      if (!expectedState.equals(state)) {
        httpExchange.sendResponseHeaders(401, ERROR_SCRIPT_RESPONSE.length);
        httpExchange.getResponseBody().write(ERROR_SCRIPT_RESPONSE);
        throw new IllegalStateException(
          "Query section does not include the expected state on the"
            + " /script endpoint");
      } else {
        httpExchange.sendResponseHeaders(200, SCRIPT_RESPONSE.length);
        httpExchange.getResponseBody().write(SCRIPT_RESPONSE);
      }
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
  private static LoginResult handleTokenRequest(
    HttpExchange httpExchange, String expectedState
  ) {
    try (AutoCloseable autoClose = httpExchange::close) {

      Map<String, String> queryParameters =
        parseQueryParameters(httpExchange.getRequestURI().getRawQuery());

      String state = queryParameters.get("state");
      if (!expectedState.equals(state)) {
        httpExchange.sendResponseHeaders(401, ERROR_SCRIPT_RESPONSE.length);
        httpExchange.getResponseBody().write(ERROR_SCRIPT_RESPONSE);
        throw new IllegalStateException(
          "Query section does not include the expected state on the"
            + " /token endpoint");
      }

      LoginResult loginResult = LoginResult.fromQueryParameters(queryParameters);
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
   * Creates a URI that connects to an OCI login page when opened in a web
   * browser. The login page may redirect
   * the session token to the given {@code redirectURI} upon successful
   * authentication.
   * </p>
   * <p>
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
   * @param region          OCI region to connect to. Not null.
   * @param publicKey       Public key used for proof of possession with the
   *                        session
   *                        token. Not null.
   * @param state           The state will be added to the redirectUrl so that
   *                        when we receive the token, we know it is from the
   *                        user who initiated a login in the current
   *                        application.
   * @throws IllegalStateException If a browser can not be opened.
   */
  private static URI createAuthorizationURI(
    Region region, PublicKey publicKey, String state) {
      return URI.create(
        format("https://login.%s.%s/v1/oauth2/authorize",
          region == null ? "oci" : region.getRegionId(),
          region == null ? "oraclecloud.com" : region.getRealm().getSecondLevelDomain()) +
          "?action=login" +
          "&client_id=iaas_console" +
          "&response_type=" +
            encodeUrlParameter("token id_token") +
          // The browser may send a cached token from a previous login, and the
          // token contains a "nonce" claim that was randomly generated in a
          // previous request. We don't validate the nonce claim, because we don't
          // know if it was generated here, or by a previous request.
          // Instead, we validate the state parameter of the redirect URI.
          "&nonce=" +
            encodeUrlParameter(UUID.randomUUID().toString()) +
          "&scope=openid" +
          "&public_key=" +
            encodeUrlParameter(Base64.getUrlEncoder().encodeToString(
              encodeJwk(publicKey).getBytes(UTF_8))) +
          "&redirect_uri=" +
            encodeUrlParameter(format("http://%s:%d%s?state=%s",
              REDIRECT_URI.getHost(),
              REDIRECT_URI.getPort(),
              SCRIPT_ENDPOINT,
              // double-encode: When the OCI server decodes this parameter, it
              // should result in a redirect URI having a percent-encoded state
              // parameter. The server will redirect the browser to this URI, with
              // its percent-encoded state parameter.
              encodeUrlParameter(state))));
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
   * <a href=
   * "https://github.com/oracle/oci-cli/blob/acbb4b98d4c47d223135a20faf160b9f0fe6046b/src/oci_cli/cli_util.py#L2369">
   * OCI CLI's Python implementation
   * </a>. The JWK returned by this method only include fields which the CLI
   * would include: kty, n, e, and kid.
   * </p>
   * 
   * @param publicKey Key to encode. Not null.
   * @return JWK encoding of the key. Not null.
   * @implNote This implementation assumes the modulus (n) and exponent (e) of
   *           the key are both positive integers.
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
     * @param queryParameters Parameters parsed from the query section of the
     * request URI. Not null.
     * @return The parsed login result.
     */
    static LoginResult fromQueryParameters(Map<String, String> queryParameters) {

      if (queryParameters.isEmpty()) {
        throw new IllegalStateException(
          "Query section not included in request on /token endpoint");
      }

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
    "  var hash = window.location.hash;\n" +
    "  window.location.hash = '';\n" +
    "  \n" +
    "  // Remove the leading '#' from the URL fragment\n" +
    "  if (hash && hash[0] === '#') {\n" +
    "      hash = hash.substr(1);\n" +
    "  }\n" +
    "  \n" +
    "  var hashParams = new URLSearchParams(hash);\n" +
    "  var searchParams = new URLSearchParams(window.location.search);\n" +
    "  var stateParam = searchParams.get('state');\n" +
    "  if (stateParam !== null) {\n" +
    "      hashParams.set('state', stateParam);\n" +
    "  }\n" +
    "  \n" +
    "  function reqListener () {\n" +
    "      document.write('Authorization completed! Please close this window and return to your application.')\n" +
    "      document.close();\n" +
    "  }\n" +
    "  \n" +
    "  var oReq = new XMLHttpRequest();\n" +
    "  oReq.addEventListener(\"load\", reqListener);\n" +
    "  oReq.open(\"GET\", \"" + TOKEN_ENDPOINT + "?\" + hashParams.toString());\n" +
    "  oReq.send();\n" +
    "</script>").getBytes(UTF_8);

    private static final byte[] ERROR_SCRIPT_RESPONSE =
      ("Unauthorized login! Please close this window and return to your application.").getBytes(UTF_8);

  private static Map<String, String> parseQueryParameters(String rawQuery) {
    if (rawQuery == null || rawQuery.isEmpty()) {
      return Collections.emptyMap();
    }

    return Arrays.stream(rawQuery.split("&"))
      .map(nameEqualsValue -> nameEqualsValue.split("=", 2))
      .collect(Collectors.toMap(
        nameValue -> URLDecoder.decode(nameValue[0], UTF_8),
        nameValue -> nameValue.length == 1 ? "" : URLDecoder.decode(nameValue[1], UTF_8)
      ));
  }

}
