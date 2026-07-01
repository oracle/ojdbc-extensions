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
import oracle.jdbc.provider.hashicorp.util.VaultEndpointPolicy;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.AUTHENTICATION_METHOD;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.GITHUB_TOKEN;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.PASSWORD;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.PARAM_GITHUB_TOKEN;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.PARAM_VAULT_PASSWORD;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.PARAM_VAULT_SECRET_ID;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.PARAM_VAULT_TOKEN;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.SECRET_ID;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.VAULT_ADDR;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.VAULT_TOKEN;

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

  private static final String KEY_TOKEN_CACHE_MAX_ENTRIES =
          "TOKEN_CACHE_MAX_ENTRIES";

  private static final int MAX_CACHE_ENTRIES =
          Math.max(1, getConfiguredCacheMaxEntries());

  private static final Set<String> SENSITIVE_CACHE_KEY_FIELDS = Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(
                  PARAM_VAULT_TOKEN, PARAM_VAULT_PASSWORD, PARAM_VAULT_SECRET_ID, PARAM_GITHUB_TOKEN)));

  private static final Set<Parameter<?>> SENSITIVE_CREDENTIAL_PARAMETERS = Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(
                  VAULT_TOKEN, PASSWORD, SECRET_ID, GITHUB_TOKEN)));

  private static final ReentrantLock TOKEN_CACHE_LOCK = new ReentrantLock();
  private static final LinkedHashMap<TokenCacheKey, CachedToken> tokenCache = new LinkedHashMap<>();

  private static final DedicatedVaultTokenFactory INSTANCE =
          new DedicatedVaultTokenFactory();

  private DedicatedVaultTokenFactory() {
  }

  /**
   * Returns a singleton instance of {@code DedicatedVaultTokenFactory}.
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
   * not be null.
   * @return the created {@code DedicatedVaultToken} instance.
   */
  private static DedicatedVaultToken getCredential(ParameterSet parameterSet) {
    DedicatedVaultAuthenticationMethod method = parameterSet.getRequired(AUTHENTICATION_METHOD);
    AbstractDedicatedVaultAuthentication authentication = getAuthenticationInstance(method);
    return createCachedToken(parameterSet, method, authentication);
  }

  /**
   * Creates or retrieves a cached {@link DedicatedVaultToken} for the specified
   * authentication method.
   *
   * @param parameterSet the set of parameters for the request.
   * @param method selected authentication method.
   * @param authentication the authentication method to generate the token.
   * @return a {@code DedicatedVaultToken} instance.
   */
  private static DedicatedVaultToken createCachedToken(
          ParameterSet parameterSet,
          DedicatedVaultAuthenticationMethod method,
          AbstractDedicatedVaultAuthentication authentication) {

    enforceTrustedVaultEndpointForCredentials(parameterSet);

    TokenCacheKey cacheKey = TokenCacheKey.create(
            method, authentication.generateCacheKey(parameterSet));

    TOKEN_CACHE_LOCK.lock();
    try {
      evictExpiredEntries();

      CachedToken cachedToken = tokenCache.get(cacheKey);
      if (cachedToken != null && cachedToken.isValid()) {
        return new DedicatedVaultToken(cachedToken.getToken().token().get());
      }
    } finally {
      TOKEN_CACHE_LOCK.unlock();
    }

    CachedToken generatedToken = authentication.generateToken(parameterSet);

    TOKEN_CACHE_LOCK.lock();
    try {
      evictExpiredEntries();

      CachedToken existing = tokenCache.get(cacheKey);
      if (existing != null && existing.isValid()) {
        return new DedicatedVaultToken(existing.getToken().token().get());
      }

      tokenCache.put(cacheKey, generatedToken);
      evictSurplusEntries();
      return new DedicatedVaultToken(generatedToken.getToken().token().get());
    } finally {
      TOKEN_CACHE_LOCK.unlock();
    }
  }

  /**
   * Blocks use of secret-bearing credentials when the target vault endpoint is untrusted.
   *
   * @param parameterSet Parameter set containing endpoint and credential values.
   */
  private static void enforceTrustedVaultEndpointForCredentials(ParameterSet parameterSet) {
    String vaultAddr = parameterSet.getOptional(VAULT_ADDR);
    if (vaultAddr == null || vaultAddr.trim().isEmpty()) {
      return;
    }

    for (Parameter<?> parameter : SENSITIVE_CREDENTIAL_PARAMETERS) {
      if (parameterSet.contains(parameter)) {
        String parameterName = parameterSet.getName(parameter);
        VaultEndpointPolicy.ensureCredentialAllowed(
                vaultAddr,
                parameterName == null ? parameter.toString() : parameterName);
      }
    }
  }

  /**
   * Removes cache entries whose token is no longer valid.
   */
  private static void evictExpiredEntries() {
    tokenCache.entrySet().removeIf(entry -> !entry.getValue().isValid());
  }

  /**
   * Enforces an upper bound on cache size by evicting one oldest key when oversized.
   */
  private static void evictSurplusEntries() {
    if (tokenCache.size() <= MAX_CACHE_ENTRIES) {
      return;
    }

    Iterator<TokenCacheKey> keyIterator = tokenCache.keySet().iterator();
    if (keyIterator.hasNext()) {
      TokenCacheKey key = keyIterator.next();
      tokenCache.remove(key);
    }
  }

  /**
   * Returns an instance of the appropriate authentication class based on the method.
   *
   * @param method The selected authentication method.
   * @return An instance of {@link AbstractDedicatedVaultAuthentication}.
   */
  private static AbstractDedicatedVaultAuthentication getAuthenticationInstance(DedicatedVaultAuthenticationMethod method) {
    switch (method) {
      case VAULT_TOKEN:
        return VaultTokenAuthentication.INSTANCE;
      case USERPASS:
        return UserpassAuthentication.INSTANCE;
      case APPROLE:
        return AppRoleAuthentication.INSTANCE;
      case GITHUB:
        return GitHubAuthentication.INSTANCE;
      case AUTO_DETECT:
        return AutoDetectAuthentication.INSTANCE;
      default:
        throw new IllegalArgumentException("Unsupported authentication method: " + method);
    }
  }

  /**
   * Reads cache size from configuration.
   *
   * @return Configured max cache entries, or default when unset.
   * @throws IllegalArgumentException If configured value is not a valid integer.
   */
  private static int getConfiguredCacheMaxEntries() {
    String configured = System.getProperty(
            KEY_TOKEN_CACHE_MAX_ENTRIES,
            System.getenv(KEY_TOKEN_CACHE_MAX_ENTRIES));
    if (configured == null || configured.trim().isEmpty()) {
      return 100;
    }

    try {
      return Integer.parseInt(configured.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
              "Invalid token cache size '" + configured + "' for "
                      + KEY_TOKEN_CACHE_MAX_ENTRIES, e);
    }
  }

  /**
   * Canonical cache key for token reuse.
   */
  private static final class TokenCacheKey {
    private final DedicatedVaultAuthenticationMethod authenticationMethod;
    private final Map<String, Object> keyParts;

    private TokenCacheKey(
            DedicatedVaultAuthenticationMethod authenticationMethod,
            Map<String, Object> keyParts) {
      this.authenticationMethod = authenticationMethod;
      this.keyParts = keyParts;
    }

    /**
     * Creates a canonical cache key from method and key parts.
     *
     * @param method Authentication method associated with the key.
     * @param sourceKeyParts Source key parts before normalization.
     * @return Canonical cache key.
     */
    private static TokenCacheKey create(
            DedicatedVaultAuthenticationMethod method, Map<String, Object> sourceKeyParts) {

      Map<String, Object> normalized = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      for (Map.Entry<String, Object> entry : sourceKeyParts.entrySet()) {
        normalized.put(entry.getKey(), normalizeKeyPart(entry.getKey(), entry.getValue()));
      }

      return new TokenCacheKey(method, Collections.unmodifiableMap(new HashMap<>(normalized)));
    }

    /**
     * Normalizes one key part and hashes sensitive values.
     *
     * @param key Key-part name.
     * @param value Key-part value.
     * @return Normalized key-part representation.
     */
    private static Object normalizeKeyPart(String key, Object value) {
      if (value == null) {
        return null;
      }

      String valueAsText = String.valueOf(value);
      if (SENSITIVE_CACHE_KEY_FIELDS.contains(key)) {
        return "sha256:" + sha256Hex(valueAsText);
      }

      return valueAsText;
    }

    /**
     * Computes SHA-256 hex for key material.
     *
     * @param text Text to hash.
     * @return Lowercase hexadecimal digest.
     * @throws IllegalStateException If hashing cannot be performed.
     */
    private static String sha256Hex(String text) {
      try {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] digest = messageDigest.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
          hex.append(Character.forDigit((b >> 4) & 0xF, 16));
          hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
      } catch (Exception e) {
        throw new IllegalStateException("Failed to hash cache key material", e);
      }
    }

    @Override
    public int hashCode() {
      return 31 * authenticationMethod.hashCode() + keyParts.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof TokenCacheKey)) {
        return false;
      }

      TokenCacheKey that = (TokenCacheKey) other;
      return authenticationMethod == that.authenticationMethod
              && keyParts.equals(that.keyParts);
    }
  }

}
