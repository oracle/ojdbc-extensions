/*
 *  Copyright (c) 2026 Oracle and/or its affiliates.
 *
 *  The Universal Permissive License (UPL), Version 1.0
 *
 *  Subject to the condition set forth below, permission is hereby granted to any
 *  person obtaining a copy of this software, associated documentation and/or data
 *  (collectively the "Software"), free of charge and under any and all copyright
 *  rights in the Software, and any and all patent rights owned or freely
 *  licensable by each licensor hereunder covering either (i) the unmodified
 *  Software as contributed to or provided by such licensor, or (ii) the Larger
 *  Works (as defined below), to deal in both
 *
 *  (a) the Software, and
 *  (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *  one is included with the Software (each a "Larger Work" to which the Software
 *  is contributed by such licensors),
 *
 *  without restriction, including without limitation the rights to copy, create
 *  derivative works of, display, perform, and distribute the Software and make,
 *  use, sell, offer for sale, import, export, have made, and have sold the
 *  Software and the Larger Work(s), and to sublicense the foregoing rights on
 *  either these or other terms.
 *
 *  This license is subject to the following condition:
 *  The above copyright notice and either this complete permission notice or at
 *  a minimum a reference to the UPL must be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package oracle.jdbc.provider.nimbus.oauth;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.sun.net.httpserver.HttpExchange;
import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.oauth.RedirectServer;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.util.JsonWebTokenParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.SENSITIVE;

/**
 * A factory that requests OAuth 2.0 access tokens using the Nimbus SDK.
 */
public final class AccessTokenFactory implements ResourceFactory<AccessToken> {

  /**
   * Grant type that identifies how to authenticate when requesting a
   * token.
   */
  public static final Parameter<GrantType> GRANT_TYPE =
    Parameter.create(REQUIRED);

  /**
   * URI where an access token is requested from.
   */
  public static final Parameter<URI> TOKEN_ENDPOINT =
    Parameter.create(REQUIRED);

  /**
   * Scope identifying the resources a client will access.
   */
  public static final Parameter<Scope> SCOPE =
    Parameter.create();

  /**
   * ID of an OAuth 2.0 client that is requesting an access token.
   */
  public static final Parameter<ClientID> CLIENT_ID =
    Parameter.create();

  /**
   * Secret used for {@link GrantType#CLIENT_CREDENTIALS}
   */
  public static final Parameter<Secret> CLIENT_SECRET =
    Parameter.create(SENSITIVE);

  /**
   * Username for {@link GrantType#PASSWORD}
   */
  public static final Parameter<String> USERNAME =
    Parameter.create();

  /**
   * Password for {@link GrantType#PASSWORD}
   */
  public static final Parameter<Secret> PASSWORD =
    Parameter.create(SENSITIVE);

  /**
   * URI where an authorization code is requested from.
   */
  public static final Parameter<URI> AUTHORIZATION_ENDPOINT =
    Parameter.create();

  /**
   * URI where an authorization code is redirected to.
   */
  public static final Parameter<URI> REDIRECT_URI =
    Parameter.create();

  private AccessTokenFactory() {}
  private static final AccessTokenFactory INSTANCE = new AccessTokenFactory();

  /**
   * Returns an instance of this factory.
   * @return This factory. Not null.
   */
  public static AccessTokenFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<AccessToken> request(ParameterSet parameterSet)
    throws IllegalStateException, IllegalArgumentException {

    TokenRequest request =
      createBuilder(parameterSet)
        .scope(parameterSet.getOptional(SCOPE))
        .build();

    final HTTPResponse httpResponse;
    try {
      httpResponse = request.toHTTPRequest().send();
    }
    catch (IOException ioException) {
      throw new IllegalStateException(ioException);
    }

    final TokenResponse response;
    try {
      response = TokenResponse.parse(httpResponse);
    }
    catch (ParseException parseException) {
      throw new IllegalStateException(parseException);
    }

    if (!response.indicatesSuccess()) {
      throw new IllegalStateException(
        response.toErrorResponse()
          .getErrorObject()
          .toJSONObject()
          .toString());
    }

    com.nimbusds.oauth2.sdk.token.AccessToken accessToken =
      response.toSuccessResponse()
        .getTokens()
        .getAccessToken();
    String tokenString = accessToken.getValue();
    return Resource.createExpiringResource(
      // TODO: Don't assume JWT, and replace with new 23.26.3 API:
      // AccessToken.create(
      //    accessToken.getValue(),
      //    Duration.ofSeconds(accessToken.getLifetime()));
      AccessToken.createJsonWebToken(tokenString.toCharArray()),
      JsonWebTokenParser.parseExp(tokenString),
      true);
  }

  /**
   * Creates a token request builder for a specific grant type. This
   * method does not need to configure any common builder
   * settings, such a {@link TokenRequest.Builder#scope(Scope)}. It just needs
   * to construct a TokenRequest.Builder, and then allo the caller of this
   * method to handle the rest.
   * <p>
   * There are few different TokenRequest.Builder constructors, so this
   * method provides a place to figure out which one should be used. The correct
   * constructor to use will depend on the grant type and whether any
   * optional parameters are configured, such as client credentials. This method
   * should abide by
   * <a href="https://datatracker.ietf.org/doc/html/rfc6749">
   * RFC 6749
   * </a> in terms of treating parameters as required vs optional. For instance,
   * a client id and client secret are required for the client credentials
   * grant type, but are optional for the ROPC grant type.
   * </p>
   * @param parameterSet Parameters that configure the request. Not null.
   * @return A TokenRequest.Builder configured for a specific grant type. Not
   * null.
   */
  private static TokenRequest.Builder createBuilder(
    ParameterSet parameterSet
  ) {
    GrantType grantType = parameterSet.getRequired(GRANT_TYPE);

    if (grantType == GrantType.CLIENT_CREDENTIALS) {
      return createClientCredentialsBuilder(parameterSet);
    }
    else if (grantType == GrantType.AUTHORIZATION_CODE) {
      return createAuthorizationCodeBuilder(parameterSet);
    }
    else if (grantType == GrantType.PASSWORD) {
      return createPasswordBuilder(parameterSet);
    }
    else {
      throw new IllegalArgumentException(
        "Unsupported grant type: " + grantType);
    }
  }

  /**
   * Creates a builder for a token request that uses a client credentials grant.
   */
  private static TokenRequest.Builder createClientCredentialsBuilder(
    ParameterSet parameterSet
  ) {
    return new TokenRequest.Builder(
      parameterSet.getRequired(TOKEN_ENDPOINT),
      new ClientSecretBasic(
        parameterSet.getRequired(CLIENT_ID),
        parameterSet.getRequired(CLIENT_SECRET)),
      new ClientCredentialsGrant());
  }

  /**
   * Creates a builder for a token request that uses an authorization code
   * grant.
   */
  private static TokenRequest.Builder createAuthorizationCodeBuilder(
    ParameterSet parameterSet
  ) {
    AuthorizationCodeGrant grant =
      requestAuthorizationCodeGrant(parameterSet);

    ClientID clientID = parameterSet.getRequired(CLIENT_ID);
    Secret clientSecret = parameterSet.getOptional(CLIENT_SECRET);

    if (clientSecret == null) {
      return new TokenRequest.Builder(
        parameterSet.getRequired(TOKEN_ENDPOINT),
        clientID,
        grant);
    }
    else {
      return new TokenRequest.Builder(
        parameterSet.getRequired(TOKEN_ENDPOINT),
        new ClientSecretBasic(clientID, clientSecret),
        grant);
    }
  }

  /**
   * Creates a builder for a token request that uses a password
   * credentials grant.
   */
  private static TokenRequest.Builder createPasswordBuilder(
    ParameterSet parameterSet
  ) {
    ResourceOwnerPasswordCredentialsGrant passwordGrant =
      new ResourceOwnerPasswordCredentialsGrant(
        parameterSet.getRequired(USERNAME),
        parameterSet.getRequired(PASSWORD));

    ClientID clientID = parameterSet.getOptional(CLIENT_ID);
    Secret clientSecret = parameterSet.getOptional(CLIENT_SECRET);

    if (clientID != null && clientSecret != null) {
      return new TokenRequest.Builder(
        parameterSet.getRequired(TOKEN_ENDPOINT),
        new ClientSecretBasic(clientID, clientSecret),
        passwordGrant);
    }
    else if (clientID != null) {
      return new TokenRequest.Builder(
        parameterSet.getRequired(TOKEN_ENDPOINT),
        clientID,
        passwordGrant);
    }
    else {
      return new TokenRequest.Builder(
        parameterSet.getRequired(TOKEN_ENDPOINT),
        passwordGrant);
    }
  }

  /**
   * Returns an authorization code grant, which requires interactive
   * authentication in a web browser.
   */
  private static AuthorizationCodeGrant requestAuthorizationCodeGrant(
    ParameterSet parameterSet
  ) {
    AuthorizationRequest request = createAuthorizationCodeRequest(parameterSet);
    URI redirectURI = request.getRedirectionURI();

    final AuthorizationCode code;
    try (
      RedirectServer<AuthorizationCode> redirectServer =
        new RedirectServer<>(redirectURI)
    ) {
      redirectServer.setResultHandler(
        "/",
        new AuthorizationCodeHandler(request.getState()));

      code = redirectServer.awaitResult(
        request.toURI(),
        5,
        TimeUnit.MINUTES);
    }

    return new AuthorizationCodeGrant(code, redirectURI);
  }

  /**
   * Creates a request for the authorization code grant.
   */
  private static AuthorizationRequest createAuthorizationCodeRequest(
    ParameterSet parameterSet) {
    return new AuthorizationRequest.Builder(
        ResponseType.CODE,
        parameterSet.getRequired(CLIENT_ID))
      .endpointURI(parameterSet.getRequired(AUTHORIZATION_ENDPOINT))
      .scope(parameterSet.getOptional(SCOPE))
      .redirectionURI(parameterSet.getRequired(REDIRECT_URI))
      .state(new State())
      .build();
  }

  /**
   * Handles the redirect from an authorization code request.
   */
  private static final class AuthorizationCodeHandler
    implements RedirectServer.ResultHandler<AuthorizationCode> {

    private static final byte[] BROWSER_MESSAGE =
      ("<html><body><center><font size=7>"
        + "Log in complete"
        + "</font></center></body></html>")
        .getBytes(UTF_8);

    /**
     * State included in the {@link AuthorizationRequest}, which is expected to
     * be sent back in the redirect.
     */
    private final State state;

    /**
     * Constructs a handler that expects a redirect with a given state.
     *
     * @param state State to expect. Not null.
     */
    AuthorizationCodeHandler(State state) {
      this.state = state;
    }

    @Override
    public AuthorizationCode handle(HttpExchange exchange) throws IOException {

      exchange.sendResponseHeaders(200, BROWSER_MESSAGE.length);
      try (OutputStream responseStream = exchange.getResponseBody()) {
        responseStream.write(BROWSER_MESSAGE);
      }

      final AuthorizationResponse response;
      try {
        response = AuthorizationResponse.parse(exchange.getRequestURI());
      }
      catch (ParseException parseException) {
        throw new IOException(parseException);
      }

      State responseState = response.getState();
      if (! state.equals(responseState)) {
        throw new IllegalStateException(
          "Response contains an insecure \"state\" value"
            + "\nRequest state: " + state
            + "\nResponse state: " + responseState);
      }

      if (! response.indicatesSuccess()) {
        AuthorizationErrorResponse errorResponse = response.toErrorResponse();
        throw new IllegalStateException(
          errorResponse.getErrorObject()
            .toJSONObject()
            .toString());
      }

      return response.toSuccessResponse().getAuthorizationCode();
    }
  }
}
