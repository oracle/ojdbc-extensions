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

package oracle.jdbc.provider.azure.resource;

import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.azure.oauth.AccessTokenFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.spi.AccessTokenProvider;

import java.util.Map;

/**
 * <p>
 * A provider of OAUTH access tokens issued by Azure's Active Directory service.
 * This class inherits common parameters and behavior from
 * {@link AzureResourceProvider}.
 * </p><p>
 * This class implements the {@link AccessTokenProvider} SPI defined by Oracle
 * JDBC. It is designed to be located and instantiated by
 * {@link java.util.ServiceLoader}.
 * </p>
 */
public final class AzureTokenProvider
  extends AzureResourceProvider
  implements AccessTokenProvider {

  private static final ResourceParameter[] PARAMETERS =
    new ResourceParameter[] {
      new ResourceParameter("scope", AccessTokenFactory.SCOPE)
    };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public AzureTokenProvider() {
    super("token", PARAMETERS);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns an access token that is requested from Azure.
   * </p><p>
   * The {@code parameters} MUST include a parameter named {@code scope} that
   * configures the
   * <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-permissions-and-consent#scopes-and-permissions">
   * scope
   * </a> for which access is requested.
   * </p><p>
   * The {@code parameters} MAY include a parameter named {@code timeout}
   * that configures a timeout <i>of seconds</i> for the request. If no
   * timeout is configured, then no timeout is enforced for the request.
   * </p>
   */
  @Override
  public AccessToken getAccessToken(
    Map<Parameter, CharSequence> parameterValues) {

    ParameterSet parameterSet = parseParameterValues(parameterValues);
    // we get the supplier than we get the token from supplier with get method
    return AccessTokenFactory.getInstance()
      .request(parameterSet)
      .getContent().get();
  }

}
