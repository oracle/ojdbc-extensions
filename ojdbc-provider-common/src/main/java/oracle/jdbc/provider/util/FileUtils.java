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

package oracle.jdbc.provider.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for common file handling operations.
 */
public final class FileUtils {

  /**
   * Checks if the given byte array is Base64-encoded.
   */
  public static boolean isBase64Encoded(byte[] secretBytes) {
    try {
      Base64.getDecoder().decode(secretBytes);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Decodes the given secret bytes if base64 encoded, otherwise returns them as-is.
   *
   * @param input the secret bytes
   * @return the decoded byte array if base64, or the original byte array
   */
  public static byte[] decodeIfBase64(byte[] input) {
    return isBase64Encoded(input) ? Base64.getDecoder().decode(input)
            : input;
  }

  /**
   * Converts a secret string to a Base64-encoded char array.
   * If the secret is already Base64-encoded, it is returned as a char array.
   * Otherwise, it is encoded to Base64.
   *
   * @param secretString The secret string to process
   * @return A char array containing the Base64-encoded secret,
   * or null if the input is null
   */
  public static char[] toBase64EncodedCharArray(String secretString) {
    if (secretString == null) {
      return null;
    }
    byte[] secretBytes = secretString.getBytes(StandardCharsets.UTF_8);
    if (isBase64Encoded(secretBytes)) {
      return secretString.toCharArray();
    } else {
      return Base64.getEncoder().encodeToString(secretBytes).toCharArray();
    }
  }
}
