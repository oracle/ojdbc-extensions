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

package oracle.jdbc.provider.oci.vault;

import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.model.SecretBundle;
import com.oracle.bmc.secrets.model.SecretBundleContentDetails;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A secret managed by the OCI Vault service.
 */
public final class Secret {

  private final String base64Secret;

  private Secret(String base64Secret) {
    this.base64Secret = base64Secret;
  }

  static Secret fromSecretBundle(SecretBundle secretBundle) {

    SecretBundleContentDetails secretBundleContentDetails =
        secretBundle.getSecretBundleContent();

    if (secretBundleContentDetails instanceof
        Base64SecretBundleContentDetails) {

      String base64Secret =
          ((Base64SecretBundleContentDetails)secretBundleContentDetails)
              .getContent();

      return new Secret(base64Secret);
    }
    else {
      throw new IllegalStateException(
          "Unsupported content type: " + secretBundleContentDetails.getClass());
    }
  }

  /**
   * Returns the secret decoded as UTF-8 characters. The {@code char[]} returned
   * by this method is not retained: It's contents may be wiped from memory
   * after it has been consumed.
   * @return Characters representing the UTF-8 decoding of the secret. Not null.
   */
  public char[] toCharArray() {
    byte[] contentBytes = Base64.getDecoder().decode(base64Secret);
    try {
      CharBuffer contentBuffer = UTF_8.decode(ByteBuffer.wrap(contentBytes));
      char[] contentChars = new char[contentBuffer.remaining()];
      try {
        contentBuffer.get(contentChars);
        return contentChars;
      }
      finally {
        contentBuffer.clear();
        contentBuffer.put(new char[contentBuffer.capacity()]);
      }
    }
    finally {
      Arrays.fill(contentBytes, (byte)0);
    }
  }

  /**
   * @return String that represents the Secret in base64 format
   */
  public String getBase64Secret() {
    return base64Secret;
  }
}
