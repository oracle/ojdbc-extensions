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

package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.provider.oci.vault.Secret;
import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.PemData;
import oracle.jdbc.spi.TlsConfigurationProvider;
import oracle.security.pki.OraclePKIProvider;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static oracle.jdbc.provider.oci.vault.SecretFactory.OCID;

/**
 * <p>
 * A provider for TCPS/TLS files used to establish secure TLS communication
 * with an Autonomous Database. The file is retrieved from OCI Vault, where
 * it is stored as a base64-encoded string. This provider supports different
 * file types including SSO, PKCS12, and PEM formats.
 * </p>
 * <p>
 * The type of the file must be explicitly specified using the {@code type}
 * parameter. Based on the type, the file may contain private keys and
 * certificates for establishing secure communication. A password is only
 * required
 * for PKCS12 or encrypted PEM files.
 * </p>
 * <p>
 * This class implements the {@link TlsConfigurationProvider} SPI defined by
 * Oracle JDBC and is designed to be instantiated via
 * {@link java.util.ServiceLoader}.
 * </p>
 */
public class VaultTCPSProvider
        extends OciResourceProvider
        implements TlsConfigurationProvider {

  /**
   * A parameter for specifying the password used to decrypt the file if it is
   * password-protected. The password is necessary for PKCS12 and encrypted
   * PEM files.
   * <p>
   * If the file is not encrypted (e.g., SSO format or non
   * password-protected PEM), this parameter can be {@code null}. It is
   * only required when dealing with password-protected files.
   * </p>
   */
  private static final oracle.jdbc.provider.parameter.Parameter<String> PASSWORD =
          oracle.jdbc.provider.parameter.Parameter.create();

  /**
   * A parameter for specifying the type of the file being used.
   * <p>
   * This parameter defines the format of the wallet and dictates how it
   * should be processed.
   * The acceptable values are:
   * <ul>
   *     <li>{@code SSO} - For Single Sign-On format.</li>
   *     <li>{@code PKCS12} - For PKCS12 keystore format, which may be
   *     password-protected.</li>
   *     <li>{@code PEM} - For PEM-encoded format, which may include
   *     encrypted or unencrypted private keys and certificates.</li>
   * </ul>
   * </p>
   * The type parameter is required to correctly parse and handle the file
   * data.
   */
  private static final oracle.jdbc.provider.parameter.Parameter<String> TYPE =
          oracle.jdbc.provider.parameter.Parameter.create();


  private static final String CERTIFICATE_TYPE = "X.509";
  private static final String ALGORITHM_TYPE = "RSA";
  private static final String PEM_KEYSTORE_TYPE = "PEM";


  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter("ocid", OCID),
          new ResourceParameter("walletPassword", PASSWORD),
          new ResourceParameter("type", TYPE)
  };


  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public VaultTCPSProvider() {
    super("vault-tls", PARAMETERS);
  }

  /**
   * Retrieves an SSLContext by loading a file from OCI Vault and configuring it
   * for secure TLS communication with Autonomous Database.
   * <p>
   * The file is stored in OCI Vault as a base64-encoded string. The type of
   * the file
   * (SSO, PKCS12, or PEM) must be explicitly provided, and the method
   * processes the file
   * data accordingly, extracting keys and certificates, and creating an
   * SSLContext.
   * </p>
   *
   * @param parameterValues The parameters required to access the file,
   * including the OCID, password (if applicable), and file type (SSO,
   * PKCS12, PEM).
   * @return An initialized SSLContext for establishing secure communications.
   * @throws IllegalStateException If the SSLContext cannot be created due to
   * errors during processing.
   */
  @Override
  public SSLContext getSSLContext(Map<Parameter, CharSequence> parameterValues) {
    try {
      ParameterSet parameterSet = parseParameterValues(parameterValues);
      Secret secret = SecretFactory
              .getInstance()
              .request(parameterSet)
              .getContent();

      byte[] fileBytes = Base64
              .getDecoder()
              .decode(secret.getBase64Secret());

      char[] password = parameterSet.getOptional(PASSWORD) != null
              ? parameterSet.getOptional(PASSWORD).toCharArray()
              : null;

      String type = parameterSet.getRequired(TYPE);
      return createSSLContext(fileBytes, password, type);
    } catch (Exception e) {
      throw new IllegalStateException
              ("Failed to create SSLContext from wallet", e);
    }
  }

  /**
   * Creates an SSLContext using the provided file bytes and optional
   * password.
   * <p>
   * Based on the specified type (SSO, PKCS12, or PEM), this method
   * processes the file data accordingly. It converts the file into a
   * KeyStore, which is then used to initialize the SSLContext for secure
   * communication. The password is only required for PKCS12 and
   * encrypted PEM files.
   * </p>
   *
   * @param fileBytes The bytes representing the wallet, decoded from base64.
   * @param password  The password for the wallet, or {@code null} if the
   * file does not require a password.
   * @param type The type of the file (PEM, PKCS12, or SSO).
   * @return An initialized SSLContext ready for secure communication.
   */
  static SSLContext createSSLContext(
          byte[] fileBytes, char[] password, String type)
          throws Exception {
      KeyStore keyStore = (PEM_KEYSTORE_TYPE.equalsIgnoreCase(type)) ?
              createPEMKeyStore(fileBytes, password) :
              loadKeyStore(fileBytes, password, type);

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
   * Loads a KeyStore for either SSO or PKCS12 format based on the specified
   * type.
   * <p>
   * This method takes the file bytes and password (if applicable) and
   * loads the corresponding KeyStore. It supports both SSO (password-less)
   * and PKCS12 (password-protected) keystores, as specified by the
   * {@code type} parameter.
   * </p>
   *
   * @param fileBytes The byte array representing the wallet file, decoded
   * from base64.
   * @param password The password for the wallet, or {@code null} for SSO
   * wallets.
   * @param type The type of the KeyStore (SSO or PKCS12).
   * @return An initialized KeyStore containing the keys and certificates
   * from the wallet.
   */
  static KeyStore loadKeyStore(
          byte[] fileBytes, char[] password, String type)
          throws IOException, GeneralSecurityException {
    try (ByteArrayInputStream fileStream =
                 new ByteArrayInputStream(fileBytes)) {
      KeyStore keyStore = KeyStore.getInstance(type,
              new OraclePKIProvider());
      keyStore.load(fileStream, password);
      return keyStore;
    }
  }

  /**
   * Creates a KeyStore from PEM-encoded data.
   * <p>
   * This method processes a PEM file that contains one or more PEM-encoded
   * sections, such as a private key, certificates, or an encrypted private
   * key. It parses these sections, decodes the data, and adds the parsed
   * private key and certificates to a newly created KeyStore.
   * </p>
   *
   * @param fileBytes The byte array containing the PEM-encoded data (e.g.,
   * private key, certificates).
   * @param password  The password to decrypt the private key, if it is
   * encrypted. Can be {@code null} for unencrypted keys.
   * @return An initialized KeyStore containing the private key and
   * associated certificates from the PEM data.
   */
  static KeyStore createPEMKeyStore(byte[] fileBytes, char[] password) throws Exception {
    List<PemData> pemDataList =
            PemData.decode(new ByteArrayInputStream(fileBytes));

    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(null, null);

    PrivateKey privateKey = null;
    List<Certificate> certificates = new ArrayList<>();
    CertificateFactory certificateFactory =
            CertificateFactory.getInstance(CERTIFICATE_TYPE);

    for (PemData pemData : pemDataList) {
      switch (pemData.label()) {
        case PRIVATE_KEY:
          PKCS8EncodedKeySpec pkcs8 = new PKCS8EncodedKeySpec(pemData.data());
          privateKey = KeyFactory
                  .getInstance(ALGORITHM_TYPE)
                  .generatePrivate(pkcs8);
          break;
        case ENCRYPTED_PRIVATE_KEY:
          EncryptedPrivateKeyInfo encryptedPrivateKeyInfo =
                  new EncryptedPrivateKeyInfo(pemData.data());
          PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
          SecretKey secretKey = SecretKeyFactory
                          .getInstance(encryptedPrivateKeyInfo.getAlgName())
                          .generateSecret(pbeKeySpec);
          privateKey = KeyFactory
                          .getInstance(ALGORITHM_TYPE)
                          .generatePrivate(encryptedPrivateKeyInfo.getKeySpec(secretKey));
          break;
        case CERTIFICATE:
          Certificate certificate = certificateFactory
                  .generateCertificate(new ByteArrayInputStream(pemData.data()));

          certificates.add(certificate);
          break;
        default:
          throw new IllegalArgumentException("Unsupported PEM data label: " + pemData.label());
      }
    }

    if (privateKey != null && !certificates.isEmpty()) {
      keyStore.setKeyEntry("key", privateKey, password,
              certificates.toArray(new Certificate[0]));
    } else {
      throw new
              IllegalStateException("Missing private key or certificates in PEM data");
    }
    return keyStore;
  }

}