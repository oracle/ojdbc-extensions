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
package oracle.jdbc.provider.oauth;

import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.function.Supplier;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * <p>
 * A factory for caches of access tokens requested from a
 * {@code ResourceFactory<AccessToken>}. The delegate ResourceFactory must be
 * configured as the {@link #FACTORY} parameter.
 * </p><p>
 * This factory returns a cache in the form of a Supplier function. A call to
 * Supplier.get() returns a cached token.
 * </p><p>
 * While {@link CachedResourceFactory} provides a generalized caching
 * mechanism for all resources, Oracle JDBC provides a cache implementation
 * that is specialized for access tokens. The specialized cache will schedule a
 * background thread to request a new token shortly before a cached token will
 * expire. This avoids threads becoming blocked while waiting for a new token to
 * be requested.
 * </p>
 */
public final class AccessTokenCacheFactory
  implements ResourceFactory<Supplier<? extends AccessToken>> {

  /**
   * A factory which requests tokens for the caches created by this factory.
   */
  public static final Parameter<ResourceFactory<AccessToken>> FACTORY =
    Parameter.create(REQUIRED);

  private static final ResourceFactory<Supplier<? extends AccessToken>> INSTANCE =
    CachedResourceFactory.create(new AccessTokenCacheFactory());

  private AccessTokenCacheFactory() { }

  /**
   * Returns a singleton of {@code AccessTokenCacheFactory}.
   * @return a singleton of {@code AccessTokenCacheFactory}
   */
  public static ResourceFactory<Supplier<? extends AccessToken>> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<Supplier<? extends AccessToken>> request(
    ParameterSet parameterSet) {

    ResourceFactory<AccessToken> factory = parameterSet.getRequired(FACTORY);

    Supplier<? extends AccessToken> accessTokenCache =
      AccessToken.createJsonWebTokenCache(() ->
        factory.request(parameterSet).getContent());

    // Access tokens are security sensitive, but the Supplier object is not. The
    // Supplier returned by createJsonWebTokenCache does not implement
    // toString() to expose the cached token, or any other security sensitive
    // information such as proof-of-possession key.
    return Resource.createPermanentResource(accessTokenCache, false);
  }
}
