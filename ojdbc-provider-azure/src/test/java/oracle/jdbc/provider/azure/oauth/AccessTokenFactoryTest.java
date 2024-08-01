package oracle.jdbc.provider.azure.oauth;

import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.provider.azure.authentication.TokenCredentialFactory;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static oracle.jdbc.provider.TestProperties.getOrAbort;
import static oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod.SERVICE_PRINCIPLE;
import static oracle.jdbc.provider.azure.authentication.TokenCredentialFactoryTest.buildParameterSet;
import static org.junit.Assert.assertSame;
/**
 * Test class for AccessTokenFactory.
 * This class contains unit tests to verify the functionality and thread safety of AccessToken caching.
 */
public class AccessTokenFactoryTest {

  private CachedResourceFactory<Supplier<?extends AccessToken>> cachedResourceFactory;
  private ParameterSet parameterSet;

  /**
   * Sets up the test environment by initializing the parameter set and cached resource factory.
   */
  @Before
  public void setUp() {
    parameterSet = getParameterSet("dummy"+0);
    cachedResourceFactory = (CachedResourceFactory<Supplier<? extends AccessToken>>) AccessTokenFactory.getInstance();
  }
  /**
   * Helper method to create a ParameterSet with a dummy value.
   *
   * @param dummyValue the dummy value to be added to the parameter set
   * @return a  ParameterSet
   */
  private static ParameterSet getParameterSet(String dummyValue) {
    Parameter<String> dummyParameter = Parameter.create();

    return buildParameterSet(SERVICE_PRINCIPLE)
            .add(
                    AzureTestProperty.AZURE_CLIENT_SECRET.name(),
                    TokenCredentialFactory.CLIENT_SECRET,
                    getOrAbort(AzureTestProperty.AZURE_CLIENT_SECRET))
            .add("SCOPE", AccessTokenFactory.SCOPE, TestProperties.getOrAbort(AzureTestProperty.AZURE_TOKEN_SCOPE))
            .add("dummy parameter",dummyParameter,dummyValue)
            .build();
  }

  /**
   * Tests the thread safety of the access token cache by running multiple threads requesting the token concurrently.
   */
  @Test
  public void  testAccessTokenCacheThreadSafety() throws InterruptedException, ExecutionException {
    ExecutorService executorService = Executors.newFixedThreadPool(16);

    CompletableFuture<AccessToken> [] futures = new CompletableFuture[16];
    for (int i = 0; i < 16; i++) {
      futures[i] = CompletableFuture.supplyAsync(()->{
        AccessToken perviousToken = null;
        for (int j = 0; j < 100; j++) {
          AccessToken currentAccessToken = cachedResourceFactory.request(parameterSet).getContent().get();
          if (perviousToken!= null && perviousToken != currentAccessToken){
            throw new AssertionError("AccessToken instance changed during iteration");
          }
          perviousToken = currentAccessToken;
        }
        return perviousToken;
      }, executorService);
    }

    // wait all the futures to finish
    CompletableFuture.allOf(futures).join();

    AccessToken sharedToken = futures[0].get();
    // test that all futures retrieved from different threads are same
    for (CompletableFuture<AccessToken> future: futures){
      assertSame("All threads should return same Access Token ",sharedToken,future.get());
    }

    executorService.shutdown();
  }


  /**
   * Tests caching of more than one token by using different parameter sets.
   */
  @Test
  public void  testCachingMoreThanOneToken() throws InterruptedException, ExecutionException {
    ParameterSet parameterSet2 = getParameterSet("dummy1");

    AccessToken tokenX = cachedResourceFactory.request(parameterSet).getContent().get();
    AccessToken tokenY = cachedResourceFactory.request(parameterSet2).getContent().get();

    Assert.assertNotSame("This two Access Tokens should be different  ",tokenX,tokenY);
  }

  /**
   * Tests the uniqueness of tokens with multiple scopes and threads by running multiple threads
   * with different parameter sets and verifying that tokens are unique per parameter set.
   */
  @Test
  public void testTokenUniquenessWithMultipleScopesAndThreads() throws InterruptedException, ExecutionException {
    // create 4 parameter set each one  will have a 16 thread that have 100 iteration to request token
    int numParameterSet = 4;
    int numThreads = 16;
    int numIterations = 100;


    ExecutorService executorService = Executors.newFixedThreadPool(numThreads*numParameterSet);

    Map<String,CompletableFuture<AccessToken>[]>  completableFutureMap = new ConcurrentHashMap<>();
    for (int i=0;i<numParameterSet;i++){
      String dummyParameter = "dummy"+i;
      ParameterSet parameterSet = getParameterSet(dummyParameter);
      CompletableFuture<AccessToken>[] futures =new CompletableFuture[numThreads];

      for (int j=0;j<numThreads;j++){
        futures[j] = CompletableFuture.supplyAsync(()->{
          AccessToken previousAccessToken = null;
          for (int k = 0; k < numIterations; k++) {
            AccessToken currentAccessToken = cachedResourceFactory.request(parameterSet).getContent().get();
            if (previousAccessToken != null && previousAccessToken != currentAccessToken){
              throw new AssertionError("This Access Token instances should be same  ");
            }
            previousAccessToken = currentAccessToken;
          }
          return previousAccessToken;

        },executorService);
      }
      completableFutureMap.putIfAbsent(dummyParameter,futures);

    }

    // wait all futures to join
    for (CompletableFuture<AccessToken>[] futures : completableFutureMap.values()) {
      CompletableFuture.allOf(futures).join();
    }

    // test if all instances that have same parameter set  are same .
    for (int i = 0; i < numParameterSet; i++) {
      String dummyParameter = "dummy"+i;
      CompletableFuture<AccessToken>[] parameterSetFutures = completableFutureMap.get(dummyParameter);
      AccessToken firstAccessToken = parameterSetFutures[0].get();
      for (Future<AccessToken> currentAccessToken : parameterSetFutures){
        Assert.assertSame("This instances should be same ,because we use same parameter set  ",firstAccessToken,
                currentAccessToken.get());
      }
    }

    // test that all instances that doesn't have same parameter set will be different
    Set<AccessToken> tokenSet = new HashSet<>();
    for (CompletableFuture<AccessToken>[] future : completableFutureMap.values()){
      AccessToken accessToken = future[0].get();
      if (!tokenSet.add(accessToken)){
        throw new AssertionError("This tokens should be unique per parameter set ");
      }
    }

  }

}


