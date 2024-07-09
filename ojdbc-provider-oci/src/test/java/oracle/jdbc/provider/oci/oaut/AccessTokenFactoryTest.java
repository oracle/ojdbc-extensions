package oracle.jdbc.provider.oci.oaut;

import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.provider.oci.authentication.AuthenticationDetailsFactory;
import oracle.jdbc.provider.oci.authentication.AuthenticationMethod;
import oracle.jdbc.provider.oci.oauth.AccessTokenFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
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
import static oracle.jdbc.provider.oci.AuthenticationDetailsFactoryTest.buildParameterSet;
import static org.junit.Assert.assertSame;


public class AccessTokenFactoryTest {

    private CachedResourceFactory<Supplier<?extends AccessToken>> cachedResourceFactory;
    private ParameterSet parameterSetMock;


    @Before
    public void setUp() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        parameterSetMock = getParameterSet("dummy0");
        cachedResourceFactory = (CachedResourceFactory<Supplier<? extends AccessToken>>)AccessTokenFactory.getInstance();
    }

    private static ParameterSet getParameterSet(String dummyValue) {
        Parameter<String> dummyParameter = Parameter.create();
        return buildParameterSet(AuthenticationMethod.CONFIG_FILE)
                .add(
                        OciTestProperty.OCI_CONFIG_FILE.name(),
                        AuthenticationDetailsFactory.CONFIG_FILE_PATH,
                        getOrAbort(OciTestProperty.OCI_CONFIG_FILE))
                .add(
                        OciTestProperty.OCI_CONFIG_PROFILE.name(),
                        AuthenticationDetailsFactory.CONFIG_PROFILE,
                        getOrAbort(OciTestProperty.OCI_CONFIG_PROFILE))
                .add("SCOPE", AccessTokenFactory.SCOPE, TestProperties.getOrAbort(OciTestProperty.OCI_TOKEN_SCOPE))
                .add("dummy parameter",dummyParameter,dummyValue)
                .build();
    }


    @Test
    public void  testAccessTokenCacheThreadSafety() throws InterruptedException, ExecutionException {
        // Act
        ExecutorService executorService = Executors.newFixedThreadPool(16);

        CompletableFuture<AccessToken>[] futures = new CompletableFuture[16];
        for (int i = 0; i < 16; i++) {
            futures[i] = CompletableFuture.supplyAsync(()->{
                AccessToken perviousToken = null;
                for (int j = 0; j < 100; j++) {
                    AccessToken currentAccessToken = cachedResourceFactory.request(parameterSetMock).getContent().get();
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



    @Test
    public void  testCachingMoreThanOneToken() throws InterruptedException, ExecutionException {

        ParameterSet parameterSet2 = getParameterSet("dummy1");
        AccessToken tokenX = cachedResourceFactory.request(parameterSetMock).getContent().get();
        AccessToken tokenY = cachedResourceFactory.request(parameterSet2).getContent().get();

        Assert.assertNotSame("This two Access Tokens should be different  ",tokenX,tokenY);
    }


    @Test
    public void testTokenUniquenessWithMultipleScopesAndThreads() throws InterruptedException, ExecutionException {
        // create 4 parameter set each one  will have a 16 thread that have 100 iteration to request token
        int numParameterSet = 4;
        int numThreads = 16;
        int numIterations = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads*numParameterSet);

        Map<String,CompletableFuture<AccessToken>[]> completableFutureMap = new ConcurrentHashMap<>();
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
                Assert.assertSame("This instances should be same ,because we use same parameter set  ",firstAccessToken,currentAccessToken.get());
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


