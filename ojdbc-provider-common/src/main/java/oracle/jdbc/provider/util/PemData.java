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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Data that is encoded in the Privacy-Enhanced Mail (PEM) format. The format is
 * specific by <a href="https://www.rfc-editor.org/rfc/rfc7468">
 * RFC 7468
 * </a>.
 */
public final class PemData {

  private static final Logger LOGGER =
    Logger.getLogger(PemData.class.getName());

  /**
   * ASCII/UTF-8 encoded new line characters. PEM data decoders should
   * recognize a carriage return followed by a newline as a new line.
   */
  private static final byte[] ASCII_NEW_LINE = "\r\n".getBytes(US_ASCII);

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
      new ByteArrayInputStream(ASCII_NEW_LINE),
      new ByteArrayInputStream(label.beginTag.getBytes(US_ASCII)),
      new ByteArrayInputStream(ASCII_NEW_LINE),
      new ByteArrayInputStream(base64Data),
      new ByteArrayInputStream(ASCII_NEW_LINE),
      new ByteArrayInputStream(label.endTag.getBytes(US_ASCII)),
      new ByteArrayInputStream(ASCII_NEW_LINE));

    return new SequenceInputStream(
      Collections.enumeration(inputStreams));
  }

  /**
   * Returns the Label of this PEM data.
   *
   * @return The label for this data. Not null.
   */
  public Label label() {
    return label;
  }

  /**
   * Returns the binary data encapsulated by this PemData. The data can be
   * decoded according to the encoding specified for the {@link #label()}.
   *
   * @return Binary data. Not null.
   */
  public byte[] data() {
    return Base64.getMimeDecoder().decode(base64Data);
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

  /**
   * Decodes labeled data from ASCII encoded PEM text. Unrecognized labels are
   * ignored.
   *
   * @param pemData Stream of ASCII encoded PEM text. Not null.
   *
   * @return PEM data for labels that were recognized in the stream. Not null.
   */
  public static List<PemData> decode(InputStream pemData) {
    List<PemData> pemDataList = new ArrayList<>();

    String line;
    BufferedReader reader =
      new BufferedReader(new InputStreamReader(pemData, US_ASCII));
    try {
      while (null != (line = reader.readLine())) {

        // Parse a BEGIN tag
        String labelName = parseBeginLabelName(line);

        // Ignore data outside of a BEGIN and END tags.
        if (labelName == null)
          continue;

        // Check if the BEGIN tag has a recognized label
        Label label = Label.NAME_TO_LABEL_MAP.get(labelName);

        if (label == null) {
          LOGGER.warning("Ignoring unrecognized PEM label: " + labelName);
          // Ignore data for an unrecognized label
          parseData(reader, null, toEndTag(labelName));
        }
        else {
          // Capture data for a recognized label
          ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
          parseData(reader, dataStream, label.endTag);
          pemDataList.add(new PemData(label, dataStream.toByteArray()));
        }
      }

      return pemDataList;
    }
    catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  /**
   * Parses the label name appearing in a BEGIN tag:
   * <pre>
   * -----BEGIN {label name}-----
   * </pre>
   *
   * @param line Line to parse. Not null.
   * @return The label name, or null if the line is not a BEGIN tag.
   */
  private static String parseBeginLabelName(String line) {
    line = line.trim();

    if (!line.startsWith(Label.BEGIN_PREFIX))
      return null;

    if (!line.endsWith(Label.TAG_POSTFIX))
      return null;

    return line.substring(
      Label.BEGIN_PREFIX.length(),
      line.length() - Label.TAG_POSTFIX.length());
  }

  /**
   * Returns an END tag for a label name:
   * <pre>
   * -----END {label name}-----
   * </pre>
   *
   * @param labelName Label name appearing in the END tag. Not null.
   * @return The END tag for the label name. Not null.
   */
  private static String toEndTag(String labelName) {
    return Label.END_PREFIX + labelName + Label.TAG_POSTFIX;
  }

  /**
   * Parses data encapsulated by a BEGIN and END tag, starting with the next
   * line of a reader, and stopping when a END tag is matched.
   *
   * @param reader Reader positioned on the next line of data to parse. Not
   * null.
   *
   * @param outputStream Stream to output ASCII encoded data into. May be null
   * if data should be ignored.
   *
   * @param endTag The END tag which terminates the data lines. Not null.
   *
   * @throws IOException If an error prevents the reader from reading lines,
   * or an error prevents the output stream from writing.
   *
   * @throws IllegalStateException If no END tag is found.
   */
  private static void parseData(
    BufferedReader reader, OutputStream outputStream, String endTag)
    throws IOException  {

    String line;
    while (null != (line = reader.readLine())) {
      // Ignore whitespace
      line = line.trim();

      if (endTag.equals(line))
        break;

      if (outputStream != null)
        outputStream.write(line.getBytes(US_ASCII));
    }

    if (line == null)
      throw missingEndTag(endTag);
  }

  /**
   * Returns an exception indicating that an END tag is missing from a PEM
   * data encoding.
   *
   * @param endTag The missing tag. Not null.
   * @return Exception for the missing tag. Not null.
   */
  private static IllegalStateException missingEndTag(String endTag) {
    return new IllegalStateException("Missing END tag: " + endTag);
  }

  /**
   * Labels recognized by {@link PemData}. These labels are defined in
   * <a href="https://www.rfc-editor.org/rfc/rfc7468">
   * RFC 7468
   * </a>.
   */
  public enum Label {

    /**
     * <a href="https://www.rfc-editor.org/rfc/rfc7468#section-10">
     * Unencrypted PKCS #8 Private Key
     * </a>
     */
    PRIVATE_KEY("PRIVATE KEY"),

    /**
     * <a href="https://www.rfc-editor.org/rfc/rfc7468#section-11">
     * Encrypted PKCS #8 Private Key
     * </a>
     */
    ENCRYPTED_PRIVATE_KEY("ENCRYPTED PRIVATE KEY"),

    /**
     * <a href="https://www.rfc-editor.org/rfc/rfc7468#section-5">
     * Public-key certificate
     * </a>
     */
    CERTIFICATE("CERTIFICATE"),
    ;

    /**
     * Mapping of label names to {@link Label} members.
     */
    private static final Map<String, Label> NAME_TO_LABEL_MAP =
      Arrays.stream(values())
        .collect(Collectors.toMap(
          label -> label.labelName,
          Function.identity()));

    /** The start of a BEGIN tag, up to the where the label name appears */
    static final String BEGIN_PREFIX = "-----BEGIN ";

    /** The start of an END tag, up to the where the label name appears */
    static final String END_PREFIX = "-----END ";

    /** The end of a BEGIN or END tag, after where the label name appears */
    static final String TAG_POSTFIX = "-----";

    /**
     * The name of which appears in BEGIN and END tags for this label.
     */
    private final String labelName;

    /** The tag that begins an encoding with the name of this label */
    private final String beginTag;

    /** The tag that ends an encoding with the name of this label */
    private final String endTag;

    Label(String labelName) {
      this.labelName = labelName;
      beginTag = BEGIN_PREFIX + labelName + TAG_POSTFIX;
      endTag = toEndTag(labelName);
    }

  }

}
