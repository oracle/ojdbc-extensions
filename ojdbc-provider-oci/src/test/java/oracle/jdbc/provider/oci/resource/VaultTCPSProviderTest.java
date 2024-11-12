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

/**
 * Verifies {@link VaultTCPSProvider}
 */
public class VaultTCPSProviderTest {


  private static final TlsConfigurationProvider PROVIDER =
          findProvider(TlsConfigurationProvider.class,
                  "ojdbc-provider-oci-vault-tls");

  /**
   * Utility method to get common test parameters for TCPS Wallet Provider tests.
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
   * Verifies that {@link VaultTCPSProvider#getParameters()}  includes parameters
   * to configure an OCID, wallet password, and connection string index.
   */
  @Test
  public void testProviderParameters() {
    Collection<? extends Parameter> parameters =
            PROVIDER.getParameters();
    assertNotNull(parameters);

    OracleResourceProvider.Parameter ocidParameter =
            parameters.stream()
                    .filter(parameter -> "ocid".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(ocidParameter.isSensitive());
    assertTrue(ocidParameter.isRequired());
    assertNull(ocidParameter.defaultValue());

    OracleResourceProvider.Parameter walletPasswordParameter =
            parameters.stream()
                    .filter(parameter -> "walletPassword".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertTrue(walletPasswordParameter.isSensitive());
    assertFalse(walletPasswordParameter.isRequired());
    assertNull(walletPasswordParameter.defaultValue());

    OracleResourceProvider.Parameter connStringParameter =
            parameters.stream()
                    .filter(parameter -> "type".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(connStringParameter.isSensitive());
    assertTrue(connStringParameter.isRequired());
    assertNull(connStringParameter.defaultValue());
  }

  @Test
  public void testPKCS12TcpsWallet() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_TLS_WALLET_OCID));
    testParameters.put("type", "PKCS12");
    testParameters.put("walletPassword",
            TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_TLS_WALLET_PASSWORD));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext sslContext = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(sslContext);
  }

  @Test
  public void testSSOTcpsWallet() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_SSO_TLS_WALLET_OCID));
    testParameters.put("type", "SSO");

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext sslContext = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(sslContext);
  }

  @Test
  public void testPEMTcpsWallet() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_PEM_TLS_WALLET_OCID));
    testParameters.put("type", "PEM");
    testParameters.put("walletPassword",
            TestProperties.getOrAbort(OciTestProperty.OCI_PEM_TLS_WALLET_PASSWORD));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    SSLContext sslContext = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(sslContext);
  }

  /**
   * Negative test: Missing password for PKCS12 wallet should throw an exception.
   */
  @Test
  public void testPKCS12MissingPassword() {
    Map<String, CharSequence> parameters = getCommonParameters();
    parameters.put("ocid", TestProperties.getOrAbort(OciTestProperty.OCI_PKCS12_TLS_WALLET_OCID));
    parameters.put("type", "PKCS12");

    Map<Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, parameters);

    assertThrows(IllegalStateException.class, () -> {
      PROVIDER.getSSLContext(parameterValues);
    });
  }

  /**
   * Negative test: Missing password for PEM wallet should throw an exception.
   */
  @Test
  public void testPemMissingPassword() {
    Map<String, CharSequence> parameters = getCommonParameters();
    parameters.put("ocid", TestProperties.getOrAbort(OciTestProperty.OCI_PEM_TLS_WALLET_OCID));
    parameters.put("type", "PEM");

    Map<Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, parameters);

    assertThrows(IllegalStateException.class, () -> {
      PROVIDER.getSSLContext(parameterValues);
    });
  }

  @Test
  public void testCorruptedBase64TCPSWallet() {
    Map<String, CharSequence> testParameters = getCommonParameters();
    testParameters.put("ocid", TestProperties.getOrAbort(OciTestProperty.OCI_CORRUPTED_TLS_WALLET_OCID));
    testParameters.put("type", "SSO");

    Map<Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, testParameters);

    assertThrows(IllegalStateException.class, () -> {
      PROVIDER.getSSLContext(parameterValues);
    });
  }

}