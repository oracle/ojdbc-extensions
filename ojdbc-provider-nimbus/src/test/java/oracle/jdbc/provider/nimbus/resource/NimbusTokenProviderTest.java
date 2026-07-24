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

package oracle.jdbc.provider.nimbus.resource;

import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.resource.ResourceProviderTestUtil;
import oracle.jdbc.spi.AccessTokenProvider;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;

/**
 * Verifies {@link NimbusTokenProvider} as
 * implementing the behavior specified by its JavaDoc
 */
public class NimbusTokenProviderTest {

  private static final AccessTokenProvider PROVIDER =
    findProvider(AccessTokenProvider.class, "ojdbc-provider-nimbus-token");

  /**
   * Verifies a grantType=client_credentials
   */
  @Test
  public void testClientCredentials() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("grantType", "client_credentials");
    testParameters.put("tokenEndpoint", TestProperties.getOrAbort(
      NimbusTestProperty.TOKEN_ENDPOINT));
    testParameters.put("clientId", TestProperties.getOrAbort(
      NimbusTestProperty.CLIENT_ID));
    testParameters.put("clientSecret", TestProperties.getOrAbort(
      NimbusTestProperty.CLIENT_SECRET));
    verifyAccessToken(testParameters);
  }


  /**
   * Verifies grantType=password
   */
  @Test
  public void testPassword() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("grantType", "password");
    testParameters.put("tokenEndpoint", TestProperties.getOrAbort(
      NimbusTestProperty.TOKEN_ENDPOINT));
    testParameters.put("clientId", TestProperties.getOrAbort(
      NimbusTestProperty.CLIENT_ID));
    testParameters.put("clientSecret", TestProperties.getOrAbort(
      NimbusTestProperty.CLIENT_SECRET));
    testParameters.put("username", TestProperties.getOrAbort(
      NimbusTestProperty.USERNAME));
    testParameters.put("password", TestProperties.getOrAbort(
      NimbusTestProperty.PASSWORD));
    verifyAccessToken(testParameters);
  }

  /** Verifies an access token returned for a URI with the given parameters */
  private static void verifyAccessToken(
    Map<String, CharSequence> testParameters) {

    Map<Parameter, CharSequence> parameterValues =
      createParameters(testParameters);

    AccessToken accessToken = PROVIDER.getAccessToken(parameterValues);

    Assertions.assertNotNull(accessToken);
  }

  private static Map<Parameter, CharSequence> createParameters(
    Map<String, CharSequence> testParameters) {

    // Add default values for all tests
    testParameters.putIfAbsent("scope", TestProperties.getOrAbort(
      NimbusTestProperty.SCOPE));

    return ResourceProviderTestUtil.createParameterValues(
      PROVIDER, testParameters);
  }
}
