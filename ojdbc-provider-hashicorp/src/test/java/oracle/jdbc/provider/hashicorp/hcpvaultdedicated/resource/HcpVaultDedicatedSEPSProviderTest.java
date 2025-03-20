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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.DedicatedVaultTestProperty;
import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.HcpVaultDedicatedTestUtil;
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

public class HcpVaultDedicatedSEPSProviderTest {

  private static final UsernameProvider USERNAME_PROVIDER = findProvider(
    UsernameProvider.class, "ojdbc-provider-hcpvault-dedicated-seps");

  private static final PasswordProvider PASSWORD_PROVIDER = findProvider(
    PasswordProvider.class, "ojdbc-provider-hcpvault-dedicated-seps");

  @Test
  public void testProviderParameters() {
    Collection<? extends Parameter> usernameParameters =
            USERNAME_PROVIDER.getParameters();
    Collection<? extends Parameter> passwordParameters =
            PASSWORD_PROVIDER.getParameters();

    // Verify both providers have the same parameters
    assertEquals(usernameParameters, passwordParameters,
            "Username and Password providers should have identical parameters");

    assertNotNull(usernameParameters);
    assertNotNull(passwordParameters);

    Parameter vaultAddrParameter =
      usernameParameters.stream()
        .filter(parameter -> "vaultAddr".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertFalse(vaultAddrParameter.isSensitive());
    assertTrue(vaultAddrParameter.isRequired());
    assertNull(vaultAddrParameter.defaultValue());

    Parameter secretPathParameter =
      usernameParameters.stream()
        .filter(parameter -> "secretPath".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertFalse(secretPathParameter.isSensitive());
    assertTrue(secretPathParameter.isRequired());
    assertNull(secretPathParameter.defaultValue());

    Parameter walletPasswordParameter =
      usernameParameters.stream()
        .filter(parameter -> "walletPassword".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(walletPasswordParameter.isSensitive());
    assertNull(walletPasswordParameter.defaultValue());

    Parameter connStringParameter =
      usernameParameters.stream()
        .filter(parameter -> "connectionStringIndex".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertFalse(connStringParameter.isSensitive());
    assertNull(connStringParameter.defaultValue());
  }

  @Test
  public void testRetrieveUsernameFromPKCS12() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("vaultAddr", TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR));
    testParameters.put("secretPath", TestProperties.getOrAbort(DedicatedVaultTestProperty.WALLET_SECRET_PATH));
    testParameters.put("fieldName", "base64-seps-p12");
    testParameters.put("walletPassword", TestProperties.getOrAbort(DedicatedVaultTestProperty.WALLET_PASSWORD));

    HcpVaultDedicatedTestUtil.configureAuthentication(testParameters);

    Map<Parameter, CharSequence> parameterValues = createParameterValues(USERNAME_PROVIDER, testParameters);
    String username = USERNAME_PROVIDER.getUsername(parameterValues);

    assertNotNull(username);
  }

  @Test
  public void testRetrievePasswordFromPKCS12() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("vaultAddr", TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR));
    testParameters.put("secretPath", TestProperties.getOrAbort(DedicatedVaultTestProperty.WALLET_SECRET_PATH));
    testParameters.put("fieldName", "base64-seps-p12");
    testParameters.put("walletPassword", TestProperties.getOrAbort(DedicatedVaultTestProperty.WALLET_PASSWORD));

    HcpVaultDedicatedTestUtil.configureAuthentication(testParameters);

    Map<Parameter, CharSequence> parameterValues = createParameterValues(PASSWORD_PROVIDER, testParameters);
    char[] password = PASSWORD_PROVIDER.getPassword(parameterValues);

    assertNotNull(password);
  }

  /**
   * Test retrieving a password from an SSO SEPS wallet with a connection
   * string index.
   */
  @Test
  public void testRetrievePasswordFromSSO() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("vaultAddr", TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR));
    testParameters.put("secretPath", TestProperties.getOrAbort(DedicatedVaultTestProperty.WALLET_SECRET_PATH));
    testParameters.put("fieldName", "base64-seps-sso");

    HcpVaultDedicatedTestUtil.configureAuthentication(testParameters);

    Map<Parameter, CharSequence> parameterValues = createParameterValues(PASSWORD_PROVIDER, testParameters);
    char[] password = PASSWORD_PROVIDER.getPassword(parameterValues);
    assertNotNull(password);
  }

  /**
   * Test retrieving a username from an SSO SEPS wallet without specifying a connection string index.
   */
  @Test
  public void testRetrieveUsernameFromSSO() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("vaultAddr", TestProperties.getOrAbort(DedicatedVaultTestProperty.VAULT_ADDR));
    testParameters.put("secretPath", TestProperties.getOrAbort(DedicatedVaultTestProperty.WALLET_SECRET_PATH));
    testParameters.put("fieldName", "base64-seps-sso");

    HcpVaultDedicatedTestUtil.configureAuthentication(testParameters);

    Map<Parameter, CharSequence> parameterValues = createParameterValues(USERNAME_PROVIDER, testParameters);
    String username = USERNAME_PROVIDER.getUsername(parameterValues);
    assertNotNull(username);
  }



}
