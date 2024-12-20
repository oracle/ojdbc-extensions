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

package oracle.jdbc.provider.oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * A custom serializer for serializing objects that implement `Serializable` to a byte array for JSON representation.
 * This serializer converts an object into its serialized byte form and writes it as a binary value in the JSON output.
 */
public class OsonSerializableSerializer extends JsonSerializer<Object> {

  /**
   * A singleton instance of the `OsonSerializableSerializer` for global use.
   */
  public final static OsonSerializableSerializer INSTANCE = new OsonSerializableSerializer();

  /**
   * Default constructor.
   */
  public OsonSerializableSerializer() {}

  /**
   * Serializes a `Serializable` object into its byte array representation and writes it as a binary value in the JSON.
   * If the object is `null`, a `null` value is written.
   *
   * @param value the object to serialize
   * @param gen the JSON generator used for serialization
   * @param serializers the serializer provider
   * @throws IOException if an I/O error occurs during serialization
   */
  @Override
  public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    if (value == null) {
      gen.writeNull();
    }
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
      objectStream.writeObject(value); // Serialize object to byte array
    }

    byte[] bytes = byteStream.toByteArray();
    gen.writeBinary(bytes);
  }
}
