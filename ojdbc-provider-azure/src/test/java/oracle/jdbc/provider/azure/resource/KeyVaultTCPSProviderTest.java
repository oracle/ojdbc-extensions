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

package oracle.jdbc.provider.azure.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.spi.OracleResourceProvider;
import oracle.jdbc.spi.TlsConfigurationProvider;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class KeyVaultTCPSProviderTest {
  private static final TlsConfigurationProvider PROVIDER =
          findProvider(
            TlsConfigurationProvider.class, "ojdbc-provider-azure-key-vault-tls");

  /**
   * Verifies that {@link KeyVaultTCPSProvider#getParameters()}  includes parameters
   * to configure a vault URL, secret name, wallet password, and type.
   */
  @Test
  public void testGetParameters() {
    Collection<? extends OracleResourceProvider.Parameter> parameters = PROVIDER.getParameters();
    assertNotNull(parameters);

    OracleResourceProvider.Parameter vaultUrlParameter =
            parameters.stream()
                    .filter(parameter -> "vaultUrl".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(vaultUrlParameter.isSensitive());
    assertTrue(vaultUrlParameter.isRequired());
    assertNull(vaultUrlParameter.defaultValue());

    OracleResourceProvider.Parameter secretNameParameter =
            parameters.stream()
                    .filter(parameter -> "secretName".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(secretNameParameter.isSensitive());
    assertTrue(secretNameParameter.isRequired());
    assertNull(secretNameParameter.defaultValue());

    OracleResourceProvider.Parameter typeParameter =
            parameters.stream()
                    .filter(parameter -> "type".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(typeParameter.isSensitive());
    assertTrue(typeParameter.isRequired());
    assertNull(typeParameter.defaultValue());

    OracleResourceProvider.Parameter walletPasswordParameter =
            parameters.stream()
                    .filter(parameter -> "walletPassword".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertTrue(walletPasswordParameter.isSensitive());
    assertFalse(walletPasswordParameter.isRequired());
    assertNull(walletPasswordParameter.defaultValue());
  }

  @Test
  public void testPKCS12TcpsWallet() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_PKCS12_TLS_WALLET_SECRET_NAME));
    testParameters.put("type", "PKCS12");
    testParameters.put("walletPassword",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_PKCS12_TLS_WALLET_PASSWORD));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext sslContext = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(sslContext);
  }

  @Test
  public void testSSOTcpsWallet() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_SSO_TLS_WALLET_SECRET_NAME));
    testParameters.put("type", "SSO");

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext sslContext = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(sslContext);
  }

  @Test
  public void testPEMTcpsWallet() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_PEM_TLS_WALLET_SECRET_NAME));
    testParameters.put("type", "PEM");
    testParameters.put("walletPassword",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_PEM_TLS_WALLET_PASSWORD));

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext sslContext = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(sslContext);
  }

  @Test
  public void testPKCS12MissingPassword() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_PKCS12_TLS_WALLET_SECRET_NAME));
    testParameters.put("type", "PKCS12");

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, testParameters);

    assertThrows(IllegalStateException.class, () -> {
      PROVIDER.getSSLContext(parameterValues);
    });
  }

  @Test
  public void testPemMissingPassword() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_PEM_TLS_WALLET_SECRET_NAME));
    testParameters.put("type", "PEM");

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, testParameters);

    assertThrows(IllegalStateException.class, () -> {
      PROVIDER.getSSLContext(parameterValues);
    });
  }

  @Test
  public void testCorruptedBase64TCPSWallet() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_CORRUPTED_TLS_WALLET_SECRET_NAME));
    testParameters.put("type", "SSO");

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, testParameters);

    assertThrows(IllegalStateException.class, () -> {
      PROVIDER.getSSLContext(parameterValues);
    });
  }
}
