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

package oracle.jdbc.provider.nimbus.resource;

import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory;
import oracle.jdbc.provider.oauth.AccessTokenCacheFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.AbstractResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.spi.AccessTokenProvider;

import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.AUTHORIZATION_ENDPOINT;
import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.CLIENT_ID;
import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.CLIENT_SECRET;
import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.GRANT_TYPE;
import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.PASSWORD;
import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.REDIRECT_URI;
import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.SCOPE;
import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.TOKEN_ENDPOINT;
import static oracle.jdbc.provider.nimbus.oauth.AccessTokenFactory.USERNAME;

/**
 * Adapts the Nimbus SDK to the AccessTokenProvider interface which is consumed
 * by Oracle JDBC.
 */
public final class NimbusTokenProvider
  extends AbstractResourceProvider
  implements AccessTokenProvider {

  private static final Logger LOGGER =
    Logger.getLogger(NimbusTokenProvider.class.getName());

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter("grantType", GRANT_TYPE, null, value -> {
        try {
          return GrantType.parse(value);
        }
        catch (ParseException parseException) {
          throw new IllegalArgumentException(parseException);
        }
      }),
    new ResourceParameter("tokenEndpoint", TOKEN_ENDPOINT, null, URI::create),
    new ResourceParameter("scope", SCOPE, null, Scope::parse),
    new ResourceParameter("clientId", CLIENT_ID, null, ClientID::new),
    new ResourceParameter("clientSecret", CLIENT_SECRET, null, Secret::new),
    new ResourceParameter("username", USERNAME),
    new ResourceParameter("password", PASSWORD, null, Secret::new),
    new ResourceParameter(
      "authorizationEndpoint", AUTHORIZATION_ENDPOINT, null, URI::create),
    new ResourceParameter("redirectUri", REDIRECT_URI, null, URI::create),
    new ResourceParameter("factory", AccessTokenCacheFactory.FACTORY,
      "default", ignored -> AccessTokenFactory.getInstance())
  };

  /**
   * This public no-arg constructor is required for
   * {@link java.util.ServiceLoader} to load this class.
   */
  public NimbusTokenProvider() {
    super("nimbus", "token", PARAMETERS);
  }

  @Override
  public AccessToken getAccessToken(
    Map<Parameter, CharSequence> parameterValues
  ) {
    try {
      ParameterSet parameterSet = parseParameterValues(parameterValues);
      return AccessTokenCacheFactory.getInstance()
        .request(parameterSet)
        .getContent()
        .get();
    }
    catch (RuntimeException | Error error) {
      LOGGER.log(
        Level.SEVERE,
        "Failure occurred when requesting an access token for Oracle JDBC",
        error);
      throw error;
    }
  }
}
