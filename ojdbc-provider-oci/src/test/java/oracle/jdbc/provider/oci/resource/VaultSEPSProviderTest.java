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

package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.OracleResourceProvider;
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.spi.UsernameProvider;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies {@link VaultSEPSProvider}
 */
public class VaultSEPSProviderTest {
  private static final PasswordProvider PASSWORD_PROVIDER =
          findProvider(PasswordProvider.class,
                  "ojdbc-provider-oci-vault-seps");

  private static final UsernameProvider USERNAME_PROVIDER =
          findProvider(UsernameProvider.class,
                  "ojdbc-provider-oci-vault-seps");

  /**
   * Utility method to get common test parameters for SEPS Wallet Provider
   * tests.
   */
  private Map<String, CharSequence> getCommonParameters() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "config-file");
    testParameters.put("configFile",
            TestProperties.getOrAbort(OciTestProperty.OCI_CONFIG_FILE));
    testParameters.put("profile",
            TestProperties.getOrAbort(OciTestProperty.OCI_CONFIG_PROFILE));
    return testParameters;
  }

  /**
   * Verifies that {@link VaultSEPSProvider#getParameters()}  includes parameters
   * to configure an OCID, wallet password, and connection string index.
   */
  @Test
  public void testProviderParameters() {
    Collection<? extends OracleResourceProvider.Parameter> usernameParameters =
            USERNAME_PROVIDER.getParameters();
    Collection<? extends OracleResourceProvider.Parameter> passwordParameters =
            PASSWORD_PROVIDER.getParameters();

    // Verify both providers have the same parameters
    assertEquals(usernameParameters, passwordParameters,
            "Username and Password providers should have identical parameters");

    assertNotNull(usernameParameters);

    assertNotNull(passwordParameters);

    OracleResourceProvider.Parameter ocidParameter =
            usernameParameters.stream()
                    .filter(parameter -> "ocid".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(ocidParameter.isSensitive());
    assertTrue(ocidParameter.isRequired());
    assertNull(ocidParameter.defaultValue());

    OracleResourceProvider.Parameter walletPasswordParameter =
            usernameParameters.stream()
                    .filter(parameter -> "walletPassword".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertTrue(walletPasswordParameter.isSensitive());
    assertFalse(walletPasswordParameter.isRequired());
    assertNull(walletPasswordParameter.defaultValue());

    OracleResourceProvider.Parameter connStringParameter =
            usernameParameters.stream()
                    .filter(parameter -> "connectionStringIndex".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(connStringParameter.isSensitive());
    assertFalse(connStringParameter.isRequired());
    assertNull(connStringParameter.defaultValue());
  }

  @Test
  public void testPKCS12PasswordWithConnectionStringIndex() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_OCID));
    testParameters.put("walletPassword",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_PASSWORD));
    testParameters.put("connectionStringIndex",
            TestProperties.getOrAbort(OciTestProperty.OCI_SEPS_CONNECTION_STRING_INDEX));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PASSWORD_PROVIDER, testParameters);

    char[] password = PASSWORD_PROVIDER.getPassword(parameterValues);
    assertNotNull(password);
  }

  @Test
  public void testPKCS12PasswordWithoutConnectionStringIndex() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_OCID));
    testParameters.put("walletPassword",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_PASSWORD));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PASSWORD_PROVIDER, testParameters);

    char[] password = PASSWORD_PROVIDER.getPassword(parameterValues);
    assertNotNull(password);
  }

  @Test
  public void testSSOPasswordWithConnectionStringIndex() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_SSO_SEPS_WALLET_OCID));
    testParameters.put("connectionStringIndex",
            TestProperties.getOrAbort(OciTestProperty.OCI_SEPS_CONNECTION_STRING_INDEX));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PASSWORD_PROVIDER, testParameters);

    char[] password = PASSWORD_PROVIDER.getPassword(parameterValues);
    assertNotNull(password);
  }

  @Test
  public void testSSOPasswordWithoutConnectionStringIndex() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_SSO_SEPS_WALLET_OCID));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PASSWORD_PROVIDER, testParameters);

    char[] password = PASSWORD_PROVIDER.getPassword(parameterValues);
    assertNotNull(password);
  }

  @Test
  public void testPKCS12UsernameWithConnectionStringIndex() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_OCID));
    testParameters.put("walletPassword",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_PASSWORD));
    testParameters.put("connectionStringIndex",
            TestProperties.getOrAbort(OciTestProperty.OCI_SEPS_CONNECTION_STRING_INDEX));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(USERNAME_PROVIDER, testParameters);

    String username = USERNAME_PROVIDER.getUsername(parameterValues);
    assertNotNull(username);
  }

  @Test
  public void testPKCS12UsernameWithoutConnectionStringIndex() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_OCID));

    testParameters.put("walletPassword",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_PASSWORD));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(USERNAME_PROVIDER, testParameters);

    String username = USERNAME_PROVIDER.getUsername(parameterValues);
    assertNotNull(username);
  }

  @Test
  public void testSSOUsernameWithConnectionStringIndex() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_SSO_SEPS_WALLET_OCID));
    testParameters.put("connectionStringIndex",
            TestProperties.getOrAbort(OciTestProperty.OCI_SEPS_CONNECTION_STRING_INDEX));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(USERNAME_PROVIDER, testParameters);

    String username = USERNAME_PROVIDER.getUsername(parameterValues);
    assertNotNull(username);
  }

  @Test
  public void testSSOUsernameWithoutConnectionStringIndex() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_SSO_SEPS_WALLET_OCID));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(USERNAME_PROVIDER, testParameters);

    String username = USERNAME_PROVIDER.getUsername(parameterValues);
    assertNotNull(username);
  }

  @Test
  public void testPKCS12WalletMissingPassword() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid", TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_SEPS_WALLET_OCID));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues
            = createParameterValues(PASSWORD_PROVIDER, testParameters);

    assertThrows(IllegalStateException.class, () -> {
      PASSWORD_PROVIDER.getPassword(parameterValues);
    });
  }

  @Test
  public void testCorruptedBase64SEPSWallet() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid", TestProperties.getOrAbort(OciTestProperty.OCI_CORRUPTED_SEPS_WALLET_OCID));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PASSWORD_PROVIDER, testParameters);

    assertThrows(IllegalStateException.class, () -> {
      PASSWORD_PROVIDER.getPassword(parameterValues);
    });
  }
}