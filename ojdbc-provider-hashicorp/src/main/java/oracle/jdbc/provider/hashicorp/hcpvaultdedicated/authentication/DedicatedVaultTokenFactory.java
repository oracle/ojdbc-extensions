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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.AUTHENTICATION_METHOD;

/**
 * <p>
 * Factory for creating {@link DedicatedVaultToken} objects for authenticating
 * with Dedicated HashiCorp Vault.
 * </p><p>
 * This factory determines the appropriate authentication method based on the provided
 * {@link ParameterSet} and creates credentials accordingly.
 * </p>
 */
public final class DedicatedVaultTokenFactory
        implements ResourceFactory<DedicatedVaultToken> {

  // Map to cache tokens based on generated cache keys
  private static final ConcurrentHashMap<Map<String, Object>, CachedToken> tokenCache = new ConcurrentHashMap<>();

  private static final DedicatedVaultTokenFactory INSTANCE =
          new DedicatedVaultTokenFactory();

  private DedicatedVaultTokenFactory() {
  }

  /**
   * Returns a singleton instance of {@code DedicatedVaultCredentialsFactory}.
   *
   * @return a singleton instance. Not null.
   */
  public static DedicatedVaultTokenFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<DedicatedVaultToken> request(ParameterSet parameterSet) {
    DedicatedVaultToken credentials = getCredential(parameterSet);
    return Resource.createPermanentResource(credentials, true);
  }

  /**
   * Determines the appropriate credentials based on the provided parameters.
   *
   * @param parameterSet the set of parameters configuring the request. Must
   *                     not be null.
   * @return the created {@code DedicatedVaultToken} instance.
   */
  private static DedicatedVaultToken getCredential(ParameterSet parameterSet) {
    // Check which authentication method is requested
    DedicatedVaultAuthenticationMethod method =
            parameterSet.getRequired(AUTHENTICATION_METHOD);

    return createCachedToken(parameterSet, method);
  }

  /**
   * Creates or retrieves a cached {@link DedicatedVaultToken} for the specified
   * authentication method.
   *
   * @param parameterSet the set of parameters for the request.
   * @param method the authentication method being used.
   * @return a {@code DedicatedVaultToken} instance.
   */
  private static DedicatedVaultToken createCachedToken(
          ParameterSet parameterSet, DedicatedVaultAuthenticationMethod method) {
    Map<String, Object> cacheKey = method.generateCacheKey(parameterSet);

    CachedToken validCachedToken = tokenCache.compute(cacheKey, (k, cachedToken) -> {
      if (cachedToken == null || !cachedToken.isValid()) {
        return method.generateToken(parameterSet);
      }
      return cachedToken;
    });
    return new DedicatedVaultToken(validCachedToken.getToken().token().get());
  }

}
