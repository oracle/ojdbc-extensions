package oracle.jdbc.provider.oci.oaut;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import io.jsonwebtoken.Jwts;
import oracle.jdbc.provider.oci.oauth.AccessTokenFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.security.KeyPair;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccessTokenFactoryTest {

    @InjectMocks
    private AccessTokenFactory accessTokenFactorySpy;

    @Mock
    private ParameterSet parameterSetMock;

    @Mock
    private AbstractAuthenticationDetailsProvider authenticationDetailsMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        accessTokenFactorySpy = (AccessTokenFactory) spy(AccessTokenFactory.getInstance());
    }

    @Test
    public void testCreateAccessToken() throws InterruptedException {
        // Arrange
        String mockScope = "urn:oracle:db::id::*";

        when(parameterSetMock.getRequired(AccessTokenFactory.SCOPE)).thenReturn(mockScope);
        doAnswer((Answer<String>) invocation -> {
            // imitate the behavior of getting new token from OCI .
            Thread.sleep(7000); // Delay for 7 seconds
            return getJwtToken();
        }).when(accessTokenFactorySpy).requestOciAccessToken(any(), anyString(), any(KeyPair.class));

        // Act
        long start = System.currentTimeMillis();
        accessTokenFactorySpy.request( authenticationDetailsMock,parameterSetMock);
        long duration1 = (System.currentTimeMillis()-start);

        System.out.println("first request took like "+duration1);
        verify(accessTokenFactorySpy,times(1)).requestOciAccessToken(any(),anyString(),any());

        // here we wait till the 180s pass so we really make sure that the token expired and replace with new one
        Thread.sleep(180000);
        long start2 = System.currentTimeMillis();
        clearInvocations(accessTokenFactorySpy);
        accessTokenFactorySpy.request( authenticationDetailsMock,parameterSetMock);
        long duration2 = (System.currentTimeMillis()-start2);
        System.out.println("second request took like "+duration2);


        assertTrue(duration2<duration1, "Second request should be faster ");
        //verify
        verify(accessTokenFactorySpy,never()).requestOciAccessToken(any(),anyString(),any());


    }

    private static String getJwtToken() {
        ZoneId timeZone = ZoneId.of("UTC");
        /* we create a token that will expire in 180s , 180s because the jdbc cache try to update the token a 1minute before token will expire
        *that why we need enough time when the token already expired and updated with new one
        */

        // Arrange
        Instant now = Instant.now();
        ZonedDateTime nowZoned = now.atZone(timeZone);

        String jwtToken = Jwts.builder()
                .claim("name", "ayoub")
                .claim("email", "ali@example.com")
                .setSubject("ayoub")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(nowZoned.toInstant()))
                .setExpiration(Date.from(nowZoned.toInstant().plus(180, ChronoUnit.SECONDS))) //the token will expire after 180 s .
                .compact();
        return jwtToken;
    }



}


