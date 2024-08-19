package oracle.jdbc.provider.oauth;

import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for AccessTokenFactory
 * This class has some unit test to verify the functionality and thread safety
 * of accessToken caching.
 */
public class AccessTokenCacheFactoryTest {

  private static final ResourceFactory<Supplier<? extends AccessToken>> CACHE_FACTORY =
    AccessTokenCacheFactory.getInstance();

  /**
   * Helper method to create a ParameterSet with a dummy value.
   *
   * @param dummyValue the dummy value to be added to the parameter set
   * @return a  ParameterSet
   */
  private static ParameterSet getParameterSet(String dummyValue) {
    return ParameterSet.builder()
      .add("dummy", TokenFactory.DUMMY, dummyValue)
      .add("factory", AccessTokenCacheFactory.FACTORY, new TokenFactory())
      .build();
  }

  /**
   * Tests the thread safety of the access token cache by running multiple
   * threads requesting the same token concurrently.
   */
  @Test
  public void  testAccessTokenCacheThreadSafety() throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(16);
    try (AutoCloseable shutdown = executorService::shutdown) {

      ParameterSet parameterSet = getParameterSet("dummy0");

      @SuppressWarnings("unchecked")
      CompletableFuture<AccessToken>[] futures = new CompletableFuture[16];
      for (int i = 0; i < 16; i++) {
        futures[i] = CompletableFuture.supplyAsync(() -> {
          AccessToken previous = null;
          for (int j = 0; j < 100; j++) {

            AccessToken current =
              CACHE_FACTORY.request(parameterSet).getContent().get();

            if (previous != null) {
              assertSame(previous, current);
            }

            previous = current;
          }
          return previous;
        }, executorService);
      }

      // wait all the futures to finish
      CompletableFuture.allOf(futures).join();

      AccessToken sharedToken = futures[0].get();
      // test that all futures retrieved from different threads are same
      for (CompletableFuture<AccessToken> future : futures) {
        Assertions.assertSame(sharedToken, future.get(),
          "All threads should return same Access Token");
      }
    }
  }


  /**
   * Tests caching of more than one token by using different parameter sets.
   */
  @Test
  public void  testCachingMoreThanOneToken() {

    ParameterSet parameterSet0 = getParameterSet("dummy0");
    AccessToken token0 = CACHE_FACTORY.request(parameterSet0).getContent().get();

    ParameterSet parameterSet1 = getParameterSet("dummy1");
    AccessToken token1 = CACHE_FACTORY.request(parameterSet1).getContent().get();

    Assertions.assertNotSame(token0, token1);
  }

  /**
   * Tests the uniqueness of tokens with multiple scopes and threads by running multiple threads
   * with different parameter sets and verifying that tokens are unique per parameter set.
   */
  @Test
  public void testTokenUniquenessWithMultipleScopesAndThreads()
    throws Exception {

    // create 4 parameter set each one  will have a 16 thread that have 100 iteration to request token
    int numParameterSet = 4;
    int numThreads = 16;
    int numIterations = 100;

    ExecutorService executorService =
      Executors.newFixedThreadPool(numThreads * numParameterSet);
    try (AutoCloseable shutdown = executorService::shutdown) {

      Map<String, CompletableFuture<AccessToken>[]> completableFutureMap =
        new ConcurrentHashMap<>();

      for (int i = 0; i < numParameterSet; i++) {

        String dummyParameter = "dummy" + i;
        ParameterSet parameterSet = getParameterSet(dummyParameter);

        @SuppressWarnings("unchecked")
        CompletableFuture<AccessToken>[] futures =
          new CompletableFuture[numThreads];

        for (int j = 0; j < numThreads; j++) {
          futures[j] = CompletableFuture.supplyAsync(() -> {
            AccessToken previous = null;

            for (int k = 0; k < numIterations; k++) {
              AccessToken current =
                CACHE_FACTORY.request(parameterSet).getContent().get();

              if (previous != null) {
                assertSame(previous, current);
              }

              previous = current;
            }

            return previous;

          }, executorService);
        }
        completableFutureMap.putIfAbsent(dummyParameter, futures);

      }

      // wait all futures to join
      for (CompletableFuture<AccessToken>[] futures : completableFutureMap.values()) {
        CompletableFuture.allOf(futures).join();
      }

      // test if all threads that used the same parameter got the same token
      for (int i = 0; i < numParameterSet; i++) {
        String dummyParameter = "dummy" + i;
        CompletableFuture<AccessToken>[] parameterSetFutures =
          completableFutureMap.get(dummyParameter);
        AccessToken firstAccessToken = parameterSetFutures[0].get();

        for (Future<AccessToken> currentAccessToken : parameterSetFutures) {
          Assertions.assertSame(firstAccessToken, currentAccessToken.get(),
            "This instances should be same, because we use same parameter set");
        }
      }

      // test that all instances that doesn't have same parameter set will be different
      Set<AccessToken> tokenSet = new HashSet<>();
      for (CompletableFuture<AccessToken>[] future : completableFutureMap.values()) {
        AccessToken accessToken = future[0].get();
        if (!tokenSet.add(accessToken)) {
          throw new AssertionError(
            "This tokens should be unique per parameter set");
        }
      }
    }
  }

  /**
   * Mock implementation of an AccessToken factory. This factory creates fake
   * AccessToken objects for each request. Tests can verify that the
   * AccessTokenCacheFactory does return not different instances of AccessToken
   * for the same ParameterSet.
   */
  private static final class TokenFactory
    implements ResourceFactory<AccessToken> {

    private static final Parameter<String> DUMMY = Parameter.create();

    @Override
    public Resource<AccessToken> request(ParameterSet parameterSet)
      throws IllegalStateException, IllegalArgumentException {

      String dummy = parameterSet.getRequired(DUMMY);

      // JDBC performs a light validation on the JWT, just checking for
      // 3 dot-separated JSON objects and an exp claim. The exp claim is read
      // to trigger a cache update, so make sure it's expiring in the future.
      // Instances of AccessToken implement equals(Object) by comparing the
      // JWT characters; Add a "dummy" field in the payload to make the
      // characters unique.
      long exp = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond() + 6000;
      String header = "{}";
      String payload = "{\"exp\" : " + exp + ", \"dummy\" : \"" + dummy + "\"}";
      String signature = "{}";
      String jwt = Stream.of(header, payload, signature)
        .map(string -> string.getBytes(StandardCharsets.UTF_8))
        .map(utf8 -> Base64.getEncoder().encodeToString(utf8))
        .collect(Collectors.joining("."));

      AccessToken token = AccessToken.createJsonWebToken(jwt.toCharArray());

      return Resource.createPermanentResource(token, false);
    }
  }
}


