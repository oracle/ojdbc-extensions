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

import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.AbstractResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.spring.oauth.OAuth2AccessTokenFactory;
import oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters;
import oracle.jdbc.spi.EndUserSecurityContextProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import oracle.sql.json.OracleJsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.REGISTRATION_ID;

/**
 * <p>
 * Provider of an {@link EndUserSecurityContext} that integrates with Spring
 * Security. End user tokens are obtained from the
 * {@link SecurityContext} via {@link SecurityContextHolder}.
 * The holder must be configured a {@link SecurityContextHolderStrategy} that
 * allows the end user's {@link Authentication} to be accessed on the
 * thread which executes JDBC API calls.
 * </p><p>
 * The provider will be invoked by Oracle JDBC driver whenever a database
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

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter(
      "registrationId", REGISTRATION_ID),

    DATA_ROLES_PARAMETER = new ResourceParameter(
      "dataRoles", null, false, false,
      (value, parameterSetBuilder) -> {}),

    END_USER_CONTEXT_ATTRIBUTE_PARAMETER = new ResourceParameter(
      "endUserContextAttribute", null, false, false,
      (value, parameterSetBuilder) -> {}),

    AUTHORITY_ROLE_PREFIX_PARAMETER = new ResourceParameter(
      "authorityRolePrefix", null, false, false,
      (value, parameterSetBuilder) -> {}),

    AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER = new ResourceParameter(
      "authorityAttributePrefix", null, false, false,
      (value, parameterSetBuilder) -> {}),
  };

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

    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();


    if (
      authentication == null
        || !authentication.isAuthenticated()
        || !(authentication instanceof AbstractOAuth2TokenAuthenticationToken<?>)
    ) {
      return null;
    }

    AbstractOAuth2TokenAuthenticationToken<?> userToken =
      (AbstractOAuth2TokenAuthenticationToken<?>)authentication;

    ParameterSet oauth2ParameterSet =
      OAuth2ResourceParameters.addSpringProperties(
        parseParameterValues(parameters));

    String databaseAccessToken =
      OAuth2AccessTokenFactory.getInstance()
        .request(oauth2ParameterSet)
        .getContent()
        .getTokenValue();

    EndUserSecurityContext endUserSecurityContext =
      EndUserSecurityContext.createWithToken(
        databaseAccessToken,
        userToken.getToken().getTokenValue());

    CharSequence commaSeparatedDataRoles = parameters.get(DATA_ROLES_PARAMETER);
    Set<String> dataRoles = commaSeparatedDataRoles == null
      ? Collections.emptySet()
      : Arrays.stream(commaSeparatedDataRoles.toString().split(","))
        .map(String::trim)
        .filter(dataRole -> !dataRole.isEmpty())
        .collect(Collectors.toSet());

    // Resolve prefixes (defaults) from provider parameters
    String rolePrefix = parameters.get(AUTHORITY_ROLE_PREFIX_PARAMETER) != null && parameters.get(AUTHORITY_ROLE_PREFIX_PARAMETER).length() > 0
      ? parameters.get(AUTHORITY_ROLE_PREFIX_PARAMETER).toString()
      : "ORACLE_DATA_ROLE_";
    String attrPrefix = parameters.get(AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER) != null && parameters.get(AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER).length() > 0
      ? parameters.get(AUTHORITY_ATTRIBUTE_PREFIX_PARAMETER).toString()
      : "ORACLE_CONTEXT_ATTRIBUTE_";

    // Extract roles from Authentication authorities (rolePrefix) and attributes (attrPrefix)
    Set<String> authorityRoles = authentication.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .filter(a -> a != null && a.startsWith(rolePrefix))
      .map(a -> a.substring(rolePrefix.length()).trim())
      .filter(s -> !s.isEmpty())
      .collect(Collectors.toSet());

    if (!authorityRoles.isEmpty()) {
      Set<String> totalRoles = java.util.stream.Stream.concat(
        dataRoles.stream(),
        authorityRoles.stream()
      ).collect(java.util.stream.Collectors.toSet());

      endUserSecurityContext = endUserSecurityContext.withDataRoles(totalRoles);
    } else {
      endUserSecurityContext = endUserSecurityContext.withDataRoles(dataRoles);
    }

    CharSequence namespaceJson = parameters.get(END_USER_CONTEXT_ATTRIBUTE_PARAMETER);
    Map<String, OracleJsonObject> appLevelAttributes = (namespaceJson != null && namespaceJson.length() > 0)
            ? ContextAttributesUtil.parseJson(namespaceJson.toString())
            : Collections.<String, OracleJsonObject>emptyMap();

    // Attributes: prefer those carried via authorities, else use configured/holder-resolved
    Map<String, OracleJsonObject> authorityAttrs = new HashMap<>();
    for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
      String authority = grantedAuthority.getAuthority();

      if (authority != null && authority.startsWith(attrPrefix)) {
        String json = authority.substring(attrPrefix.length());
        if (!json.isEmpty()) {
          Map<String, OracleJsonObject> attributes =
            ContextAttributesUtil.parseJson(json);
          authorityAttrs =
            ContextAttributesUtil.merge(authorityAttrs, attributes);
        }
      }
    }

    if (!authorityAttrs.isEmpty()) {
      endUserSecurityContext =
        endUserSecurityContext.withAttributes(
          ContextAttributesUtil.merge(appLevelAttributes, authorityAttrs));
    } else {
      endUserSecurityContext =
        endUserSecurityContext.withAttributes(appLevelAttributes);
    }

    return endUserSecurityContext;
  }

}
