/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.azure.oauth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.azure.AzureResourceFactory;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.util.JsonWebTokenParser;

import java.time.OffsetDateTime;
import java.util.Arrays;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * Factory for requesting access tokens from the Active Directory service of
 * Azure.
 */
public final class AccessTokenFactory
  extends AzureResourceFactory<AccessToken> {

  /** Scope of the provided token. This is a required parameter. */
  public static final Parameter<String> SCOPE = Parameter.create(REQUIRED);

  private static final ResourceFactory<AccessToken> INSTANCE =
    CachedResourceFactory.create(new AccessTokenFactory());

  private AccessTokenFactory() { }

  /**
   * Returns a singleton of {@code AccessTokenFactory}.
   * @return a singleton of {@code AccessTokenFactory}
   */
  public static ResourceFactory<AccessToken> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<AccessToken> request(
    TokenCredential tokenCredential, ParameterSet parameterSet) {

    String scope = parameterSet.getRequired(SCOPE);

    TokenRequestContext context = new TokenRequestContext();
    context.addScopes(scope);

    com.azure.core.credential.AccessToken azureAccessToken =
      tokenCredential.getToken(context).block();

    final AccessToken jdbcAccessToken;
    char[] jsonWebToken = azureAccessToken.getToken().toCharArray();
    try {
      jdbcAccessToken = AccessToken.createJsonWebToken(jsonWebToken);
    }
    finally {
      Arrays.fill(jsonWebToken, (char)0);
    }

    // TODO: Should azureAccessToken.getExpiresAt() be used here? In version
    //  1.26 of the Azure SDK, the code appears to always return
    //  OffsetDateTime.MAX. That does not seem correct, but maybe it is? If it
    //  is correct, then prefer using the SDK's API over our JWT parser.
    OffsetDateTime expireTime =
      JsonWebTokenParser.parseExp(azureAccessToken.getToken());

    return Resource.createExpiringResource(jdbcAccessToken, expireTime, true);
  }
}
