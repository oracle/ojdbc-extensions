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

import oracle.jdbc.provider.parameter.UriParameters;
import oracle.jdbc.provider.resource.AbstractResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.spring.oauth.OAuth2AccessTokenFactory;
import oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters;
import oracle.jdbc.spi.EndUserSecurityContextProvider;
import oracle.sql.json.OracleJsonException;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonValue;
import oracle.sql.json.OracleJsonValue.OracleJsonType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import oracle.sql.json.OracleJsonObject;
import org.springframework.util.MultiValueMap;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.REGISTRATION_ID;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.REQUEST_PARAMETERS;

/**
 * <p>
 * Provider of an {@link EndUserSecurityContext} that integrates with Spring
 * Security. End user tokens are obtained from the
 * {@link SecurityContext} via {@link SecurityContextHolder}.
 * The holder must be configured a {@link SecurityContextHolderStrategy} that
 * allows the end user's {@link Authentication} to be accessed on the
 * thread which executes JDBC API calls.
 * </p><p>
 * The provider will be invoked by the Oracle JDBC driver whenever a database
 * round-trip happens. The context will be piggybacked to the database to
 * enforce data security.
 * </p><p>
 * This provider is enabled by configuring
 * {@linkplain oracle.jdbc.OracleConnection#CONNECTION_PROPERTY_PROVIDER_END_USER_SECURITY_CONTEXT oracle.jdbc.provider.endUserSecurityContext}
 * with the name of this provider:
 * </p><pre>
 *   oracle.jdbc.provider.endUserSecurityContext = ojdbc-provider-spring-end-user-security-context
 * </pre>
 */
public final class SpringSecurityContextProvider
  extends AbstractResourceProvider
  implements EndUserSecurityContextProvider {

  /**
   * Comma separated list of data roles for an end user.
   */
  private static final ResourceParameter DATA_ROLES_PARAMETER;

  /**
   * JSON object containing a fixed set of end user context attributes
   */
  private static final ResourceParameter END_USER_CONTEXT_ATTRIBUTE_PARAMETER;

  /**
   * Prefix for {@link GrantedAuthority} that should be recognized as a
   * DATA ROLE.
   */
  private static final ResourceParameter AUTHORITY_ROLE_PREFIX_PARAMETER;

  /**
   * Prefix for {@link GrantedAuthority} that should be recognized as an
   * END USER CONTEXT attributes.
   */
  private static final ResourceParameter AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER;

  /**
   * Explicit opt-in to forwarding the Spring authentication token to the database.
   */
  private static final ResourceParameter ENABLE_TOKEN_FORWARDING;

  private static final ResourceParameter[] PARAMETERS = {

    new ResourceParameter(
      "registrationId", REGISTRATION_ID),

     new ResourceParameter(
      "requestParameters", REQUEST_PARAMETERS, null,
       value ->
         MultiValueMap.fromMultiValue(
           UriParameters.parseMultiValueQuery(value))),

    DATA_ROLES_PARAMETER = new ResourceParameter(
      "dataRoles", null, false, false,
      (value, parameterSetBuilder) -> {}),

    END_USER_CONTEXT_ATTRIBUTE_PARAMETER = new ResourceParameter(
      "endUserContextAttributes", null, false, false,
      (value, parameterSetBuilder) -> {}),

    AUTHORITY_ROLE_PREFIX_PARAMETER = new ResourceParameter(
      "authorityRolePrefix", null, false, false,
      (value, parameterSetBuilder) -> {}),

    AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER = new ResourceParameter(
      "authorityAttributesPrefix", null, false, false,
      (value, parameterSetBuilder) -> {}),

    ENABLE_TOKEN_FORWARDING = new ResourceParameter(
      "enableTokenForwarding", "false", false, false,
      (value, parameterSetBuilder) -> {}),
  };

  /**
   * Factory for creating Oracle JSON passed to
   * {@link EndUserSecurityContext#withAttributes(Map)}.
   */
  private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();

  /**
   * This is Spring's built-in check to verify if an instance of
   * {@link Authentication} represents an authenticated client, and not a
   * placeholder for an unauthenticated client, such as
   * {@link AnonymousAuthenticationToken}
   */
  private static final AuthenticationTrustResolver TRUST_RESOLVER =
    new AuthenticationTrustResolverImpl();

  /**
   * This public no-arg constructor is required by
   * {@link java.util.ServiceLoader}
   */
  public SpringSecurityContextProvider() {
    super("spring", "end-user-security-context", PARAMETERS);
  }

  @Override
  public EndUserSecurityContext getEndUserSecurityContext(
    Map<Parameter, CharSequence> parameters) {

    Optional<Authentication> authentication =
      Optional.ofNullable(
          SecurityContextHolder.getContext().getAuthentication())
        .filter(TRUST_RESOLVER::isAuthenticated);

    return createEndUserSecurityContext(parameters, authentication)
      .map(context ->
        context.withDataRoles(
          getDataRoles(parameters, authentication)))
      .map(context ->
        context.withAttributes(
          getAttributes(parameters, authentication)))
      .orElse(null);
  }

  /**
   * <p>
   * Returns an <code>EndUserSecurityContext</code> that contains a database
   * access token, and may also contain an end user token.
   * </p><p>
   * This method may return an empty <code>Optional</code> if there is no
   * authenticated end user and this provider is configured to require one. The
   * requirement for an authenticated end user may arise from the OAuth 2 grant
   * type (token-exchange, jwt-bearer, etc), or from the
   * <code>enableTokenForwarding</code> parameter.
   * </p>
   * @param parameters Parameters that configure this provider. Not null, may
   * not contain null.
   *
   * @param authentication An <code>Optional</code> containing authentication
   * for the current end user, or an empty <code>Optional</code> if there is
   * none.
   */
  private Optional<EndUserSecurityContext> createEndUserSecurityContext(
    Map<Parameter, CharSequence> parameters,
    Optional<Authentication> authentication
  ) {
    if (
      Optional.ofNullable(
          parameters.get(ENABLE_TOKEN_FORWARDING))
        .map(CharSequence::toString)
        .filter("true"::equalsIgnoreCase)
        .isPresent()
    ) {

      Optional<String> endUserToken =
        authentication.filter(
            AbstractOAuth2TokenAuthenticationToken.class::isInstance)
          .map(AbstractOAuth2TokenAuthenticationToken.class::cast)
          .map(AbstractOAuth2TokenAuthenticationToken::getToken)
          .map(OAuth2Token::getTokenValue);

      if (endUserToken.isEmpty()) {
        return Optional.empty();
      }

      return getDatabaseAccessToken(parameters)
        .map(databaseAccessToken ->
          EndUserSecurityContext.createWithToken(
            databaseAccessToken,
            endUserToken.get()));
    }
    else {
      return getDatabaseAccessToken(parameters)
        .map(EndUserSecurityContext::createWithToken);
    }
  }

  /**
   * Requests an OAuth 2.0 access token that authorizes a Spring application to
   * access an Oracle Database.
   *
   * @param parameters Parameters that configure this provider. Not null, may
   * not contain null.
   *
   * @return A database access token. Not null.
   */
  private Optional<String> getDatabaseAccessToken(
    Map<Parameter, CharSequence> parameters) {

    return OAuth2ResourceParameters.addSpringProperties(
        parseParameterValues(parameters))
      .flatMap(parameterSet ->
        OAuth2AccessTokenFactory.getInstance()
          .request(parameterSet)
          .getContent())
      .map(OAuth2AccessToken::getTokenValue);
  }

  /**
   * Returns DATA ROLE names that this provider has been
   * configured to include in an <code>EndUserSecurityContext</code> provided
   * to Oracle JDBC.
   *
   * @param parameters Parameters that configure this provider. Not null, may
   * not contain null.
   *
   * @return DATA ROLE names. Not null, not containing null, may be empty.
   */
  private static Set<String> getDataRoles(
    Map<Parameter, CharSequence> parameters,
    Optional<Authentication> authentication
  ) {
    Set<String> dataRoles = getFixedDataRoles(parameters);

    if (authentication.isPresent()) {
      dataRoles = merge(
        dataRoles,
        getAuthorityPrefixDataRoles(
          parameters,
          authentication.get()));
    }

    return dataRoles;
  }

  /**
   * Returns a new Set that contains all entries from both input Sets.
   *
   * @param current A Set containing values. Not null.
   *
   * @param next A Set containing values. Not null.
   *
   * @return A Set containing values from both the current Set and next Set.
   * Not null, not containing null, may be empty.
   */
  private static <T> Set<T> merge(Set<T> current, Set<T> next) {

    if (current.isEmpty()) {
      return next;
    }

    if (next.isEmpty()) {
      return current;
    }

    Set<T> merged = new HashSet<>(current);
    merged.addAll(next);
    return merged;
  }

  /**
   * Returns DATA ROLE names configured by the {@link #DATA_ROLES_PARAMETER}.
   *
   * @param parameters Parameters that configure this provider. Not null, may
   * not contain null.
   *
   *
   * @return Set of DATA ROLEs to enable. Not null, may not contain null.
   */
  private static Set<String> getFixedDataRoles(
    Map<Parameter, CharSequence> parameters
  ) {
    CharSequence fixedDataRoles = parameters.get(DATA_ROLES_PARAMETER);

    if (fixedDataRoles == null) {
      return Collections.emptySet();
    }

    return Arrays.stream(fixedDataRoles.toString().split(","))
      .map(String::trim)
      .filter(dataRole -> !dataRole.isEmpty())
      .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Returns DATA ROLE names derived from {@link GrantedAuthority}
   * objects having a String representation that begins with a prefix
   * configured by {@link #AUTHORITY_ROLE_PREFIX_PARAMETER}.
   *
   * @param parameters Parameters that configure this provider. Not null, may
   * not contain null.
   *
   * @return Set of DATA ROLEs to enable. Not null, may not contain null.
   */
  private static Set<String> getAuthorityPrefixDataRoles(
    Map<Parameter, CharSequence> parameters,
    Authentication authentication
  ) {

    if (authentication == null) {
      return Collections.emptySet();
    }

    CharSequence prefix =
      parameters.get(AUTHORITY_ROLE_PREFIX_PARAMETER);

    if (prefix == null) {
      return Collections.emptySet();
    }

    return getPrefixedAuthorities(prefix.toString(), authentication);
  }

  /**
   * Returns END USER CONTEXT ATTRIBUTE values that this provider has been
   * configured to include in an <code>EndUserSecurityContext</code> provided
   * to Oracle JDBC.
   *
   * @param parameters Parameters that configure this provider. Not null, may
   * not contain null.
   *
   * @param authentication An <code>Optional</code> containing authentication
   * for the current end user, or an empty <code>Optional</code> if there is
   * none.
   *
   * @return END USER CONTEXT ATTRIBUTE values. Not null, not containing null,
   * may be empty.
   */
  private static Map<String, OracleJsonObject> getAttributes(
    Map<Parameter, CharSequence> parameters,
    Optional<Authentication> authentication
  ) {
    Map<String, OracleJsonObject> attributes = getFixedAttributes(parameters);

    if (authentication.isPresent()) {
      attributes =
        merge(
          attributes,
          getAuthorityPrefixAttributes(parameters, authentication.get()));
    }

    return attributes;
  }

  /**
   * Returns END USER CONTEXT attributes configured by the
   * {@link #END_USER_CONTEXT_ATTRIBUTE_PARAMETER}.
   *
   * @param parameters Parameters that configure this provider. Not null, may
   * not contain null.
   *
   * @return Map of END USER CONTEXT attributes to set. Not null, may not
   * contain null.
   */
  private static Map<String, OracleJsonObject> getFixedAttributes(
    Map<Parameter, CharSequence> parameters) {

    CharSequence fixedAttributes =
      parameters.get(END_USER_CONTEXT_ATTRIBUTE_PARAMETER);

    if (fixedAttributes == null || fixedAttributes.isEmpty()) {
      return Collections.emptyMap();
    }

    return parseAttributes(
      "attributes configured by the "
        + END_USER_CONTEXT_ATTRIBUTE_PARAMETER.name()
        + " parameter",
      fixedAttributes.toString());
  }

  /**
   * Returns END USER CONTEXT attributes derived from {@link GrantedAuthority}
   * objects having a String representation that begins with a prefix
   * configured by {@link #AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER}.
   *
   * @param parameters Parameters that configure this provider. Not null, may
   * not contain null.
   *
   * @return Map of END USER CONTEXT attributes to set. Not null, may not
   * contain null.
   */
  private static Map<String, OracleJsonObject> getAuthorityPrefixAttributes(
    Map<Parameter, CharSequence> parameters,
    Authentication authentication) {

    if (authentication == null) {
      return Collections.emptyMap();
    }

    CharSequence authorityPrefix =
      parameters.get(AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER);

    if (authorityPrefix == null || authorityPrefix.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<String> authorities =
      getPrefixedAuthorities(authorityPrefix.toString(), authentication);

    if (authorities.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, OracleJsonObject> attributes =
      new HashMap<>(authorities.size());

    for (String authority : authorities) {
      Map<String, OracleJsonObject> authorityAttributes =
        parseAttributes(
          "GrantedAuthority matching prefix configured by the "
            + AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER.name()
            + " parameter",
          authority);

      attributes = merge(attributes, authorityAttributes);
    }

    return attributes;
  }

  /**
   * Parses a JSON string as a Map of END USER CONTEXT attributes which can be
   * passed to {@link EndUserSecurityContext#withAttributes(Map)}. The structure
   * of the JSON object passed to this method should contain END USER CONTEXT
   * names associated to JSON objects that contain attributes values, as in:
   * <pre>
   *   {
   *     "schema.context_name" : {
   *       "string_attribute" : "value",
   *       "integer_attribute" : 1,
   *     },
   *     "scott.user_info" : {
   *       "first_name" : "George",
   *       "last_name" : "Washington"
   *     }
   *   }
   * </pre>
   *
   * @param name Name to use in error messages if the jsonString cannot be
   * parsed. Not null.
   *
   * @param jsonString String to parse. May be null.
   *
   * @return Map of END USER CONTEXT attributes, not null.
   */
  private static Map<String, OracleJsonObject> parseAttributes(
    String name,
    String jsonString) {

    if (jsonString == null || jsonString.isEmpty()) {
      return Collections.emptyMap();
    }

    final OracleJsonValue jsonValue;
    try {
      jsonValue =
        JSON_FACTORY.createJsonTextValue(new StringReader(jsonString));
    }
    catch (OracleJsonException oracleJsonException) {
      throw new IllegalArgumentException(
        "Failed to parse JSON from " + name,
        oracleJsonException);
    }

    OracleJsonObject jsonObject = requireJsonObject(name, jsonValue);

    HashMap<String, OracleJsonObject> attributes =
      new HashMap<>(jsonObject.size());

    for (Entry<String, OracleJsonValue> entry : jsonObject.entrySet()) {
      String contextName = entry.getKey();

      OracleJsonObject attributeValues =
        requireJsonObject(
          contextName + " within " + name,
          entry.getValue());

      attributes.put(contextName, attributeValues);
    }

    return attributes;
  }

  /**
   * Checks if an Oracle JSON value is a JSON object, and throws an
   * IllegalArgumentException if it is not.
   *
   * @param name Name to use an error message. Not null.
   * @param value Value to check. May be null.
   *
   * @return The value as an OracleJsonObject. Not null.
   */
  private static OracleJsonObject requireJsonObject(
    String name, OracleJsonValue value) {

    if (value == null) {
      throw new IllegalArgumentException("Value of \"" + name + "\" is null");
    }

    if (value.getOracleJsonType() != OracleJsonType.OBJECT) {
      throw new OracleJsonException(
        "Value of " + name + " is a " + value.getOracleJsonType()
          + ". A JSON object is required.");
    }

    return value.asJsonObject();
  }

  /**
   * Merges JSON objects contained with maps:
   * <pre>{@code
   *   Map<String, OracleJsonObject> merged = merge(
   *     Map.of("key", toJSON("{\"a\" : 0, \"b\" : 0}")),
   *     Map.of("key", toJSON("{\"b\" : 1, \"c\" : 1}")));
   *
   *   // This is true:
   *   merged.equals(
   *     Map.of("key", toJSON("{\"a\" : 0, \"b\" : 1, \"c\" : 1}")));
   * }</pre>
   *
   * @param existing Map that contains JSON values which can be overwritten by
   * the current map. May be null, but may not contain null.
   *
   * @param current Map that contains JSON values which can overwrite those in
   * the existing map. May be null, but may not contain null.
   *
   * @return Map that contains all values in the existing map, plus those in
   * the current Map. Not null, may not contain null.
   */
  private static Map<String, OracleJsonObject> merge(
    Map<String, OracleJsonObject> existing,
    Map<String, OracleJsonObject> current) {

    if (existing == null || existing.isEmpty()) {
      return current == null
        ? Collections.emptyMap()
        : current;
    }

    if (current == null || current.isEmpty()) {
      return existing;
    }

    HashMap<String, OracleJsonObject> merged = new HashMap<>(existing);

    for (Entry<String, OracleJsonObject> entry : current.entrySet()) {
      merged.merge(
        entry.getKey(), entry.getValue(), (existingJson, currentJson) -> {
          existingJson.putAll(currentJson);
          return existingJson;
        });
    }

    return merged;
  }

  /**
   * Returns the String representation of all {@link GrantedAuthority} objects
   * that begin with a given prefix. The returned Set contains String
   * representations with the prefix omitted.
   *
   * @param prefix Prefix to match. Not null.
   *
   * @param authentication Authentication with {@link GrantedAuthority} objects.
   * Not null.
   *
   * @return All matching authorities. Not null.
   */
  private static Set<String> getPrefixedAuthorities(
    String prefix,
    Authentication authentication) {

    Set<String> authorities = null;

    for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {

      if (grantedAuthority == null) {
        continue;
      }

      String authority = grantedAuthority.getAuthority();

      if (authority == null) {
        continue;
      }

      if (! authority.startsWith(prefix)) {
        continue;
      }

      authority = authority.substring(prefix.length()).trim();

      if (authority.isEmpty()) {
        continue;
      }

      if (authorities == null) {
        authorities = new HashSet<>();
      }

      authorities.add(authority);
    }

    return authorities == null
      ? Collections.emptySet()
      : authorities;
  }
}
