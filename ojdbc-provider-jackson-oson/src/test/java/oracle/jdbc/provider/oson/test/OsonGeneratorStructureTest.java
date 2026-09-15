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

package oracle.jdbc.provider.oson.test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue.OracleJsonType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies serialization and deserialization of object and list properties
 * with the Jackson OSON generator.
 *
 * <p>These database-independent tests cover empty, non-empty, and null list
 * properties as well as null and non-null object properties. Each valid case
 * inspects the physical OSON type and deserializes the bytes to verify that
 * the original value is preserved. Invalid object output is also checked to
 * ensure that incomplete JSON structures are rejected.</p>
 */
class OsonGeneratorStructureTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new OsonFactory());

  /**
   * Verifies the encoding and round-trip behavior of an empty list property.
   *
   * <p>The list must be represented as an OSON {@link OracleJsonType#ARRAY}
   * with no elements. Closing the empty array must not be rejected, and
   * deserialization must restore an empty list.</p>
   */
  @Test
  void emptyListIsEncodedAsArrayAndRoundTrips() throws IOException {
    ImagePayload original = new ImagePayload();
    original.images = new ArrayList<>();

    byte[] oson = serialize(original);
    OracleJsonObject document = readObject(oson);

    assertEquals(OracleJsonType.ARRAY, document.get("images").getOracleJsonType());

    ImagePayload roundTripped = MAPPER.readValue(oson, ImagePayload.class);
    assertNotNull(roundTripped.images);
    assertTrue(roundTripped.images.isEmpty());
  }

  /**
   * Verifies the encoding and round-trip behavior of a non-empty list property.
   *
   * <p>The test object contains one nested {@link Image}. The property must be
   * represented as an OSON {@link OracleJsonType#ARRAY}, and the nested image
   * must retain its URL after deserialization.</p>
   */
  @Test
  void nonEmptyListIsEncodedAsArrayAndRoundTrips() throws IOException {
    ImagePayload original = new ImagePayload();
    original.images = Collections.singletonList(new Image("poster.jpg"));

    byte[] oson = serialize(original);
    OracleJsonObject document = readObject(oson);

    assertEquals(OracleJsonType.ARRAY, document.get("images").getOracleJsonType());

    ImagePayload roundTripped = MAPPER.readValue(oson, ImagePayload.class);
    assertNotNull(roundTripped.images);
    assertEquals(1, roundTripped.images.size());
    assertEquals("poster.jpg", roundTripped.images.get(0).url);
  }

  /**
   * Verifies the encoding and round-trip behavior of a null list property.
   *
   * <p>A Java {@code null} list must be represented as an OSON
   * {@link OracleJsonType#NULL}, rather than as an empty array, and must be
   * restored as {@code null} after deserialization.</p>
   */
  @Test
  void nullListIsEncodedAsNullAndRoundTrips() throws IOException {
    ImagePayload original = new ImagePayload();
    original.images = null;

    byte[] oson = serialize(original);
    OracleJsonObject document = readObject(oson);

    assertEquals(OracleJsonType.NULL, document.get("images").getOracleJsonType());

    ImagePayload roundTripped = MAPPER.readValue(oson, ImagePayload.class);
    assertNull(roundTripped.images);
  }

  /**
   * Verifies the encoding and round-trip behavior of a null object property.
   *
   * <p>A Java {@code null} object property must be written directly as an OSON
   * {@link OracleJsonType#NULL}. No child object context is created, and the
   * property must remain {@code null} after deserialization.</p>
   */
  @Test
  void nullObjectIsEncodedAsNullAndRoundTrips() throws IOException {
    ImagePayload original = new ImagePayload();
    original.image = null;

    byte[] oson = serialize(original);
    OracleJsonObject document = readObject(oson);

    assertEquals(OracleJsonType.NULL, document.get("image").getOracleJsonType());

    ImagePayload roundTripped = MAPPER.readValue(oson, ImagePayload.class);
    assertNull(roundTripped.image);
  }

  /**
   * Verifies the encoding and round-trip behavior of a non-null object followed
   * by an array property in the same document.
   *
   * <p>The nested {@link Image} must be represented as an OSON
   * {@link OracleJsonType#OBJECT}, and the following empty list must be
   * represented as an OSON {@link OracleJsonType#ARRAY}. This sequence checks
   * that closing the nested object restores the parent context before the
   * following array is written and that both values survive deserialization.</p>
   */
  @Test
  void nonNullObjectIsEncodedAsObjectAndRoundTripsWithFollowingArray() throws IOException {
    ImagePayload original = new ImagePayload();
    original.image = new Image("poster.jpg");
    original.images = new ArrayList<>();

    byte[] oson = serialize(original);
    OracleJsonObject document = readObject(oson);

    assertEquals(OracleJsonType.OBJECT, document.get("image").getOracleJsonType());
    assertEquals(OracleJsonType.ARRAY, document.get("images").getOracleJsonType());

    ImagePayload roundTripped = MAPPER.readValue(oson, ImagePayload.class);
    assertNotNull(roundTripped.image);
    assertEquals("poster.jpg", roundTripped.image.url);
    assertNotNull(roundTripped.images);
    assertTrue(roundTripped.images.isEmpty());
  }

  /**
   * Verifies that an object with a field name but no value is rejected.
   *
   * <p>The generator writes an object start marker and a field name, then
   * attempts to close the object without writing the field value. The close
   * operation must fail because a valid object cannot end while a value is
   * pending.</p>
   */
  @Test
  void objectWithMissingValueIsRejected() throws IOException {
    OsonFactory factory = new OsonFactory();
    JsonGenerator generator = factory.createGenerator(new ByteArrayOutputStream());
    try {
      generator.writeStartObject();
      generator.writeFieldName("image");

      IOException failure = assertThrows(IOException.class, generator::writeEndObject);
      assertTrue(failure.getMessage().contains("expecting end object"));
    } finally {
      try {
        generator.close();
      } catch (Exception ignored) {
        // The intentionally incomplete object can also fail during generator close.
      }
    }
  }

  /**
   * Serializes a test container with the Jackson OSON mapper.
   *
   * @param value container to serialize
   * @return binary OSON representation of the container
   * @throws IOException if serialization fails
   */
  private static byte[] serialize(ImagePayload value) throws IOException {
    return MAPPER.writeValueAsBytes(value);
  }

  /**
   * Parses binary OSON bytes as an object so individual property types can be
   * inspected without converting them to Jackson values first.
   *
   * @param oson binary OSON representation of a container
   * @return parsed OSON object
   */
  private static OracleJsonObject readObject(byte[] oson) {
    return new OracleJsonFactory()
        .createJsonBinaryValue(ByteBuffer.wrap(oson))
        .asJsonObject();
  }

  /**
   * POJO containing one image property and one list of images.
   */
  public static class ImagePayload {
    public Image image;
    public List<Image> images;
  }

  /**
   * Nested object used by the structure tests.
   */
  public static class Image {
    public String url;

    public Image() {
    }

    Image(String url) {
      this.url = url;
    }
  }
}
