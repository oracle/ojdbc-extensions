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

package oracle.jdbc.provider.spring.resource;

import oracle.jdbc.EndUserSecurityContext;
import oracle.jdbc.provider.resource.ResourceProviderTestUtil;
import oracle.jdbc.provider.spring.SpringTestProperty;
import oracle.jdbc.provider.spring.context.ApplicationContextHolder;
import oracle.jdbc.provider.util.JsonWebTokenParser;
import oracle.jdbc.spi.EndUserSecurityContextProvider;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static oracle.jdbc.provider.TestProperties.getOptional;
import static oracle.jdbc.provider.TestProperties.getOrAbort;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.getParameter;
import static oracle.jdbc.provider.spring.SpringTestProperty.AZURE_CLIENT_ID;
import static oracle.jdbc.provider.spring.SpringTestProperty.AZURE_CLIENT_SECRET;
import static oracle.jdbc.provider.spring.SpringTestProperty.AZURE_SCOPE;
import static oracle.jdbc.provider.spring.SpringTestProperty.AZURE_TOKEN_URI;
import static oracle.jdbc.provider.spring.SpringTestProperty.OCI_CLIENT_ID;
import static oracle.jdbc.provider.spring.SpringTestProperty.OCI_CLIENT_SECRET;
import static oracle.jdbc.provider.spring.SpringTestProperty.OCI_SCOPE;
import static oracle.jdbc.provider.spring.SpringTestProperty.OCI_TOKEN_URI;
import static oracle.jdbc.provider.spring.SpringTestProperty.OCI_USER_OCID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringSecurityContextProviderTest {

  private static final EndUserSecurityContextProvider PROVIDER =
    ResourceProviderTestUtil.findProvider(
      EndUserSecurityContextProvider.class,
      "ojdbc-provider-spring-end-user-security-context");

  /**
   * Parameter that configures an OAuth 2.0 client registration ID that may or
   * may not be associated to Spring properties from the Environment of an
   * ApplicationContext.
   */
  private static final Parameter REGISTRATION_ID_PARAMETER =
    getParameter(PROVIDER, "registrationId");

  /**
   * Optional parameter that configures the names of data roles
   */
  private static final Parameter ROLES_PARAMETER =
    getParameter(PROVIDER, "dataRoles");


  /**
   * Value set as the {@link #REGISTRATION_ID_PARAMETER}. Repeated use of the
   * same registration ID is deliberate: Tests will catch the provider
   * if it uses a cached OAuth2ClientRegistration when it shouldn't. One test
   * may use Azure, another OCi. Both will pass "test" to the provider as the
   * registration ID. The provider should either detect that the Environment
   * properties of an ApplicationContext have changed, or simply that parameters
   * such as "clientId" have changed in the Map passed to
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}.
  */
  private static final String TEST_REGISTRATION_ID = "test";


  /**
   * Verifies the claims of a database access token issued by Azure.
   */
  private static final Consumer<Map<String,String>> AZURE_CLAIM_VERIFIER =
    claims -> {
      String iss = claims.get("iss");
      assertNotNull(iss, "iss claim is not present");
      assertTrue(iss.startsWith("https://login.microsoftonline.com/"), iss);

      String azp = claims.get("azp");
      assertNotNull(azp, "azp claim is not present");
      assertEquals(getOrAbort(AZURE_CLIENT_ID), azp);
    };

  /**
   * Verifies the claims of a database access token issued by OCI.
   */
  private static final Consumer<Map<String,String>> OCI_CLAIM_VERIFIER =
    claims -> {
      String iss = claims.get("iss");
      assertNotNull(iss, "iss claim is not present");
      // The issuer should https://identity.oraclecloud.com by default, or
      // https://{domain-ID}.identity.oraclecloud.com as a non-default.
      assertTrue(
        URI.create(iss).getHost().endsWith(".identity.oraclecloud.com"), iss);

      String sub = claims.get("sub");
      assertNotNull(sub, "sub claim is not present");
      assertEquals(getOrAbort(OCI_CLIENT_ID), sub);
    };

  /**
   * Verifies that {@link SpringSecurityContextProvider} is identified by the
   * name used to initialize the {@link #PROVIDER} field of this class.
   */
  @Test
  public void testProviderName() {
    assertInstanceOf(SpringSecurityContextProvider.class, PROVIDER);
  }

  /**
   * Verifies the case in which {@link SecurityContextHolder#getContext()}
   * returns null. In this case
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}
   * is expected to return null.
   */
  @Test
  public void testNoSecurityContext() {
    // Expect null if SecurityContextHolder is cleared
    SecurityContextHolder.clearContext();
    assertNullEndUserSecurityContext();
  }

  /**
   * Verifies the case in which {@link SecurityContextHolder#getContext()}
   * returns non-null, but {@link SecurityContext#getAuthentication()} returns
   * null. In this case
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}
   * is expected to return null.
   */
  @Test
  public void testNoAuthentication() {
    // Expect null if SecurityContextHolder has an empty SecurityContext
    SecurityContextHolder.setContext(
      SecurityContextHolder.createEmptyContext());
    try {
      assertNullEndUserSecurityContext();
    }
    finally {
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * Verifies the case in which {@link SecurityContextHolder#getContext()}
   * returns non-null, and {@link SecurityContext#getAuthentication()} returns
   * non-null, but the Authentication is not a class which the provider
   * recognizes. In this case
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}
   * is expected to return null.
   */
  @Test
  public void testUnsupportedAuthentication() {
    // Expect null if SecurityContextHolder has a SecurityContext with an
    // Authentication class that the provider does not recognize.
    UnsupportedAuthentication unsupportedAuthentication =
      new UnsupportedAuthentication();
    SecurityContext securityContext =
      SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(unsupportedAuthentication);
    try {
      assertNullEndUserSecurityContext();
    }
    finally {
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * Verifies the case in which {@link SecurityContextHolder#getContext()}
   * returns non-null, and {@link SecurityContext#getAuthentication()} returns
   * non-null, and the Authentication is a class which the provider recognizes,
   * but {@link Authentication#isAuthenticated()} returns false. In this case
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}
   * is expected to return null.
   */
  @Test
  public void testNotAuthenticated() {
    // Expect null if SecurityContextHolder has a SecurityContext with a
    // supported Authentication where isAuthenticated() is false
    JwtAuthenticationToken userAuthentication = createUserAuthentication();
    userAuthentication.setAuthenticated(false);

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(userAuthentication);
    SecurityContextHolder.setContext(securityContext);
    try {
      assertNullEndUserSecurityContext();
    }
    finally {
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * Verifies the case where an application user is authenticated and the
   * provider is configured with the client ID and client secret of an Azure
   * application via Spring properties.
   */
  @Test
  public void testAzureClientCredentialsSpringProperties() {
    Map<Parameter, CharSequence> springPropertyParameters =
      getSpringPropertyParameters(
        AuthorizationGrantType.CLIENT_CREDENTIALS.getValue(),
        AZURE_TOKEN_URI, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_SCOPE);
    verifyAuthenticatedUser(springPropertyParameters, AZURE_CLAIM_VERIFIER);
  }

  /**
   * Verifies the case where an application user is authenticated and the
   * provider is configured with the client ID and client secret of an OCI
   * application via Spring properties.
   */
  @Test
  public void testOciClientCredentialsSpringProperties() {
    Map<Parameter, CharSequence> springPropertyParameters =
      getSpringPropertyParameters(
        AuthorizationGrantType.CLIENT_CREDENTIALS.getValue(),
        OCI_TOKEN_URI, OCI_CLIENT_ID, OCI_CLIENT_SECRET, OCI_SCOPE);
    verifyAuthenticatedUser(springPropertyParameters, OCI_CLAIM_VERIFIER);
  }

  /**
   * Verifies the case where an application user is authenticated and
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)} is
   * invoked with a given set of parameters when. Test methods can configure the
   * parameters to use a specific grant type and authorization endpoint. This
   * method may invoke getEndUserSecurityContext multiple times to verify
   * handling of parameters such as
   * {@link #ROLES_PARAMETER} which do not influence OAUTH 2.0.
   *
   * @param parameters Parameter for the provider. Not null.
   */
  private void verifyAuthenticatedUser(
    Map<Parameter, CharSequence> parameters,
    Consumer<Map<String,String>> claimVerifier) {

    JwtAuthenticationToken userAuthentication = createUserAuthentication();
    SecurityContext securityContext =
      SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(userAuthentication);

    SecurityContextHolder.setContext(securityContext);
    try {
      parameters = new HashMap<>(parameters);
//      verifyEndUserSecurityContext(
//        userAuthentication,
//        parameters,
//        PROVIDER.getEndUserSecurityContext(parameters),
//        claimVerifier);

      parameters.put(ROLES_PARAMETER, "role1");
      verifyEndUserSecurityContext(
        userAuthentication,
        parameters,
        PROVIDER.getEndUserSecurityContext(parameters),
        claimVerifier);

      parameters.put(ROLES_PARAMETER, "role1,role2");
      verifyEndUserSecurityContext(
        userAuthentication,
        parameters,
        PROVIDER.getEndUserSecurityContext(parameters),
        claimVerifier);

      parameters.put(ROLES_PARAMETER, "role1, role2, role3");
      verifyEndUserSecurityContext(
        userAuthentication,
        parameters,
        PROVIDER.getEndUserSecurityContext(parameters),
        claimVerifier);
    }
    finally {
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * Verifies that an {@link EndUserSecurityContext} from
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}
   * is correct for a given set of parameters.
   *
   * @param endUserToken Authentication set on the {@link SecurityContext} when
   * the end user security context was provided. Not null.
   * @param parameters Parameters passed to the provider. Not null.
   * @param endUserSecurityContext The end user security context provided for
   * the parameters. Not null.
   */
  private void verifyEndUserSecurityContext(
    JwtAuthenticationToken endUserToken,
    Map<Parameter, CharSequence> parameters,
    EndUserSecurityContext endUserSecurityContext,
    Consumer<Map<String,String>> claimVerifier) {

    assertArrayEquals(
      endUserToken.getToken().getTokenValue().toCharArray(),
      endUserSecurityContext.endUserToken().get());

    Set<String> dataRoles = new HashSet<>();

    CharSequence sessionRoles = parameters.get(ROLES_PARAMETER);
    if (sessionRoles != null) {
      Collections.addAll(dataRoles, sessionRoles.toString().split(","));
    }

    dataRoles =
      dataRoles.stream()
        .map(String::trim)
        .collect(Collectors.toSet());
    assertEquals(dataRoles, endUserSecurityContext.dataRoles());

    char[] databaseAccessToken =
      endUserSecurityContext.databaseAccessToken();
    assertNotNull(databaseAccessToken);

    Map<String,String> claims =
      JsonWebTokenParser.parseClaims(CharBuffer.wrap(databaseAccessToken));
    claimVerifier.accept(claims);
    int exp = Integer.parseInt(claims.get("exp"));
    long now = System.currentTimeMillis() / 1000;
    assertTrue(exp >= now, exp + " < " + now);
  }

  /**
   * Returns an Authentication object representing an authenticated application
   * user. Tests that verify cases where an application user is authenticated
   * can set this Authentication with a
   * {@link SecurityContext#setAuthentication(Authentication)}.
   *
   * @return Authentication representing an authenticated application user.
   * Not null.
   *
   * @implNote This method creates a fake JWT with just enough claims to get
   * past basic validations:
   * The exp claim is checked when
   * {@link EndUserSecurityContext#createWithToken(CharSequence, CharSequence)} is
   * called.
   * The alg and sub claims are required when {@link Jwt.Builder#build()}
   * is called.
   * The userOcid claim is required when using OCI as an identity provider
   */
  private static JwtAuthenticationToken createUserAuthentication() {
    String alg = "HS256";
    String sub = "yellow";
    long exp = 3600 + (System.currentTimeMillis() / 1000);
    String userOcid = getOptional(OCI_USER_OCID);

    Base64.Encoder base64Encoder = Base64.getEncoder();
    String jwtString =
      base64Encoder.encodeToString(
        ("{"
          + "\"typ\" : \"JWT\","
          + "\"alg\" : \"" + alg  + "\","
          + "}")
          .getBytes(StandardCharsets.US_ASCII))
        + "."
        + base64Encoder.encodeToString(
          ("{"
            + "\"sub\" : \"" + sub + "\","
            + "\"exp\" : " + exp + ","
            + (userOcid != null
              ? "\"userOcid\" : \"" + userOcid + "\""
              : "")
            + "}")
            .getBytes(StandardCharsets.US_ASCII))
        + ".dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    Jwt.Builder jwtBuilder =
      Jwt.withTokenValue(jwtString)
        .header("alg", alg)
        .claim("sub", sub);

    if (userOcid != null) {
      jwtBuilder.claim("userOcid", userOcid);
    }

    Jwt jwt = jwtBuilder.build();
    JwtAuthenticationToken jwtAuthenticationToken =
      new JwtAuthenticationToken(jwt);
    jwtAuthenticationToken.setAuthenticated(true);
    return jwtAuthenticationToken;
  }

  /**
   * Asserts that
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}
   * returns null. This is to verify cases where the
   * {@link SecurityContextHolder} has been set up in a way where it won't
   * provide information about an authenticated application user.
   */
  private static void assertNullEndUserSecurityContext() {
    Map<Parameter, CharSequence> parameters =
      Map.of(REGISTRATION_ID_PARAMETER, "assertNullEndUserSecurityContext");
    EndUserSecurityContext applicationSecurityContext =
      PROVIDER.getEndUserSecurityContext(parameters);

    assertNull(applicationSecurityContext);
  }

  /**
   * <p>
   * Returns provider parameters for test cases that configure OAuth 2.0 client
   * registration properties using the environment of an
   * {@link ApplicationContext}. The client registration is configured values
   * from of a given set of SpringTestProperties. A test will be aborted if any
   * of those test properties are not configured. This method will ignore any
   * test property that doesn't have a {@link SpringTestProperty#springName()}.
   * </p>
   * @param grantType Value of an {@link AuthorizationGrantType} to configure
   * for the OAuth 2.0 client. Not null.
   * @param testProperties Test properties that configure an OAuth 2.0 client
   * registration.
   *
   * @return Parameters that can be passed to
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}. Not
   * null.
   */
  private static Map<Parameter, CharSequence> getSpringPropertyParameters(
    String grantType,
    SpringTestProperty... testProperties) {

    Map<String, Object> springProperties = new HashMap<>();

    String registrationPrefix =
      SpringTestProperty.SpringPropertyType.REGISTRATION.prefix()
        + TEST_REGISTRATION_ID;

    springProperties.put(
      registrationPrefix + ".provider",
      TEST_REGISTRATION_ID);

    springProperties.put(
      registrationPrefix + ".authorization-grant-type",
      grantType);

    for (SpringTestProperty property : testProperties) {
      String springPropertyName = property.toSpringProperty(TEST_REGISTRATION_ID);

      if (springPropertyName == null) {
        continue;
      }

      springProperties.put(springPropertyName, getOrAbort(property));
    }

    StandardEnvironment environment = new StandardEnvironment();
    environment
      .getPropertySources()
      .addFirst(new MapPropertySource("test-properties", springProperties));

    GenericApplicationContext applicationContext =
      new GenericApplicationContext();
    applicationContext.setEnvironment(environment);
    applicationContext.addApplicationListener(new ApplicationContextHolder());
    applicationContext.refresh();

    Map<Parameter, CharSequence> parameters = new HashMap<>();
    parameters.put(REGISTRATION_ID_PARAMETER, TEST_REGISTRATION_ID);
    return parameters;
  }

  /**
   * Pseudo-Implementation of Authentication that the provider will not recognize.
   */
  private static final class UnsupportedAuthentication
    implements Authentication {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of();
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getDetails() {
      return null;
    }

    @Override
    public Object getPrincipal() {
      return null;
    }

    @Override
    public boolean isAuthenticated() {
      return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated)
      throws IllegalArgumentException {

    }

    @Override
    public String getName() {
      return "Unsupported";
    }
  }
}
