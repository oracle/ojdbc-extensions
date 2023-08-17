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

package oracle.jdbc.provider.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Data that is encoded in the Privacy-Enhanced Mail (PEM) format. The format is
 * specific by <a href="https://www.rfc-editor.org/rfc/rfc7468">
 * RFC 7468
 * </a>.
 */
public final class PemData {

  /** Label that identifies the type of data */
  private final Label label;

  /** Base 64 encoding of data */
  private final byte[] base64Data;

  /**
   * Constructs PEM encoded data
   * @param label Label that identifies the type of data. Not null.
   * @param base64Data Base 64 encoding of the data. Not null. Retained.
   */
  private PemData(Label label, byte[] base64Data) {
    this.label = label;
    this.base64Data = base64Data;
  }

  /**
   * Streams the ASCII encoded bytes of textual PEM encoded data.
   * @return A stream of PEM encoded data. Not null.
   */
  public InputStream createInputStream() {

    List<InputStream> inputStreams = Arrays.asList(
      new ByteArrayInputStream(label.asciiBeginLabel),
      new ByteArrayInputStream(base64Data),
      new ByteArrayInputStream(label.asciiEndLabel));

    return new SequenceInputStream(
      Collections.enumeration(inputStreams));
  }

  /**
   * Returns PEM encoded data of a
   * <a href="https://www.rfc-editor.org/rfc/rfc7468#section-10">
   * private key
   * </a>
   * @param privateKey PKCS #8 private key encoding. Not null. Not retained.
   * @return PEM encoding of the private key. Not null.
   */
  public static PemData encodePrivateKey(PrivateKey privateKey) {
    byte[] pkcs8Key = privateKey.getEncoded();
    try {
      byte[] base64Data = Base64.getMimeEncoder().encode(pkcs8Key);
      return new PemData(Label.PRIVATE_KEY, base64Data);
    }
    finally {
      Arrays.fill(pkcs8Key, (byte)0);
    }
  }

  private enum Label {

    PRIVATE_KEY;

    /**
     * The name of this label, derived from the name of this enum, with
     * underscores replaced by spaces.
     */
    private final String labelText = name().replace('_', ' ');

    /** The line that begins an encoding */
    private final String beginLabel = format("\r\n-----BEGIN %s-----\r\n", labelText);

    /** ASCII encoding of the line that begins an encoding */
    private final byte[] asciiBeginLabel = beginLabel.getBytes(US_ASCII);

    /** The line that ends an encoding */
    private final String endLabel = format("\r\n-----END %s-----\r\n", labelText);

    /** ASCII encoding of the line that ends an encoding */
    private final byte[] asciiEndLabel = endLabel.getBytes(US_ASCII);

  }

}
