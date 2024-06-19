package oracle.jdbc.provider.oci.oaut;

import io.jsonwebtoken.Jwts;
import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.oci.authentication.AuthenticationDetailsFactory;
import oracle.jdbc.provider.oci.authentication.AuthenticationMethod;
import oracle.jdbc.provider.oci.oauth.AccessTokenFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccessTokenFactoryTest {
    @Spy
    private AccessTokenFactory accessTokenFactorySpy;
    private CachedResourceFactory<Supplier<?extends AccessToken>> cachedResourceFactory;

    @Mock
    private ParameterSet parameterSetMock;


    @Before
    public void setUp() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        MockitoAnnotations.initMocks(this);
        Constructor<AccessTokenFactory> constructor = AccessTokenFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        accessTokenFactorySpy = spy(constructor.newInstance());

        cachedResourceFactory = (CachedResourceFactory<Supplier<? extends AccessToken>>) CachedResourceFactory.create(accessTokenFactorySpy);
    }



    @Test
    public void  testAccessTokenCacheThreadSafety() throws InterruptedException, ExecutionException {
        // Arrange
        String mockScope = "urn:oracle:db::id::*";

        when(parameterSetMock.getRequired(AccessTokenFactory.SCOPE)).thenReturn(mockScope);
        when(parameterSetMock.getRequired(AuthenticationDetailsFactory.AUTHENTICATION_METHOD)).thenReturn(AuthenticationMethod.AUTO_DETECT);

        imitatiateRequestJwtToken();

        // Act
        ExecutorService executorService = Executors.newFixedThreadPool(36);

        CompletableFuture<AccessToken>[] futures = new CompletableFuture[36];
        for (int i = 0; i < 36; i++) {
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

        String mockScopeX = "scopeX";
        String mockScopeY ="scopeY";
        ParameterSet parameterSetMock2 = mock(ParameterSet.class);
        when(parameterSetMock.getRequired(AccessTokenFactory.SCOPE)).thenReturn(mockScopeX);
        when(parameterSetMock2.getRequired(AccessTokenFactory.SCOPE)).thenReturn(mockScopeY);

        when(parameterSetMock.getRequired(AuthenticationDetailsFactory.AUTHENTICATION_METHOD)).thenReturn(AuthenticationMethod.AUTO_DETECT);
        when(parameterSetMock2.getRequired(AuthenticationDetailsFactory.AUTHENTICATION_METHOD)).thenReturn(AuthenticationMethod.AUTO_DETECT);

        imitatiateRequestJwtToken();

        AccessToken tokenX = cachedResourceFactory.request(parameterSetMock).getContent().get();
        AccessToken tokenY = cachedResourceFactory.request(parameterSetMock2).getContent().get();

        Assert.assertNotSame("This two Access Tokens should be different  ",tokenX,tokenY);
    }


    @Test
    public void testTokenUniquenessWithMultipleScopesAndThreads() throws InterruptedException, ExecutionException {
        // create 16 parameter set each one  will have a 32 thread that have 100 iteration to request token
        int numParameterSet = 16;
        int numThreads = 32;
        int numIterations = 100;
        imitatiateRequestJwtToken();

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads*numParameterSet);

        Map<String,CompletableFuture<AccessToken>[]> completableFutureMap = new ConcurrentHashMap<>();
        for (int i=0;i<numParameterSet;i++){
            String scope = "scope"+i;
            CompletableFuture<AccessToken>[] futures =new CompletableFuture[numThreads];
            ParameterSet parameterSet = mock(ParameterSet.class);
            when(parameterSet.getRequired(AccessTokenFactory.SCOPE)).thenReturn(scope);
            when(parameterSet.getRequired(AuthenticationDetailsFactory.AUTHENTICATION_METHOD)).thenReturn(AuthenticationMethod.AUTO_DETECT);
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
            completableFutureMap.putIfAbsent(scope,futures);

        }

        // wait all futures to join
        for (CompletableFuture<AccessToken>[] futures : completableFutureMap.values()) {
            CompletableFuture.allOf(futures).join();
        }

        // test if all instances that have same parameter set  are same .
        for (int i = 0; i < numParameterSet; i++) {
            String scope = "scope"+i;
            CompletableFuture<AccessToken>[] parameterSetFutures = completableFutureMap.get(scope);
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



    private  String getJwtToken() {
        ZoneId timeZone = ZoneId.of("UTC");

        /* we create a token that will expire in 180s , 180s because the jdbc cache try to update the token a 1minute before token will expire
         *that why we need enough time when the token already expired and updated with new one
         */
        // Arrange
        Instant now = Instant.now();
        ZonedDateTime nowZoned = now.atZone(timeZone);

        String jwtToken = Jwts.builder()
                .claim("name", " ayoub")
                .claim("email", "alli@example.com")
                .setSubject("ayoub")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(nowZoned.toInstant()))
                .setExpiration(Date.from(nowZoned.toInstant().plus(180, ChronoUnit.SECONDS))) //the token will expire after 180 s .
                .compact();
        return jwtToken;
    }
    private void imitatiateRequestJwtToken() {
        doAnswer((Answer<String>) invocation -> {
            // imitate the behavior of getting new token from Oci .
            Thread.sleep(7000); // Delay for 7 seconds
            return getJwtToken();
        }).when(accessTokenFactorySpy).requestOciAccessToken(any(), anyString(),any());
    }



}


