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
import oracle.jdbc.spi.TlsConfigurationProvider;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class HcpVaultTCPSProviderTest {

  private static final TlsConfigurationProvider PROVIDER = findProvider(
    TlsConfigurationProvider.class, "ojdbc-provider-hcpvault-secrets-tls");

  @Test
  public void testGetParameters() {
    Collection<? extends Parameter> parameters = PROVIDER.getParameters();
    assertNotNull(parameters);

    Parameter secretNameParameter =
      parameters.stream()
        .filter(parameter -> "secretName".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(secretNameParameter.isRequired());
    assertNull(secretNameParameter.defaultValue());

    Parameter orgIdParameter =
      parameters.stream()
        .filter(parameter -> "orgId".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(orgIdParameter.isRequired());
    assertNull(orgIdParameter.defaultValue());

    Parameter appNameParameter =
      parameters.stream()
        .filter(parameter -> "appName".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(appNameParameter.isRequired());
    assertNull(appNameParameter.defaultValue());

    Parameter projectIdParameter =
      parameters.stream()
        .filter(parameter -> "projectId".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(projectIdParameter.isRequired());
    assertNull(projectIdParameter.defaultValue());

    Parameter typeParameter =
      parameters.stream()
        .filter(parameter -> "type".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(typeParameter.isRequired());
    assertNull(typeParameter.defaultValue());

    Parameter walletPasswordParameter =
      parameters.stream()
        .filter(parameter -> "walletPassword".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(walletPasswordParameter.isSensitive());
    assertFalse(walletPasswordParameter.isRequired());
    assertNull(walletPasswordParameter.defaultValue());
  }

  @Test
  public void testRetrieveSSLContextFromPKCS12() {
    Map<String, String> testParams = new HashMap<>();
    testParams.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_P12_SECRET_NAME));
    testParams.put("type", "PKCS12");
    testParams.put("walletPassword", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_PASSWORD));
    HcpVaultTestUtil.configureAuthentication(testParams);

    Map<Parameter, CharSequence> values = createParameterValues(PROVIDER, testParams);
    SSLContext context = PROVIDER.getSSLContext(values);
    assertNotNull(context);
  }

  @Test
  public void testRetrieveSSLContextFromSSO() {
    Map<String, String> testParams = new HashMap<>();
    testParams.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_SSO_SECRET_NAME));
    testParams.put("type", "SSO");
    HcpVaultTestUtil.configureAuthentication(testParams);

    Map<Parameter, CharSequence> values = createParameterValues(PROVIDER, testParams);
    SSLContext context = PROVIDER.getSSLContext(values);
    assertNotNull(context);
  }

  @Test
  public void testRetrieveSSLContextFromPEM() {
    Map<String, String> testParams = new HashMap<>();
    testParams.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_PEM_SECRET_NAME));
    testParams.put("type", "PEM");
    testParams.put("walletPassword", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_PASSWORD));
    HcpVaultTestUtil.configureAuthentication(testParams);

    Map<Parameter, CharSequence> values = createParameterValues(PROVIDER, testParams);
    SSLContext context = PROVIDER.getSSLContext(values);
    assertNotNull(context);
  }

  @Test
  public void testMissingPasswordForPKCS12() {
    Map<String, String> testParams = new HashMap<>();
    testParams.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_P12_SECRET_NAME));
    testParams.put("type", "PKCS12");
    HcpVaultTestUtil.configureAuthentication(testParams);

    Map<Parameter, CharSequence> values = createParameterValues(PROVIDER, testParams);
    assertThrows(IllegalStateException.class, () -> PROVIDER.getSSLContext(values));
  }

  @Test
  public void testMissingPasswordForPEM() {
    Map<String, String> testParams = new HashMap<>();
    testParams.put("secretName", TestProperties.getOrAbort(HcpVaultTestProperty.WALLET_PEM_SECRET_NAME));
    testParams.put("type", "PEM");
    HcpVaultTestUtil.configureAuthentication(testParams);

    Map<Parameter, CharSequence> values = createParameterValues(PROVIDER, testParams);
    assertThrows(IllegalStateException.class, () -> PROVIDER.getSSLContext(values));
  }
}