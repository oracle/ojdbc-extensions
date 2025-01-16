/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
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

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

/**
 * A factory for creating {@link HcpVaultCredentials} objects for HCP Vault Secrets.
 * <p>
 * This class implements the client_credentials flow for OAuth2 authentication, retrieving
 * an API token to interact with the HCP Vault Secrets API.
 * </p>
 */
public final class HcpVaultCredentialsFactory implements ResourceFactory<HcpVaultCredentials> {

  // 1 minutes buffer for token expiration (in ms)
  private static final long TOKEN_TTL_BUFFER = 60_000;
  private static volatile BearerToken bearerToken;

  /**
   * Parameter indicating the authentication method to use for HCP Vault Secrets.
   */
  public static final Parameter<HcpVaultAuthenticationMethod> AUTHENTICATION_METHOD = Parameter.create(REQUIRED);

  /**
   * Parameter for the OAuth2 client ID. Required.
   */
  public static final Parameter<String> CLIENT_ID = Parameter.create(REQUIRED);

  /**
   * Parameter for the OAuth2 client secret. Required.
   */
  public static final Parameter<String> CLIENT_SECRET = Parameter.create(REQUIRED);

  private static final HcpVaultCredentialsFactory INSTANCE = new HcpVaultCredentialsFactory();

  private HcpVaultCredentialsFactory() {}

  public static HcpVaultCredentialsFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<HcpVaultCredentials> request(ParameterSet parameterSet) {
    HcpVaultCredentials credentials = getCredential(parameterSet);
    return Resource.createPermanentResource(credentials, true);
  }

  private HcpVaultCredentials getCredential(ParameterSet parameterSet) {
    HcpVaultAuthenticationMethod method = parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (method) {
      case CLIENT_CREDENTIALS:
        return createClientCredentials(parameterSet);
      default:
        throw new IllegalArgumentException("Unrecognized HCP auth method: " + method);
    }
  }

  private HcpVaultCredentials createClientCredentials(ParameterSet parameterSet) {
    synchronized (HcpVaultCredentialsFactory.class) {
      long currentTime = System.currentTimeMillis();

      if (bearerToken != null && currentTime < bearerToken.getExpiryTime()) {
        return new HcpVaultCredentials(bearerToken.getToken());
      }

      String clientId = getRequiredOrFallback(parameterSet, CLIENT_ID, "CLIENT_ID");
      String clientSecret = getRequiredOrFallback(parameterSet, CLIENT_SECRET, "CLIENT_SECRET");

      String apiToken = HcpVaultOAuthClient.fetchHcpAccessToken(clientId, clientSecret);
      if (apiToken == null || apiToken.isEmpty()) {
        throw new IllegalStateException("Failed to obtain HCP token using client_credentials flow");
      }

      long tokenTTL = HcpVaultOAuthClient.getLastTokenTTL() * 1000;
      bearerToken = new BearerToken(apiToken, currentTime + tokenTTL - TOKEN_TTL_BUFFER);

      return new HcpVaultCredentials(apiToken);
    }
  }

  /**
   * A class representing a Bearer Token and its expiration time.
   */
  private static class BearerToken {
    private final String token;
    private final long expiryTime;

    BearerToken(String token, long expiryTime) {
      this.token = token;
      this.expiryTime = expiryTime;
    }

    public String getToken() {
      return token;
    }

    public long getExpiryTime() {
      return expiryTime;
    }
  }

}
