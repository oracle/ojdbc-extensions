package oracle.jdbc.provider.util;

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
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Common operations related to Transport Layer Security (TLS) in Java.
 */
public final class TlsUtils {

  private static final String PEM_KEYSTORE_TYPE = "PEM";

  private TlsUtils() {}

  /**
   * Creates a {@code KeyStore} loaded with the contents of a given stream.
   *
   * @param inputStream Contents of the key store. Not null.
   * @param password Password for the key store. May be null if the store is not
   * password protected.
   * @param type The type of the key store, such as: "JKS", "SSO", or "PKCS12".
   * Not null.
   * @param provider The provider of the KeyStore. May be null to use the most
   * preferred {@linkplain Security#getProviders() provider} for the given
   * key store type.
   * @return The loaded KeyStore.
   * @throws IllegalStateException If failed to load KeyStore.
   *
   */
  public static KeyStore loadKeyStore(
    InputStream inputStream, char[] password, String type, Provider provider) {
    try {
      KeyStore keyStore = provider == null
        ? KeyStore.getInstance(type)
        : KeyStore.getInstance(type, provider);
      keyStore.load(inputStream, password);
      return keyStore;
    }
    catch (IOException | GeneralSecurityException exception) {
      throw new IllegalStateException("Failed to load KeyStore", exception);
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
   * @throws Exception If an error occurs while creating the KeyStore.
   */
  public static KeyStore createPEMKeyStore(
    byte[] fileBytes, char[] password) throws Exception {
    List<PemData> pemDataList =
      PemData.decode(new ByteArrayInputStream(fileBytes));

    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(null, null);

    PrivateKey privateKey = null;
    List<Certificate> certificates = new ArrayList<>();
    CertificateFactory certificateFactory =
      CertificateFactory.getInstance("X.509");

    for (PemData pemData : pemDataList) {
      switch (pemData.label()) {
        case PRIVATE_KEY:
          PKCS8EncodedKeySpec pkcs8 = new PKCS8EncodedKeySpec(pemData.data());
          privateKey = KeyFactory
            .getInstance("RSA")
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
            .getInstance("RSA")
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

  /**
   * Creates an {@code SSLContext} initialized with key and trust material
   *
   * @param keyStore Key material. May be null.
   * @param trustStore Trust material. May be null.
   * @param password Password for the key material. May be null.
   * @return An SSLContext initialized with the key and trust material. Not
   * null.
   * @throws IllegalStateException If the SSLContext cannot be initialized.
   */
  public static SSLContext createSSLContext(
    KeyStore keyStore, KeyStore trustStore, char[] password) {
    try {
      KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, password);

      TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(
          TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);

      // Use TLS 1.2 or newer. Older versions are less secure.
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      sslContext.init(
        keyManagerFactory.getKeyManagers(),
        trustManagerFactory.getTrustManagers(),
        null/*Use default SecureRandom*/);
      return sslContext;
    }
    catch (GeneralSecurityException generalSecurityException) {
      throw new IllegalStateException(
        "Failed initialize SSLContext", generalSecurityException);
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
   * @param fileBytes The bytes representing the file, decoded from base64.
   * @param password  The password for the file, or {@code null} if the
   * file does not require a password.
   * @param type The type of the file (PEM, PKCS12, or SSO).
   * @return An initialized SSLContext ready for secure communication.
   */
  public static SSLContext createSSLContext(
          byte[] fileBytes, char[] password, String type)
          throws Exception {
    KeyStore keyStore = (PEM_KEYSTORE_TYPE.equalsIgnoreCase(type)) ?
            createPEMKeyStore(fileBytes, password) :
            loadKeyStore(fileBytes, password, type);
    return createSSLContext(keyStore, keyStore, password);
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
   * @param fileBytes The byte array representing the file, decoded
   * from base64.
   * @param password The password for the file, or {@code null} for SSO
   * files.
   * @param type The type of the KeyStore (SSO or PKCS12).
   * @return An initialized KeyStore containing the keys and certificates
   * from the file.
   */
  static KeyStore loadKeyStore(
          byte[] fileBytes, char[] password, String type)
          throws IOException {
    try (ByteArrayInputStream fileStream =
                 new ByteArrayInputStream(fileBytes)) {
      return loadKeyStore(
              fileStream, password, type, new OraclePKIProvider());
    }
  }
}
