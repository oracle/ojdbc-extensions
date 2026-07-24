/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Random;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies error handling in {@link TlsUtils} for malformed or incomplete TLS
 * configuration inputs.
 */
public class TlsUtilsTest {

  @Test
  public void testEncryptedPemRequiresPasswordWhenNull() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> TlsUtils.createPEMKeyStore(createEncryptedPrivateKeyPem(), null));

    assertEquals("Encrypted PEM private key requires a password", exception.getMessage());
  }

  @Test
  public void testEncryptedPemRequiresPasswordWhenEmpty() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> TlsUtils.createPEMKeyStore(
        createEncryptedPrivateKeyPem(), new char[0]));

    assertEquals("Encrypted PEM private key requires a password",
      exception.getMessage());
  }

  /**
   * Creates PEM text containing an encrypted private key label with deterministic
   * test bytes, which is sufficient to exercise pre-decryption validation.
   *
   * @return ASCII bytes of a PEM document containing an encrypted private key
   * label.
   */
  private static byte[] createEncryptedPrivateKeyPem() {
    byte[] encryptedKey = new byte[64];
    new Random(0L).nextBytes(encryptedKey);
    String pemString = String.join("\n",
      "-----BEGIN ENCRYPTED PRIVATE KEY-----",
      Base64.getMimeEncoder().encodeToString(encryptedKey),
      "-----END ENCRYPTED PRIVATE KEY-----");
    return pemString.getBytes(US_ASCII);
  }
}
