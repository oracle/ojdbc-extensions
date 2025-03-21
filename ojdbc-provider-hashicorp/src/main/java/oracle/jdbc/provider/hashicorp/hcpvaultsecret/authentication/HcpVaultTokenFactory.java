/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication;

import oracle.jdbc.AccessToken;
import oracle.jdbc.driver.oauth.JsonWebToken;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication.HcpVaultSecretParameters.AUTHENTICATION_METHOD;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
/**
 * A factory for creating {@link HcpVaultSecretToken} objects for HCP Vault Secrets.
 * <p>
 * Implements the client_credentials flow as well as file-based authentication.
 * The auto-detect mode attempts file-based authentication first, then falls back
 * to client credentials.
 * </p>
 */
public final class HcpVaultTokenFactory implements ResourceFactory<HcpVaultSecretToken> {

  private static final HcpVaultTokenFactory INSTANCE = new HcpVaultTokenFactory();

  private static final ConcurrentHashMap<Map<String, Object>, Supplier<? extends AccessToken>> tokenCache =
          new ConcurrentHashMap<>();

  private HcpVaultTokenFactory() {}

  public static HcpVaultTokenFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<HcpVaultSecretToken> request(ParameterSet parameterSet) {
    HcpVaultSecretToken credentials = getCredential(parameterSet);
    return Resource.createPermanentResource(credentials, true);
  }

  /**
   * Determines the authentication method and retrieves credentials accordingly.
   *
   * @param parameterSet The parameter set containing authentication details.
   * @return The HCP Vault secret token.
   */
  private HcpVaultSecretToken getCredential(ParameterSet parameterSet) {
    HcpVaultAuthenticationMethod method = parameterSet.getRequired(AUTHENTICATION_METHOD);
    AbstractHcpVaultAuthentication authentication = getAuthentication(method);
    return createCachedToken(parameterSet, authentication);
  }

  /**
   * Creates or retrieves a cached {@link HcpVaultSecretToken} for the specified
   * authentication method.
   *
   * @param parameterSet the set of parameters for the request.
   * @param authentication the authentication method being used.
   * @return a {@code HcpVaultSecretToken} instance.
   */
  private HcpVaultSecretToken createCachedToken(
          ParameterSet parameterSet, AbstractHcpVaultAuthentication authentication) {

    Map<String, Object> cacheKey = authentication.generateCacheKey(parameterSet);

    Supplier<? extends AccessToken> tokenSupplier = tokenCache.computeIfAbsent(cacheKey, k -> AccessToken.createJsonWebTokenCache(() -> {
      HcpVaultSecretToken token = authentication.generateToken(parameterSet);
      return AccessToken.createJsonWebToken(token.getHcpApiToken().toCharArray());
    }));

    AccessToken cachedToken = tokenSupplier.get();
    JsonWebToken jwt = (JsonWebToken) cachedToken;
    return new HcpVaultSecretToken(jwt.token().get());
  }

  /**
   * Returns the appropriate authentication strategy for the specified method.
   *
   * @param method the authentication method
   * @return the corresponding {@link AbstractHcpVaultAuthentication} instance
   */
  private AbstractHcpVaultAuthentication getAuthentication(HcpVaultAuthenticationMethod method) {
    switch (method) {
      case CLIENT_CREDENTIALS:
        return ClientCredentialsAuthentication.INSTANCE;
      case CLI_CREDENTIALS_FILE:
        return CliCredentialsFileAuthentication.INSTANCE;
      case AUTO_DETECT:
        return AutoDetectAuthentication.INSTANCE;
      default:
        throw new IllegalArgumentException("Unsupported authentication method: " + method);
    }
  }

}
