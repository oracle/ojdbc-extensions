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

package  oracle.jdbc.provider.util;

import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonValue;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

/** Utility methods for parsing and formatting JSON Web Token (JWT) claims */
public class JsonWebTokenParser {

  private JsonWebTokenParser() {}

  /** Oracle JDBC's JSON factory, used to parse JSON text */
  private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();

  /**
   * The maximum size of a JWT that this class will process. Defining this
   * protects against an abnormally large input that would exhaust system
   * resources. It is expected that 16kb will be enough to store any JWT,
   * however there is no true limit for the size of a JWT.
   */
  private static final int MAX_JWT_SIZE = 16_000;

  /**
   * Returns the claims of a JWT as a name=value map, where the JWT is stored in
   * a file at a given {@code filePath}.
   *
   * @param filePath The file path where the JWT file is stored
   * @return The claims of a {@code jwt} as a name=value map
   */
  public static Map<String, String> parseClaims(Path filePath) {

    final int bufferSize;
    try {
      bufferSize = requireValidSize(Files.size(filePath));
    }
    catch (IOException ioException) {
      throw new IllegalArgumentException(ioException);
    }

    char[] jwt = new char[bufferSize];
    try {
      CharBuffer buffer = CharBuffer.wrap(jwt);

      try (BufferedReader reader = Files.newBufferedReader(filePath)) {
        while (reader.read(buffer) != -1) {
          // Terminate the loop if the buffer is full. The call to read will
          // always return 0 in this case.
          if (! buffer.hasRemaining())
            break;
        }
      }
      catch (IOException ioException) {
        throw new IllegalArgumentException(
          "Failed to read " + filePath, ioException);
      }

      buffer.flip();
      return parseClaims(buffer);
    }
    finally {
      Arrays.fill(jwt, (char)0);
    }
  }

  /**
   * Returns the claims of a {@code jwt} as a name=value map.
   *
   * @param jwt The {@code jwt} payload to be parsed
   * @return The claims of a {@code jwt} as a name=value map
   */
  public static Map<String, String> parseClaims(CharSequence jwt) {
    Objects.requireNonNull(jwt, "jwt is null");
    requireValidSize(jwt.length());

    InputStream base64Stream =
      Base64.getDecoder().wrap(new PayloadInputStream(jwt));

    try (Reader payloadReader = new InputStreamReader(base64Stream, UTF_8)) {
      return JSON_FACTORY.createJsonTextValue(payloadReader)
        .asJsonObject()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
          Map.Entry::getKey,
          entry -> toString(entry.getValue())));

    }
    catch (IOException ioException) {
      throw new IllegalArgumentException("Failed to read JWT", ioException);
    }
  }

  /**
   * Parses the "exp" claim of a {@code jwt}.
   *
   * @param jwt The {@code jwt} to be parsed.
   * @return The expiration time of the token, of {@link OffsetDateTime#MAX} if
   *         the exp claim is not present. Not null.
   */
  public static OffsetDateTime parseExp(CharSequence jwt) {
    Map<String, String> claims = JsonWebTokenParser.parseClaims(jwt);

    String exp = claims.get("exp");
    if (exp == null)
      return OffsetDateTime.MAX;

    return Instant.ofEpochSecond(Long.parseLong(exp))
      .atOffset(ZoneOffset.UTC);
  }

  /** Returns a String representation of a JSON value */
  private static String toString(OracleJsonValue oracleJsonValue) {
    switch (oracleJsonValue.getOracleJsonType()) {
      case STRING:
        // OracleJsonString.toString() returns a quote enclosed "value", so
        // call getString instead
        return oracleJsonValue.asJsonString().getString();
      default:
        return oracleJsonValue.toString();
    }
  }

  /**
   * Returns a {@code size} if it is no greater than an expected maximum, or
   * throws an exception if it is not.
   */
  private static int requireValidSize(long size) {
    if (size > MAX_JWT_SIZE) {
      throw new IllegalArgumentException(format(
        "JWT exceeds maximum size of %d bytes", MAX_JWT_SIZE));
    }

    return (int)size;
  }

  /**
   * Returns an exception indicating that a valid JWT format was not identified
   */
  private static IllegalArgumentException invalidSyntax() {
    return new IllegalArgumentException(
      "JWT does not match expected syntax:" +
        " {header}.{payload}.[signature]");
  }

  /** Streams the base 64 encoded payload of a JWT */
  private static final class PayloadInputStream extends InputStream {

    private final CharBuffer payloadBuffer;

    /**
     * Constructs an instance of this class.
     * @param jwt The {@code jwt} to be streamed
     */
    PayloadInputStream(CharSequence jwt) {

      int headerEnd = indexOf('.', 0, jwt);

      if (headerEnd == -1)
        throw invalidSyntax();

      int payloadEnd = indexOf('.', headerEnd + 1, jwt);

      if (payloadEnd == -1)
        throw invalidSyntax();

      this.payloadBuffer = CharBuffer.wrap(jwt, headerEnd + 1, payloadEnd);
    }

    /** Returns 8-bit encoding of the base 64 character in JWT payload */
    @Override
    public int read() throws IOException {
      return payloadBuffer.hasRemaining()
        ? payloadBuffer.get() & 0xFF
        : -1;
    }

    /**
     * Returns the next index of a {@code charValue} in a {@code charSequence},
     * starting from a {@code fromIndex}.
     */
    private static int indexOf(
      char charValue, int fromIndex, CharSequence charSequence) {

      for (int i = fromIndex; i < charSequence.length(); i++) {
        if (charSequence.charAt(i) == charValue)
          return i;
      }

      return -1;
    }
  }

}
