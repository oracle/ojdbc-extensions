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

package oracle.jdbc.provider.spring.oauth;

import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JwtBearerOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.JwtBearerGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientJwtBearerTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;

import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.CLIENT_ID;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.CLIENT_SECRET;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.GRANT_TYPE;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.REGISTRATION_ID;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.SCOPE;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.TOKEN_URI;

/**
 * Abstract ResourceFactory designed for extension by subclasses that
 * request resources as an authorized OAuth 2.0 client. This ResourceFactory
 * integrates with the OAuth 2.0 API of Spring Security. It must be configured
 * to request resources with {@link Parameter}s defined in
 * {@link OAuth2ResourceParameters}.
 *
 * @param <T> The type of resource requested by a subclass of this factory.
 */
public abstract class OAuth2ResourceFactory<T> implements ResourceFactory<T> {

  /**
   * A cache of {@link OAuth2AuthorizedClientManager} instances. Each instance
   * of OAuth2AuthorizedClientManager caches resources for authorized OAUTH 2.0
   * clients. Instances are themselves cached by this field so that they can
   * be reused and repeated calls to authorization endpoints can be avoided.
   */
  private static final ResourceFactory<OAuth2AuthorizedClientManager>
    CLIENT_MANAGER_FACTORY =
      CachedResourceFactory.create(new ClientManagerFactory());

  @Override
  public final Resource<T> request(ParameterSet parameterSet)
    throws IllegalStateException, IllegalArgumentException {

    OAuth2AuthorizedClientManager clientManager =
      CLIENT_MANAGER_FACTORY.request(parameterSet).getContent();

    AuthorizationGrantType grantType = parameterSet.getRequired(GRANT_TYPE);

    OAuth2AuthorizeRequest request;
    if (AuthorizationGrantType.JWT_BEARER.equals(grantType))  {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
        throw new IllegalStateException("JWT_BEARER grant requested but no JwtAuthenticationToken found in SecurityContext");
      }

      request = OAuth2AuthorizeRequest.withClientRegistrationId(
                  parameterSet.getRequired(REGISTRATION_ID))
                .principal(jwtAuth)
                .build();
    } else {
      // For client credentials (or other client-only grants), a synthetic principal is sufficient
      request = OAuth2AuthorizeRequest.withClientRegistrationId(
                  parameterSet.getRequired(REGISTRATION_ID))
                .principal("client-credentials")
                .build();
    }

    OAuth2AuthorizedClient authorizedClient = clientManager.authorize(request);

    // The authorize method is specified to return null if the grant type of
    // client registration is not supported.
    if (authorizedClient == null) {
      String grantTypeStr = grantType.getValue();

      throw new IllegalStateException(
        "Unable to authorize as an OAUTH 2.0 client using " + grantTypeStr
          + " and the following parameters: " + parameterSet);
    }

    return request(authorizedClient, parameterSet);
  }

  /** Constructor that is declared only to clear a JavaDoc warning */
  protected OAuth2ResourceFactory() {}

  /**
   * Requests a resource as an authorized OAUTH 2.0 client.
   *
   * @param authorizedClient Authorized client. Not null.
   * @param parameterSet Set of parameters that configure the request. Not null.
   *
   * @return The requested resource. Not null.
   */
  protected abstract Resource<T> request(
    OAuth2AuthorizedClient authorizedClient, ParameterSet parameterSet);

  /**
   * <p>
   * A factory for instances of {@link OAuth2AuthorizedClientManager} that
   * are configured to request access tokens from an authorization endpoint.
   * This factory caches instances of
   * <code>OAuth2AuthorizedClientManager</code>, such that the same instance is
   * returned when {@link #request(ParameterSet)} is called with an identical
   * <code>ParameterSet</code>. Instances of
   * <code>OAuth2AuthorizedClientManager</code> then cache resources internally,
   * such that
   * {@link OAuth2AuthorizedClientManager#authorize(OAuth2AuthorizeRequest)}
   * may return same instance of {@link OAuth2AuthorizedClient}, without having
   * to repeatedly send requests to an authorization endpoint. This internal
   * ResourceFactory class is declared for this reason; So that instances of
   * OAuth2AuthorizedClientManager can themselves be cached and reused.
   * </p>
   */
  private static final class ClientManagerFactory
    implements ResourceFactory<OAuth2AuthorizedClientManager> {

    private ClientManagerFactory() {}

    @Override
    public Resource<OAuth2AuthorizedClientManager> request(
      ParameterSet parameterSet)
      throws IllegalStateException, IllegalArgumentException {

      String registrationId = parameterSet.getRequired(REGISTRATION_ID);

      ClientRegistration clientRegistration =
        ClientRegistration
          .withRegistrationId(registrationId)
          .tokenUri(parameterSet.getOptional(TOKEN_URI))
          .authorizationGrantType(parameterSet.getRequired(GRANT_TYPE))
          .clientId(parameterSet.getOptional(CLIENT_ID))
          .clientSecret(parameterSet.getOptional(CLIENT_SECRET))
          .scope(parameterSet.getOptional(SCOPE))
          .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
          .build();

      ClientRegistrationRepository clientRegistrationRepository =
         new InMemoryClientRegistrationRepository(clientRegistration);

      // Setup OAuth2AuthorizedClientService (token cache)
      OAuth2AuthorizedClientService authorizedClientService =
        new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);

      AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
              new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                      clientRegistrationRepository, authorizedClientService);

      AuthorizationGrantType grantType = parameterSet.getRequired(GRANT_TYPE);
      if (AuthorizationGrantType.JWT_BEARER.equals(grantType)) {
        // Configure On-Behalf-Of (OBO) with JWT Bearer grant
        JwtBearerOAuth2AuthorizedClientProvider provider = new JwtBearerOAuth2AuthorizedClientProvider();
        RestClientJwtBearerTokenResponseClient responseClient = new RestClientJwtBearerTokenResponseClient();
        responseClient.addParametersConverter((JwtBearerGrantRequest req) -> {
          LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
          params.add("requested_token_use", "on_behalf_of");
          return params;
        });
        provider.setAccessTokenResponseClient(responseClient);
        authorizedClientManager.setAuthorizedClientProvider(provider);
      } else {
        // Default to client credentials grant with 60s clock skew
        ClientCredentialsOAuth2AuthorizedClientProvider provider = new ClientCredentialsOAuth2AuthorizedClientProvider();
        authorizedClientManager.setAuthorizedClientProvider(provider);
      }
      return Resource.createPermanentResource(authorizedClientManager, false);
    }
  }
}
