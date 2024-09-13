package oracle.jdbc.provider.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PEMDataTest {

  @Test
  public void testEncodePrivateKey() throws Exception {
    PrivateKey privateKey =
      KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
    PemData pemData = PemData.encodePrivateKey(privateKey);
    assertEquals(pemData.label(), PemData.Label.PRIVATE_KEY);
    assertArrayEquals(
      privateKey.getEncoded(),
      pemData.data());

    final String pemText;
    try (InputStream inputStream = pemData.createInputStream()) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int length;
      while (-1 != (length = inputStream.read(buffer))) {
        outputStream.write(buffer, 0, length);
      }
      pemText = new String(outputStream.toByteArray(), US_ASCII);
    }

    assertEquals(
      String.join("\r\n",
        "",
        "-----BEGIN PRIVATE KEY-----",
        Base64.getMimeEncoder().encodeToString(privateKey.getEncoded()),
        "-----END PRIVATE KEY-----",
        ""),
      pemText);
  }

  /**
   * Verifies a PEM encoding similar to what the Autonomous Database service
   * includes as ewallet.pem in a wallet zip file.
   */
  @Test
  public void testDecode() throws Exception {

    // The intent here is to replicate data with the same length and tags as the
    // ewallet.pem file from ADB. The test data is just random bytes. To verify
    // PemData.decode(), the data doesn't actually need to be decoded as a
    // PrivateKey or Certificate.
    Random random = new Random();
    byte[] encryptedKey = new byte[1262];
    random.nextBytes(encryptedKey);
    byte[] certificate0 = new byte[976];
    random.nextBytes(encryptedKey);
    byte[] certificate1 = new byte[947];
    random.nextBytes(encryptedKey);
    byte[] certificate2 = new byte[982];
    random.nextBytes(encryptedKey);

    String pemString = String.join("\n",
      "-----BEGIN ENCRYPTED PRIVATE KEY-----",
      Base64.getMimeEncoder().encodeToString(encryptedKey),
      "-----END ENCRYPTED PRIVATE KEY-----",
      "-----BEGIN CERTIFICATE-----",
      Base64.getMimeEncoder().encodeToString(certificate0),
      "-----END CERTIFICATE-----",
      "-----BEGIN CERTIFICATE-----",
      Base64.getMimeEncoder().encodeToString(certificate1),
      "-----END CERTIFICATE-----",
      "-----BEGIN CERTIFICATE-----",
      Base64.getMimeEncoder().encodeToString(certificate2),
      "-----END CERTIFICATE-----");

    List<PemData> pemDataList =
      PemData.decode(new ByteArrayInputStream(pemString.getBytes(US_ASCII)));

    assertEquals(4, pemDataList.size());

    PemData privateKeyData = pemDataList.get(0);
    assertEquals(PemData.Label.ENCRYPTED_PRIVATE_KEY, privateKeyData.label());
    assertArrayEquals(
      encryptedKey,
      privateKeyData.data());

    PemData certificate0Data = pemDataList.get(1);
    assertEquals(PemData.Label.CERTIFICATE, certificate0Data.label());
    assertArrayEquals(
      certificate0,
      certificate0Data.data());

    PemData certificate1Data = pemDataList.get(2);
    assertEquals(PemData.Label.CERTIFICATE, certificate1Data.label());
    assertArrayEquals(
      certificate1,
      certificate1Data.data());

    PemData certificate2Data = pemDataList.get(3);
    assertEquals(PemData.Label.CERTIFICATE, certificate2Data.label());
    assertArrayEquals(
      certificate2,
      certificate2Data.data());

  }
}
