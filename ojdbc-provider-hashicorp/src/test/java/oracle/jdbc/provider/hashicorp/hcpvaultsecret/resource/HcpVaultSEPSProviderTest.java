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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.hashicorp.hcpvaultsecret.HcpVaultTestUtil;
import oracle.jdbc.provider.hashicorp.hcpvaultsecret.HcpVaultTestProperty;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.spi.UsernameProvider;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class HcpVaultSEPSProviderTest {

  private static final UsernameProvider USERNAME_PROVIDER = findProvider(
    UsernameProvider.class, "ojdbc-provider-hcpvault-secrets-seps");

  private static final PasswordProvider PASSWORD_PROVIDER = findProvider(
    PasswordProvider.class, "ojdbc-provider-hcpvault-secrets-seps");

  @Test
  public void testGetusernameParams() {
    Collection<? extends Parameter> usernameParams = USERNAME_PROVIDER.getParameters();
    Collection<? extends Parameter> passwordParams = PASSWORD_PROVIDER.getParameters();

    assertEquals(usernameParams, passwordParams);

    Parameter secretNameParameter =
      usernameParams.stream()
        .filter(parameter -> "secretName".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(secretNameParameter.isRequired());
    assertNull(secretNameParameter.defaultValue());

    Parameter orgIdParameter =
      usernameParams.stream()
        .filter(parameter -> "orgId".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(orgIdParameter.isRequired());
    assertNull(orgIdParameter.defaultValue());

    Parameter appNameParameter =
      usernameParams.stream()
        .filter(parameter -> "appName".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(appNameParameter.isRequired());
    assertNull(appNameParameter.defaultValue());

    Parameter projectIdParameter =
      usernameParams.stream()
        .filter(parameter -> "projectId".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(projectIdParameter.isRequired());
    assertNull(projectIdParameter.defaultValue());


    Parameter walletPasswordParameter =
      usernameParams.stream()
        .filter(parameter -> "walletPassword".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(walletPasswordParameter.isSensitive());
    assertFalse(walletPasswordParameter.isRequired());
    assertNull(walletPasswordParameter.defaultValue());
  }

  @Test
  public void testRetrieveUsernameFromPKCS12() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_SECRET_PKCS12_NAME));
    params.put("walletPassword", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_PASSWORD));
    HcpVaultTestUtil.configureAuthentication(params);

    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
    String username = USERNAME_PROVIDER.getUsername(values);

    assertNotNull(username);
  }

  @Test
  public void testRetrievePasswordFromPKCS12() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_SECRET_PKCS12_NAME));
    params.put("walletPassword", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_PASSWORD));
    HcpVaultTestUtil.configureAuthentication(params);

    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
    char[] password = PASSWORD_PROVIDER.getPassword(values);
    assertNotNull(password);
  }

  @Test
  public void testRetrieveUsernameFromSSO() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_SECRET_SSO_NAME));
    HcpVaultTestUtil.configureAuthentication(params);

    Map<Parameter, CharSequence> values = createParameterValues(USERNAME_PROVIDER, params);
    String username = USERNAME_PROVIDER.getUsername(values);

    assertNotNull(username);
  }

  @Test
  public void testRetrievePasswordFromSSO() {
    Map<String, String> params = new HashMap<>();
    params.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_SECRET_SSO_NAME));
    HcpVaultTestUtil.configureAuthentication(params);

    Map<Parameter, CharSequence> values = createParameterValues(PASSWORD_PROVIDER, params);
    char[] password = PASSWORD_PROVIDER.getPassword(values);

    assertNotNull(password);
  }

}