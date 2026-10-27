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

package oracle.jdbc.provider.spring.oauth;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * <p>
 * An {@link OAuth2ResourceFactory} factory that returns a token that authorizes
 * access to a resource. This token can be used by Oracle JDBC for authorizing
 * database access.
 * </p><p>
 * The {@link #request(ParameterSet)} method of this factory must be called with
 * a {@link ParameterSet} containing {@link Parameter}s declared by the
 * {@link OAuth2ResourceFactory} superclass.
 * </p>
 */
public class OAuth2AccessTokenFactory
  extends OAuth2ResourceFactory<OAuth2AccessToken> {

  /**
   * This factory should not be wrapped in a
   * {@link oracle.jdbc.provider.cache.CachedResourceFactory}. This is because
   * the super-class {@link OAuth2ResourceFactory#request(ParameterSet)} method
   * may access information from the
   * {@link org.springframework.security.core.context.SecurityContextHolder},
   * and this information is not part of the {@link ParameterSet} that is used
   * as a cache key.
   */
  private static final OAuth2AccessTokenFactory INSTANCE =
    new OAuth2AccessTokenFactory();

  private OAuth2AccessTokenFactory() {}

  /**
   * Returns The sole instance of this singleton class
   * @return The sole instance of this singleton class
   */
  public static ResourceFactory<Optional<OAuth2AccessToken>> getInstance() {
    return INSTANCE;
  }

  @Override
  protected Resource<OAuth2AccessToken> request(
    OAuth2AuthorizedClient authorizedClient, ParameterSet parameterSet)
    throws IllegalStateException, IllegalArgumentException {

    OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
    Instant expiresAt = accessToken.getExpiresAt();

    return expiresAt == null
      ? Resource.createPermanentResource(accessToken, true)
      : Resource.createExpiringResource(
          accessToken,
          expiresAt.atOffset(ZoneOffset.UTC),
          true);
  }
}
