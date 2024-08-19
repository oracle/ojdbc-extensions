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
