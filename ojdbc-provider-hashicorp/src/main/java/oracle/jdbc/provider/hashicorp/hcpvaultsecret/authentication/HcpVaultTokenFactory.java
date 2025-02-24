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

import java.util.function.Supplier;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

/**
 * A factory for creating {@link HcpVaultSecretToken} objects for HCP Vault Secrets.
 * <p>
 * Implements the client_credentials flow as well as file-based authentication.
 * The auto-detect mode attempts file-based authentication first, then falls back
 * to client credentials.
 * </p>
 */
public final class HcpVaultTokenFactory implements ResourceFactory<HcpVaultSecretToken> {

  /**
   * Parameter indicating the authentication method to use for HCP Vault Secrets.
   */
  public static final Parameter<HcpVaultAuthenticationMethod> AUTHENTICATION_METHOD = Parameter.create(REQUIRED);

  /**
   * Parameter for the OAuth2 client ID. Required.
   */
  public static final Parameter<String> HCP_CLIENT_ID = Parameter.create(REQUIRED);

  /**
   * Parameter for the OAuth2 client secret. Required.
   */
  public static final Parameter<String> HCP_CLIENT_SECRET = Parameter.create(REQUIRED);

  /**
   * Parameter for the credentials file path.
   * By default, the credentials file is expected at:
   * <code>System.getProperty("user.home") + "/.config/hcp/creds-cache.json"</code>.
   */
  public static final Parameter<String> HCP_CREDENTIALS_FILE = Parameter.create(REQUIRED);

  private static final HcpVaultTokenFactory INSTANCE = new HcpVaultTokenFactory();

  /**
   * Cached supplier for tokens.
   */
  private static volatile Supplier<? extends AccessToken> cachedTokenSupplier;

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

    switch (method) {
      case CLIENT_CREDENTIALS:
        return getCachedToken(() -> createClientCredentials(parameterSet));
      case CLI_CREDENTIALS_FILE:
        return getCachedToken(() -> createFileBasedCredentials(parameterSet));
      case AUTO_DETECT:
        return getCachedToken(() -> autoDetectAuthentication(parameterSet));
      default:
        throw new IllegalArgumentException("Unrecognized HCP Vault Secret " +
                "authentication method: " + method);
    }
  }

  /**
   * Retrieves a cached token if available, otherwise creates a new one.
   *
   * @param tokenSupplier The supplier function to generate a new token.
   * @return The cached or newly generated token.
   */
  private HcpVaultSecretToken getCachedToken(Supplier<HcpVaultSecretToken> tokenSupplier) {
    if (cachedTokenSupplier == null) {
      synchronized (HcpVaultTokenFactory.class) {
        if (cachedTokenSupplier == null) {
          cachedTokenSupplier = AccessToken.createJsonWebTokenCache(() -> {
            HcpVaultSecretToken token = tokenSupplier.get();
            return AccessToken.createJsonWebToken(token.getHcpApiToken().toCharArray());
          });
        }
      }
    }

    AccessToken cachedToken = cachedTokenSupplier.get();
    JsonWebToken jwt = (JsonWebToken) cachedToken;
    return new HcpVaultSecretToken(jwt.token().get());
  }

  /**
   * Creates credentials using the OAuth2 client credentials flow.
   *
   * @param parameterSet The parameter set containing client credentials.
   * @return The HCP Vault secret token.
   */
  private HcpVaultSecretToken createClientCredentials(ParameterSet parameterSet) {
    String clientId = getRequiredOrFallback(parameterSet, HCP_CLIENT_ID, "HCP_CLIENT_ID");
    String clientSecret = getRequiredOrFallback(parameterSet, HCP_CLIENT_SECRET, "HCP_CLIENT_SECRET");

    String rawToken = HcpVaultOAuthClient.fetchHcpAccessToken(clientId, clientSecret);
    return new HcpVaultSecretToken(rawToken);
  }

  /**
   * Creates credentials using an existing credentials file.
   *
   * @param parameterSet The parameter set containing the file path.
   * @return The HCP Vault secret token.
   */
  private HcpVaultSecretToken createFileBasedCredentials(ParameterSet parameterSet) {
    try {
      HcpVaultCredentialsFileAuthenticator fileAuthenticator = new HcpVaultCredentialsFileAuthenticator(parameterSet);
      String token = fileAuthenticator.getValidAccessToken();
      return new HcpVaultSecretToken(token);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to retrieve HCP Vault token from credentials file", e);
    }
  }

  /**
   * Automatically detects the most appropriate authentication method
   * based on available parameters.
   *
   * <p>The priority order is:</p>
   * <ol>
   *   <li>Use the HCP credentials file if available and valid.</li>
   *   <li>Fallback to client credentials authentication.</li>
   *   <li>Throw an error if no valid authentication method is found.</li>
   * </ol>
   *
   * @param parameterSet The parameter set containing possible authentication details.
   * @return The detected authentication token.
   */
  private HcpVaultSecretToken autoDetectAuthentication(ParameterSet parameterSet) {
    IllegalStateException previousFailure;

    // 1. Try CLI_CREDENTIALS_FILE authentication first
    try {
      return createFileBasedCredentials(parameterSet);
    } catch (RuntimeException fileAuthFailed) {
      previousFailure = new IllegalStateException(
              "Failed to authenticate using credentials file",
              fileAuthFailed);
    }

    // 2. If that fails, try CLIENT_CREDENTIALS
    try {
      return createClientCredentials(parameterSet);
    } catch (RuntimeException clientAuthFailed) {
      previousFailure.addSuppressed(new IllegalStateException(
              "Failed to authenticate using client credentials",
              clientAuthFailed));
    }

    // 3. If all methods fail, throw an error
    throw previousFailure;
  }

}
