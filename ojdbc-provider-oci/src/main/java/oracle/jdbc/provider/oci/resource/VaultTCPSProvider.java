/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to
 *  any
 ** person obtaining a copy of this software, associated documentation and/or
 *  data
 ** (collectively the "Software"), free of charge and under any and all
 * copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the
 * Software
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
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.provider.oci.vault.Secret;
import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.spi.TlsConfigurationProvider;
import oracle.security.pki.OraclePKIProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Map;

import static oracle.jdbc.provider.oci.vault.SecretFactory.OCID;

/**
 * <p>
 * A provider for TCPS/TLS wallets used to establish secure (mTLS) communication
 * with an Autonomous Database. The wallet is retrieved from OCI Vault, where
 * it is stored
 * as a base64-encoded string. If a password is provided, the wallet is
 * assumed to be in
 * PKCS12 format, otherwise it defaults to SSO format.
 * </p><p>
 * This class implements the {@link TlsConfigurationProvider} SPI defined by
 * Oracle JDBC.
 * It is designed to be located and instantiated by
 * {@link java.util.ServiceLoader}.
 * </p><p>
 * The wallet can be in either SSO or PKCS12 format:
 * <ul>
 *     <li>If a password is provided via the {@code walletPassword} parameter,
 *     the wallet is treated as a PKCS12 keystore.</li>
 *     <li>If no password is provided, the wallet is treated as an SSO
 *     keystore.</li>
 * </ul>
 * </p>
 */
public class VaultTCPSProvider
        extends OciResourceProvider
        implements TlsConfigurationProvider {


  private static final oracle.jdbc.provider.parameter.Parameter<String> PASSWORD =
          oracle.jdbc.provider.parameter.Parameter.create();
  private static final String SSO_KEYSTORE_TYPE = "SSO";
  private static final String PKCS12_KEYSTORE_TYPE = "PKCS12";


  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter("ocid", OCID),
          new ResourceParameter("walletPassword", PASSWORD)
  };


  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public VaultTCPSProvider() {
    super("vault-tls", PARAMETERS);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Retrieves an SSLContext by loading a wallet from the OCI Vault,
   * configuring it for mTLS (mutual TLS) communication with Autonomous
   * Database. The wallet is stored in
   * the OCI Vault as a base64-encoded string, and this method decodes it
   * before loading it into a keystore.
   * </p>
   *
   * @return An initialized SSLContext for establishing secure communications.
   * @throws IllegalStateException If the SSLContext cannot be created due to
   * errors in the wallet loading process.
   */
  @Override
  public SSLContext getSSLContext(Map<Parameter, CharSequence> parameterValues) {
    try {
      ParameterSet parameterSet = parseParameterValues(parameterValues);
      Secret secret = SecretFactory
              .getInstance()
              .request(parameterSet)
              .getContent();

      byte[] walletBytes = Base64
              .getDecoder()
              .decode(secret.getBase64Secret());

      char[] walletPassword = parameterSet.getOptional(PASSWORD) != null
              ? parameterSet.getOptional(PASSWORD).toCharArray()
              : null;

      return createSSLContext(walletBytes, walletPassword);
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("Failed to create SSLContext from " +
              "wallet", e);
    }
  }

  /**
   * Creates an SSLContext using the provided wallet bytes and optional
   * password.
   * The wallet can be in either SSO or PKCS12 format, and this method will
   * configure
   * the SSLContext accordingly.
   *
   * @param walletBytes    The bytes representing the wallet (after decoding
   *                       from base64).
   * @param walletPassword The password for the wallet, or {@code null} if
   *                       the wallet is in SSO format.
   * @return An initialized SSLContext ready for secure communication.
   * @throws GeneralSecurityException If a security issue occurs during
   * keystore loading or SSLContext initialization.
   * @throws IOException              If an error occurs while loading the
   * wallet.
   */
  static SSLContext createSSLContext(byte[] walletBytes, char[] walletPassword)
          throws GeneralSecurityException, IOException {
    KeyStore keyStore = loadKeyStore(walletBytes, walletPassword);

    TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance("PKIX");
    KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance("PKIX");

    trustManagerFactory.init(keyStore);
    keyManagerFactory.init(keyStore, null);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(
            keyManagerFactory.getKeyManagers(),
            trustManagerFactory.getTrustManagers(),
            null);
    return sslContext;
  }

  /**
   * Loads a keystore (either SSO or PKCS12) from the given wallet bytes.
   *
   * @param walletBytes    The byte array representing the wallet file
   *                       (decoded from base64).
   * @param walletPassword The password for the wallet, or {@code null} for
   *                       SSO wallets.
   * @return An initialized KeyStore containing the keys and certificates
   * from the wallet.
   */
  static KeyStore loadKeyStore(byte[] walletBytes, char[] walletPassword)
          throws IOException, GeneralSecurityException {
    String keystoreType = walletPassword == null ? SSO_KEYSTORE_TYPE :
            PKCS12_KEYSTORE_TYPE;
    try (ByteArrayInputStream walletStream =
                 new ByteArrayInputStream(walletBytes)) {
      KeyStore keyStore = KeyStore.getInstance(keystoreType,
              new OraclePKIProvider());
      keyStore.load(walletStream, walletPassword);
      return keyStore;
    }
  }

}