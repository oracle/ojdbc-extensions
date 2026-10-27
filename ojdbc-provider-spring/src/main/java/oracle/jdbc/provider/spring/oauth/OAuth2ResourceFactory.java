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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JwtBearerOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.TokenExchangeOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.RestClientJwtBearerTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.RestClientTokenExchangeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.util.MultiValueMap;

import java.util.Optional;

import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.CLIENT_ID;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.CLIENT_SECRET;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.GRANT_TYPE;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.REGISTRATION_ID;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.REQUEST_PARAMETERS;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.SCOPE;
import static oracle.jdbc.provider.spring.oauth.OAuth2ResourceParameters.TOKEN_URI;

/**
 * <p>
 * Abstract ResourceFactory designed for extension by subclasses that
 * request resources as an authorized OAuth 2.0 client. This ResourceFactory
 * integrates with the OAuth 2.0 API of Spring Security. It must be configured
 * to request resources with {@link Parameter}s defined in
 * {@link OAuth2ResourceParameters}.
 * </p><p>
 * Subclasses should NOT be wrapped by
 * {@link oracle.jdbc.provider.cache.CachedResourceFactory}. This is because
 * the super-class {@link OAuth2ResourceFactory#request(ParameterSet)} method
 * may return a resource that depends on information from the
 * {@link SecurityContextHolder}, and this information may not be part of the
 * {@link ParameterSet} that is used as a cache key.
 * </p><p>
 * Instead of relying on <code>CachedResourceFactory</code>, this factory
 * relies on Spring's built-in cache, the
 * {@link InMemoryClientRegistrationRepository}.
 * </p>
 * @param <T> The type of resource requested by a subclass of this factory.
 */
public abstract class OAuth2ResourceFactory<T>
  implements ResourceFactory<Optional<T>> {

  /**
   * A factory for client managers that are configured by a
   * <code>ParameterSet</code>. Instances of
   * <code>OAuth2AuthorizedClientManager</code> are cached so that any
   * resources they retain may be reused across requests.
   */
  private static final ResourceFactory<OAuth2AuthorizedClientManager>
    CLIENT_MANAGER_FACTORY =
      CachedResourceFactory.create(new ClientManagerFactory());

  /**
   * A placeholder instance of Authentication that represents an unauthenticated
   * user in the SecurityContextHolder. This is useful for the client
   * credentials grant. Although no principal is required for this grant, an
   * <code>OAuth2AuthorizationRequest.Builder</code> requires one to be
   * configured. The construction of an AnonymousAuthenticationToken here is
   * replicating Spring's OAuth2ClientHttpRequestInterceptor.
   */
  private static final Authentication ANONYMOUS_AUTHENTICATION =
    new AnonymousAuthenticationToken(
      "anonymous",
      "anonymousUser",
      AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

  @Override
  public final Resource<Optional<T>> request(ParameterSet parameterSet)
    throws IllegalStateException, IllegalArgumentException {

    String registrationId = parameterSet.getRequired(REGISTRATION_ID);

    return Optional.ofNullable(
        SecurityContextHolder.getContext().getAuthentication())
      .or(() ->
        Optional.of(ANONYMOUS_AUTHENTICATION))
      .map(authentication ->
        // The authorize method may return null when the authentication is
        // not the right type of principal for a grant type. For instance, if
        // the authentication is not a Jwt and the grant type is JWT bearer,
        // then the authorize method returns null.
        CLIENT_MANAGER_FACTORY.request(parameterSet)
          .getContent()
          .authorize(
            OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
              .principal(authentication)
              .build()))
      .map(client ->
        Resource.map(
          request(client, parameterSet),
          Optional::of))
      .orElseGet(() ->
        Resource.createPermanentResource(
          Optional.empty(),
          false));

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

      ClientRegistration registration =
        ClientRegistration.withRegistrationId(
            parameterSet.getRequired(REGISTRATION_ID))
          .tokenUri(
            parameterSet.getOptional(TOKEN_URI))
          .authorizationGrantType(
            parameterSet.getRequired(GRANT_TYPE))
          .clientId(
            parameterSet.getOptional(CLIENT_ID))
          .clientSecret(
            parameterSet.getOptional(CLIENT_SECRET))
          .scope(
            parameterSet.getOptional(SCOPE))
          .clientAuthenticationMethod(
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
          .build();

      ClientRegistrationRepository repository =
        new InMemoryClientRegistrationRepository(registration);

      OAuth2AuthorizedClientService service =
        new InMemoryOAuth2AuthorizedClientService(repository);

      AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
          repository,
          service);

      manager.setAuthorizedClientProvider(createProvider(parameterSet));

      return Resource.createPermanentResource(manager, false);
    }
  }

  /**
   * Creates a provider configured by the grant type and request parameters
   * within a parameter set.
   *
   * @throws IllegalArgumentException If the grant type is not recognized.
   */
  private static OAuth2AuthorizedClientProvider createProvider(
    ParameterSet parameterSet
  ) {
    AuthorizationGrantType grantType =
      parameterSet.getRequired(GRANT_TYPE);

    MultiValueMap<String, String> requestParameters =
      parameterSet.getOptional(REQUEST_PARAMETERS);

    if (AuthorizationGrantType.JWT_BEARER.equals(grantType)) {
      JwtBearerOAuth2AuthorizedClientProvider jwtBearerProvider =
        new JwtBearerOAuth2AuthorizedClientProvider();

      if (requestParameters != null) {
        RestClientJwtBearerTokenResponseClient responseClient =
          new RestClientJwtBearerTokenResponseClient();
        responseClient.addParametersConverter(request -> requestParameters);
        jwtBearerProvider.setAccessTokenResponseClient(responseClient);
      }

      return jwtBearerProvider;
    }
    else if (AuthorizationGrantType.TOKEN_EXCHANGE.equals(grantType)) {
      TokenExchangeOAuth2AuthorizedClientProvider tokenExchangeProvider =
        new TokenExchangeOAuth2AuthorizedClientProvider();

      if (requestParameters != null) {
        RestClientTokenExchangeTokenResponseClient responseClient =
          new RestClientTokenExchangeTokenResponseClient();
        responseClient.addParametersConverter(request -> requestParameters);
        tokenExchangeProvider.setAccessTokenResponseClient(responseClient);
      }

      return tokenExchangeProvider;
    }
    else if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)) {
      ClientCredentialsOAuth2AuthorizedClientProvider clientCredentialsProvider =
        new ClientCredentialsOAuth2AuthorizedClientProvider();

      if (requestParameters != null) {
        RestClientClientCredentialsTokenResponseClient responseClient =
          new RestClientClientCredentialsTokenResponseClient();
        responseClient.addParametersConverter(request -> requestParameters);
        clientCredentialsProvider.setAccessTokenResponseClient(responseClient);
      }

      return clientCredentialsProvider;
    }
    else {
      throw new IllegalArgumentException(
        "Unrecognized grant type: " + grantType);
    }
  }

}
