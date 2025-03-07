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

import oracle.jdbc.driver.oauth.OpaqueAccessToken;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.*;

/**
 * Enumeration of authentication methods supported by Dedicated HashiCorp Vault.
 * <p>
 * The enum handles token generation and caching to improve performance and
 * reduce the number of authentication requests to the Vault server.
 * </p>
 * */
public enum DedicatedVaultAuthenticationMethod {

  /**
   * Authentication using a Vault token.
   * <p>
   * The Vault token is a secure string used to authenticate API requests
   * to the HashiCorp Vault. It can be provided through configuration parameters,
   * environment variables, or system properties.
   * </p>
   */
  VAULT_TOKEN(new AbstractDedicatedVaultAuthentication() {
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      String vaultToken = getVaultToken(parameterSet);
      return new CachedToken(OpaqueAccessToken.create(vaultToken.toCharArray(), null));
    }

    @Override
    public Map<String, Object> generateCacheKey(ParameterSet parameterSet) {
      return filterParameters(parameterSet, AUTH_METHOD_PARAMETERS.get(VAULT_TOKEN));
    }
  }),

  /**
   * Authentication using the Userpass method.
   * <p>
   * The Userpass method allows authentication using a username and password.
   * It is suitable for scenarios where user credentials are managed directly
   * by Vault. For more information, see the HashiCorp Vault documentation:
   * <a href="https://developer.hashicorp.com/vault/api-docs/auth/userpass">
   * Userpass Authentication API</a>.
   * </p>
   */
  USERPASS(new AbstractDedicatedVaultAuthentication() {
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      String vaultAddr = getVaultAddress(parameterSet);
      String authPath = getUserpassAuthPath(parameterSet);
      String namespace = getNamespace(parameterSet);
      String username = getUsername(parameterSet);
      String password = getPassword(parameterSet);

      String authEndpoint = buildAuthEndpoint(vaultAddr, USERPASS_LOGIN_TEMPLATE, authPath, username);
      String payload = createJsonPayload(USERPASS_PAYLOAD_TEMPLATE, password);
      return performAuthentication(authEndpoint, payload, namespace, null, "Failed to authenticate using Userpass");
    }

    @Override
    public Map<String, Object> generateCacheKey(ParameterSet parameterSet) {
      return filterParameters(parameterSet, AUTH_METHOD_PARAMETERS.get(USERPASS));
    }
  }),

  /**
   * Authentication using the AppRole method.
   * <p>
   * The AppRole method allows authentication using a Role ID and Secret ID.
   * This method is designed for machine-to-machine authentication or
   * service-based applications. For more information, see the HashiCorp Vault
   * documentation:
   * <a href="https://developer.hashicorp.com/vault/api-docs/auth/approle">
   * AppRole Authentication API</a>.
   * </p>
   */
  APPROLE(new AbstractDedicatedVaultAuthentication() {
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      String vaultAddr = getVaultAddress(parameterSet);
      String namespace = getNamespace(parameterSet);
      String roleId = getRoleId(parameterSet);
      String secretId = getSecretId(parameterSet);
      String authPath = getAppRoleAuthPath(parameterSet);

      String authEndpoint = buildAuthEndpoint(vaultAddr, APPROLE_LOGIN_TEMPLATE, authPath);
      String payload = createJsonPayload(APPROLE_PAYLOAD_TEMPLATE, roleId, secretId);
      return performAuthentication(authEndpoint, payload, namespace, null, "Failed to authenticate with AppRole");
    }

    @Override
    public Map<String, Object> generateCacheKey(ParameterSet parameterSet) {
      return filterParameters(parameterSet, AUTH_METHOD_PARAMETERS.get(APPROLE));
    }
  }),

  /**
   * Authentication using the GitHub method.
   * <p>
   * The GitHub method allows authentication using a GitHub personal access token.
   * This is particularly useful for applications or developers using GitHub
   * as an identity provider for Vault. For more information, see:
   * <a href="https://developer.hashicorp.com/vault/docs/auth/github">
   * GitHub Authentication API</a>.
   * </p>
   */
  GITHUB(new AbstractDedicatedVaultAuthentication() {
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      String vaultAddr = getVaultAddress(parameterSet);
      String githubToken = getGitHubToken(parameterSet);
      String namespace = getNamespace(parameterSet);
      String githubAuthPath = getGitHubAuthPath(parameterSet);

      String authEndpoint = buildAuthEndpoint(vaultAddr, GITHUB_LOGIN_TEMPLATE, githubAuthPath);
      String payload = createJsonPayload(GITHUB_PAYLOAD_TEMPLATE, githubToken);
      return performAuthentication(authEndpoint, payload, namespace, githubToken, "Failed to authenticate with GitHub");
    }

    @Override
    public Map<String, Object> generateCacheKey(ParameterSet parameterSet) {
      return filterParameters(parameterSet, AUTH_METHOD_PARAMETERS.get(GITHUB));
    }
  }),

  /**
   * Automatically selects the best authentication method based on available parameters.
   *
   * <p>Priority order:</p>
   * <ol>
   *   <li>Uses the Vault token if available.</li>
   *   <li>Falls back to Userpass authentication.</li>
   *   <li>Then attempts AppRole authentication.</li>
   *   <li>Finally, tries GitHub authentication.</li>
   * </ol>
   */
  AUTO_DETECT(new AbstractDedicatedVaultAuthentication() {
    @Override
    public CachedToken generateToken(ParameterSet parameterSet) {
      IllegalStateException previousFailure;

      // Attempt VAULT_TOKEN authentication first
      try {
        return VAULT_TOKEN.generateToken(parameterSet);
      } catch (RuntimeException noVaultToken) {
        previousFailure = new IllegalStateException(
                "Failed to authenticate using a Vault token", noVaultToken);
      }

      // Attempt USERPASS authentication
      try {
        return USERPASS.generateToken(parameterSet);
      } catch (RuntimeException noUserpass) {
        previousFailure.addSuppressed(new IllegalStateException(
                "Failed to authenticate using Userpass credentials", noUserpass));
      }

      // Attempt APPROLE authentication
      try {
        return APPROLE.generateToken(parameterSet);
      } catch (RuntimeException noAppRole) {
        previousFailure.addSuppressed(new IllegalStateException(
                "Failed to authenticate using AppRole credentials", noAppRole));
      }

      // Attempt GITHUB authentication
      try {
        return GITHUB.generateToken(parameterSet);
      } catch (RuntimeException noGitHub) {
        previousFailure.addSuppressed(new IllegalStateException(
                "Failed to authenticate using GitHub credentials", noGitHub));
      }

      // If all methods fail, throw an error
      throw previousFailure;
    }

    @Override
    public Map<String, Object> generateCacheKey(ParameterSet parameterSet) {
      for (DedicatedVaultAuthenticationMethod method : AUTH_METHOD_PARAMETERS.keySet()) {
        Map<String, Object> filteredParams = filterParameters(parameterSet, AUTH_METHOD_PARAMETERS.get(method));
        if (!filteredParams.isEmpty()) {
          return filteredParams;
        }
      }
      return Collections.emptyMap();
    }
  });


  private final AbstractDedicatedVaultAuthentication delegate;

  DedicatedVaultAuthenticationMethod(AbstractDedicatedVaultAuthentication delegate) {
    this.delegate = delegate;
  }

  /**
   * Delegates token generation to the underlying authentication strategy.
   *
   * @param parameterSet the authentication parameters.
   * @return the generated {@link CachedToken}.
   */
  public CachedToken generateToken(ParameterSet parameterSet) {
    return delegate.generateToken(parameterSet);
  }

  /**
   * Delegates cache key generation to the underlying authentication strategy.
   *
   * @param parameterSet the authentication parameters.
   * @return a {@link ParameterSet} to be used as a cache key.
   */
  public Map<String, Object> generateCacheKey(ParameterSet parameterSet) {
    return delegate.generateCacheKey(parameterSet);
  }

  /**
   * Maps each {@link DedicatedVaultAuthenticationMethod} to its relevant parameter keys.
   *
   * This map is used to filter parameters for cache key generation, ensuring only
   * necessary parameters are included for each authentication method. It is immutable.
   */
  private static final Map<DedicatedVaultAuthenticationMethod, String[]> AUTH_METHOD_PARAMETERS;
  static {
    Map<DedicatedVaultAuthenticationMethod, String[]> map = new HashMap<>();
    map.put(VAULT_TOKEN, new String[]{
            PARAM_VAULT_ADDR, PARAM_VAULT_TOKEN
    });
    map.put(USERPASS, new String[]{
            PARAM_VAULT_ADDR, PARAM_VAULT_NAMESPACE, PARAM_USERPASS_AUTH_PATH, PARAM_VAULT_USERNAME, PARAM_VAULT_PASSWORD
    });
    map.put(APPROLE, new String[]{
            PARAM_VAULT_ADDR, PARAM_VAULT_NAMESPACE, PARAM_APPROLE_AUTH_PATH, PARAM_VAULT_ROLE_ID, PARAM_VAULT_SECRET_ID
    });
    map.put(GITHUB, new String[]{
            PARAM_VAULT_ADDR, PARAM_VAULT_NAMESPACE, PARAM_GITHUB_AUTH_PATH, PARAM_GITHUB_TOKEN
    });
    map.put(AUTO_DETECT, new String[]{
            PARAM_VAULT_ADDR, PARAM_VAULT_NAMESPACE, PARAM_VAULT_TOKEN,
            PARAM_USERPASS_AUTH_PATH, PARAM_VAULT_USERNAME, PARAM_VAULT_PASSWORD,
            PARAM_APPROLE_AUTH_PATH, PARAM_VAULT_ROLE_ID, PARAM_VAULT_SECRET_ID,
            PARAM_GITHUB_AUTH_PATH, PARAM_GITHUB_TOKEN
    });
    AUTH_METHOD_PARAMETERS = Collections.unmodifiableMap(map);
  }

}
