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
 * A factory for caches of access tokens requested from the Dataplane service.
 * The cache is exposed as a Supplier function, where get() returns a cached
 * token. While {@link CachedResourceFactory} provides a generalized caching
 * mechanism for all resources, Oracle JDBC provides a cache implementation
 * for access tokens. This specialized cache schedule a background thread to
 * request a new token shortly before a cached token expires. This avoids
 * threads becoming blocked while waiting for a new token to be requested.
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

    // Access tokens are sensitive, but the Supplier object is not. The Supplier
    // returned by createJsonWebTokenCache does not implement toString() to
    // expose the cached token.
    return Resource.createPermanentResource(accessTokenCache, false);
  }
}
