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
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.StringReader;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
   * Optional parameter that configures END USER CONTEXT attributes
   */
  private static final Parameter ATTRIBUTES_PARAMETER =
    getParameter(PROVIDER, "endUserContextAttributes");

  /**
   * Optional parameter that configures a prefix for GrantedAuthority objects
   * that are mapped to data roles
   */
  private static final Parameter ROLE_PREFIX_PARAMETER =
    getParameter(PROVIDER, "authorityRolePrefix");

  /**
   * Optional parameter that configures a prefix for GrantedAuthority objects
   * that are mapped to END USER CONTEXT attributes.
   */
  private static final Parameter ATTRIBUTE_PREFIX_PARAMETER =
    getParameter(PROVIDER, "authorityAttributesPrefix");

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
   * Prefix for {@link GrantedAuthority} objects which this test will use when
   * verifying the {@link #ROLE_PREFIX_PARAMETER}
   */
  private static final String TEST_ROLE_PREFIX= "TEST_ROLE_";

  /**
   * Prefix for {@link GrantedAuthority} objects which this test will use when
   * verifying the {@link #ATTRIBUTE_PREFIX_PARAMETER}
   */
  private static final String TEST_ATTRIBUTE_PREFIX= "TEST_ATTRIBUTE_";

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
    configureSpringProperties(
      AuthorizationGrantType.CLIENT_CREDENTIALS.getValue(),
      AZURE_TOKEN_URI, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_SCOPE);
    test(AZURE_CLAIM_VERIFIER);
  }

  /**
   * Verifies the case where an application user is authenticated and the
   * provider is configured with the client ID and client secret of an OCI
   * application via Spring properties.
   */
  @Test
  public void testOciClientCredentialsSpringProperties() {
    configureSpringProperties(
      AuthorizationGrantType.CLIENT_CREDENTIALS.getValue(),
      OCI_TOKEN_URI, OCI_CLIENT_ID, OCI_CLIENT_SECRET, OCI_SCOPE);
    test(OCI_CLAIM_VERIFIER);
  }

  /**
   * Executes a series of tests with the given JWT claims verifier.
   *
   * @param claimVerifier Verifies the claims of the 
   * {@link EndUserSecurityContext#databaseAccessToken()}. Not null.
   */
  private void test(Consumer<Map<String,String>> claimVerifier) {

    test(
      claimVerifier,
      createUserAuthentication());

    test(
      claimVerifier,
      createUserAuthentication(
        new SimpleGrantedAuthority("ROLE_APP_NEGATIVE_TEST0"),
        new SimpleGrantedAuthority("ORACLE_DATA_ROLE_NEGATIVE_TEST1"),
        new SimpleGrantedAuthority(TEST_ROLE_PREFIX + "DATA_ROLE1"),
        new SimpleGrantedAuthority(TEST_ROLE_PREFIX + "DATA_ROLE2"),
        new SimpleGrantedAuthority(TEST_ROLE_PREFIX + "MERGED_ROLE"),
        new SimpleGrantedAuthority(TEST_ROLE_PREFIX), // NEGATIVE TEST
        new SimpleGrantedAuthority("_" + TEST_ROLE_PREFIX + "NEGATIVE_TEST_2")
      ));

    test(
      claimVerifier,
      createUserAuthentication(
        new SimpleGrantedAuthority("ATTRIBUTE_APP_NEGATIVE_TEST0"),
        new SimpleGrantedAuthority("ATTRIBUTE_{"
          + "\"a\" : {\"negative\" : \"test\"}"
          + "}"),
        new SimpleGrantedAuthority("ORACLE_CONTEXT_ATTRIBUTES_NEGATIVE_TEST1"),
        new SimpleGrantedAuthority(TEST_ATTRIBUTE_PREFIX + "{"
          + "  \"test\" : {"
          + "    \"a\" : 0,"
          + "    \"b\" : \"bee\""
          + "  }"
          + "}"),
        new SimpleGrantedAuthority(TEST_ATTRIBUTE_PREFIX + "{\n"
          // The intent of including escaped double-quotes in the JSON field
          // names is to verify that the provider will support quoted
          // identifiers, as in:
          //   CREATE USER "app_schema_1" ...
          //   CREATE END USER CONTEXT "test2" ...
          + "  \"\\\"app_schema_1\\\".test1\" : {\n"
          + "    \"a\" : 1.25,\n"
          + "    \"b\" : {\n"
          + "      \"b_1\" : \"bee\",\n"
          + "      \"b_2\" : \"bee" + Stream.generate(() -> "e").limit(4000).collect(Collectors.joining()) + "\"\n"
          + "    }\n"
          + "  },\n"
          + "  \"app_schema_2.\\\"test2\\\"\" : {\n"
          + "    \"c\" : {\n"
          + "      \"c_1\" : \"sea\",\n"
          + "      \"c_2\" : -0.0009\n"
          + "    },\n"
          + "    \"d\" : { }\n"
          + "  },\n"
          + "  \"merged_context\" : {\n"
          // The testAttributesParameter method creates a GrantedAuthority with
          // a "merged_context" JSON object, but the object does not contain a
          // "not_merged_1" value, so the value of 1 here should be included in
          // the EndUserSecurityContext.
          + "    \"not_merged_1\" : 1,"
          // The value of 1 here should overwrite by the value of 0 in the
          // testAttributesParameter method.
          + "    \"merged\" : 1"
          + "  }\n"
          + "}"),
        new SimpleGrantedAuthority(TEST_ATTRIBUTE_PREFIX), // NEGATIVE TEST
        new SimpleGrantedAuthority("_" + TEST_ATTRIBUTE_PREFIX + "NEGATIVE_TEST_2")
      ));
  }

  private void test(
    Consumer<Map<String,String>> claimVerifier,
    Authentication authentication) {

    SecurityContext securityContext =
      SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);

    SecurityContextHolder.setContext(securityContext);
    try {
      testBasic(claimVerifier);
      testRolesParameter(claimVerifier);
      testAttributesParameter(claimVerifier);
      testRolePrefixParameter(claimVerifier);
      testAttributePrefixParameter(claimVerifier);
    }
    finally {
      SecurityContextHolder.clearContext();
    }
  }

  private static void testBasic(
    Consumer<Map<String, String>> claimVerifier) {
    test(
      Collections.emptyMap(),
      claimVerifier);
  }

  private static void testRolesParameter(
    Consumer<Map<String, String>> claimVerifier
  ) {
    test(
      Map.of(ROLES_PARAMETER, "role1"),
      claimVerifier);

    test(
      Map.of(ROLES_PARAMETER, "role1,role2"),
      claimVerifier);

    test(
      Map.of(ROLES_PARAMETER, "role1, role2, MERGED_ROLE"),
      claimVerifier);
  }

  private static void testAttributesParameter(
    Consumer<Map<String, String>> claimVerifier
  ) {

    test(
      Map.of(ATTRIBUTES_PARAMETER, "{}"),
      claimVerifier);

    test(
      Map.of(ATTRIBUTES_PARAMETER, "{"
        + "  \"test\" : {"
        + "    \"a\" : 0,"
        + "    \"b\" : \"bee\""
        + "  }"
        + "}"),
      claimVerifier);

    test(
      Map.of(ATTRIBUTES_PARAMETER, "{\n"
        + "  \"app_schema.\\\"test1\\\"\" : {\n"
        + "    \"a\" : 1.25,\n"
        + "    \"b\" : {\n"
        + "      \"b_1\" : \"bee\",\n"
        + "      \"b_2\" : \"bee" + Stream.generate(() -> "e").limit(4000).collect(Collectors.joining()) + "\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"\\\"app_schema\\\".\\\"test2\\\"\" : {\n"
        + "    \"c\" : {\n"
        + "      \"c_1\" : \"sea\",\n"
        + "      \"c_2\" : -0.0009\n"
        + "    },\n"
        + "    \"d\" : { }\n"
        + "  }\n"
        + "}"),
      claimVerifier);
  }

  private static void testRolePrefixParameter(
    Consumer<Map<String, String>> claimVerifier
  ) {
    test(
      Map.of(ROLE_PREFIX_PARAMETER, TEST_ROLE_PREFIX),
      claimVerifier);
  }

  private static void testAttributePrefixParameter(
    Consumer<Map<String, String>> claimVerifier
  ) {
    test(
      Map.of(
        ATTRIBUTE_PREFIX_PARAMETER, TEST_ATTRIBUTE_PREFIX,
        ATTRIBUTES_PARAMETER, "{"
          + "  \"merged_context\" : {\n"
          // The test method creates a GrantedAuthority with a "merged_context"
          // JSON object, but the object does not contain a "not_merged_0" value,
          // so the value here should be included in the EndUserSecurityContext.
          + "    \"not_merged_0\" : 0,"
          // The GrantedAuthority also have a value for "merged", and it should
          // overwrite the value of 0 here.
          + "    \"merged\" : 0"
          + "  }"
          + "}"),
      claimVerifier);
  }

  /**
   * Invokes {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}
   * with the given parameters, and then verifies the provided
   * {@link EndUserSecurityContext}. This method will always set the required
   * {@link #REGISTRATION_ID_PARAMETER} parameter, if it is not set already.
   */
  private static void test(
    Map<Parameter, CharSequence> parameters,
    Consumer<Map<String, String>> claimVerifier) {

    if (! parameters.containsKey(REGISTRATION_ID_PARAMETER)) {
      parameters = new HashMap<>(parameters);
      parameters.put(REGISTRATION_ID_PARAMETER, TEST_REGISTRATION_ID);
    }

    verifyEndUserSecurityContext(
      parameters,
      PROVIDER.getEndUserSecurityContext(parameters),
      claimVerifier);
  }

  /**
   * Verifies that an {@link EndUserSecurityContext} from
   * {@link SpringSecurityContextProvider#getEndUserSecurityContext(Map)}
   * is correct for a given set of parameters.
   *
   * @param parameters Parameters passed to the provider. Not null.
   * @param endUserSecurityContext The end user security context provided for
   * the parameters. Not null.
   */
  private static void verifyEndUserSecurityContext(
    Map<Parameter, CharSequence> parameters,
    EndUserSecurityContext endUserSecurityContext,
    Consumer<Map<String,String>> claimVerifier) {

    verifyEndUserToken(endUserSecurityContext);
    verifyDatabaseAccessToken(endUserSecurityContext, claimVerifier);
    verifyDataRoles(parameters, endUserSecurityContext);
    verifyAttributes(parameters, endUserSecurityContext);
  }

  /**
   * Verifies that a provided EndUserSecurityContext has an end user token
   * present in the {@link SecurityContextHolder}
   */
  private static void verifyEndUserToken(EndUserSecurityContext endUserSecurityContext) {
    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();
    final char[] endUserToken;
    if (
      authentication instanceof AbstractOAuth2TokenAuthenticationToken<?> token
    ) {
      endUserToken = token.getToken().getTokenValue().toCharArray();
    }
    else {
      throw new IllegalStateException("Unrecognized: " + authentication);
    }

    assertArrayEquals(
      endUserToken,
      endUserSecurityContext.endUserToken().get());
  }

  /**
   * Verifies that a provided EndUserSecurityContext has a database access token
   * with the expected claims.
   */
  private static void verifyDatabaseAccessToken(
    EndUserSecurityContext endUserSecurityContext,
    Consumer<Map<String, String>> claimVerifier
  ) {
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
   * Verifies that a provided EndUserSecurityContext has data roles configured
   * both by the dataRoles parameter, and matching the authorityRolePrefix
   * of GrantedAuthority objects present in the {@link SecurityContextHolder}.
   */
  private static void verifyDataRoles(
    Map<Parameter, CharSequence> parameters,
    EndUserSecurityContext endUserSecurityContext) {

    Set<String> expectedDataRoles = new HashSet<>();

    CharSequence fixedDataRoles = parameters.get(ROLES_PARAMETER);
    if (fixedDataRoles != null) {
      Collections.addAll(expectedDataRoles, fixedDataRoles.toString().split(","));
    }

    expectedDataRoles.addAll(
      matchAuthorityPrefix(parameters.get(ROLE_PREFIX_PARAMETER)));

    expectedDataRoles =
      expectedDataRoles.stream()
        .map(String::trim)
        .filter(role -> !role.isEmpty())
        .collect(Collectors.toSet());

    assertEquals(
      expectedDataRoles,
      endUserSecurityContext.dataRoles());
  }

  /**
   * Verifies that a provided EndUserSecurityContext has END USER CONTEXT
   * attributes configured both by the endUserContextAttributes parameter, and
   * matching the authorityAttributesPrefix of GrantedAuthority objects present
   * in the {@link SecurityContextHolder}.
   */
  private static void verifyAttributes(
    Map<Parameter, CharSequence> parameters,
    EndUserSecurityContext endUserSecurityContext) {

    OracleJsonFactory jsonFactory = new OracleJsonFactory();
    Map<String, OracleJsonObject> expectedAttributes = new HashMap<>();

    CharSequence fixedAttributes = parameters.get(ATTRIBUTES_PARAMETER);
    if (fixedAttributes != null) {
      jsonFactory.createJsonTextValue(
        new StringReader(fixedAttributes.toString()))
        .asJsonObject()
        .forEach((contextName, attributes) ->
          expectedAttributes.put(
            contextName,
            attributes.asJsonObject()));
    }

    matchAuthorityPrefix(parameters.get(ATTRIBUTE_PREFIX_PARAMETER))
      .stream()
      .map(StringReader::new)
      .map(jsonFactory::createJsonTextValue)
      .map(OracleJsonValue::asJsonObject)
      .forEach(attributesObject ->
        attributesObject.forEach((contextName, attributes) ->
          expectedAttributes.merge(
            contextName,
            attributes.asJsonObject(),
            (existing, current) -> {
              existing.putAll(current);
              return existing;
            })));

    assertEquals(
      expectedAttributes,
      endUserSecurityContext.attributes());
  }

  /**
   * Returns the String representation of all GrantedAuthority objects present
   * in the {@link SecurityContextHolder} which match a given prefix.
   * @param prefix Prefix to match. May be null.
   * @return All matching authorities. Not null. May be empty.
   */
  private static Set<String> matchAuthorityPrefix(CharSequence prefix) {

    if (prefix == null) {
      return Collections.emptySet();
    }

    return SecurityContextHolder.getContext()
      .getAuthentication()
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .filter(Objects::nonNull)
      .filter(authorityString ->
        authorityString.length() > prefix.length())
      .filter(authorityString ->
        authorityString.startsWith(prefix.toString()))
      .map(authorityString ->
        authorityString.substring(prefix.length()))
      .collect(Collectors.toSet());
  }

  /**
   * Returns an Authentication object representing an authenticated application
   * user. Tests that verify cases where an application user is authenticated
   * can set this Authentication with a
   * {@link SecurityContext#setAuthentication(Authentication)}.
   *
   * @return Authentication representing an authenticated application user.
   * Not null.
   */
  private static JwtAuthenticationToken createUserAuthentication(
    GrantedAuthority... grantedAuthorities
  ) {
    Jwt jwt = createJwt();
    JwtAuthenticationToken jwtAuthenticationToken =
      new JwtAuthenticationToken(jwt, Arrays.asList(grantedAuthorities));
    jwtAuthenticationToken.setAuthenticated(true);
    return jwtAuthenticationToken;
  }

  /**
   * Creates a fake JWT with just enough claims to get past basic validations:
   * <ul><li>
   * The exp claim is checked when
   * {@link EndUserSecurityContext#createWithToken(CharSequence, CharSequence)}
   * is called.
   * </li><li>
   * The "alg" and "sub" claims are required when {@link Jwt.Builder#build()}
   * is called.
   * </li></ul>
   * @return A fake JWT. Not null.
   */
  private static Jwt createJwt() {

    String alg = "HS256";
    String sub = "yellow";
    long exp = 3600 + (System.currentTimeMillis() / 1000);

    Base64.Encoder base64Encoder = Base64.getEncoder();
    String jwtString =
      base64Encoder.encodeToString(
        ("{"
          + "\"typ\" : \"JWT\","
          + "\"alg\" : \"" + alg + "\","
          + "}")
          .getBytes(StandardCharsets.US_ASCII))
        + "."
        + base64Encoder.encodeToString(
        ("{"
          + "\"sub\" : \"" + sub + "\","
          + "\"exp\" : " + exp
          + "}")
          .getBytes(StandardCharsets.US_ASCII))
        + ".dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    Jwt.Builder jwtBuilder =
      Jwt.withTokenValue(jwtString)
        .header("alg", alg)
        .claim("sub", sub);

    return jwtBuilder.build();
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
   * Configures OAuth 2.0 client registration and client provider parameters
   * in the environment of an {@link ApplicationContext}.
   * </p>
   * @param grantType Value of an {@link AuthorizationGrantType} to configure
   * for the OAuth 2.0 client. Not null.
   * @param testProperties Test properties that configure an OAuth 2.0 client
   * registration.
   */
  private static void configureSpringProperties(
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
      String springPropertyName =
        property.toSpringProperty(TEST_REGISTRATION_ID);

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
