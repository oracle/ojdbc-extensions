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
package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A custom deserializer for objects(Except from java.* package) implementing the `Serializable` interface.
 * This deserializer reads binary data from the JSON input and reconstructs the object
 * using Java's `ObjectInputStream`.
 */
public class OsonSerializableDeserializer extends JsonDeserializer<Object> {
  /**
   * Singleton instance for reuse.
   */
  public static final OsonSerializableDeserializer INSTANCE = new OsonSerializableDeserializer();

  /**
   * Default constructor.
   */
  OsonSerializableDeserializer() {}

  /**
   * Deserializes binary data from JSON input into a `Serializable` object.
   *
   * <p>The method reads binary data, interprets it as a serialized object, and reconstructs the
   * object using an `ObjectInputStream`.</p>
   *
   * @param p      the JSON parser
   * @param ctxt   the deserialization context
   * @return the deserialized `Serializable` object, or `null` if no data is present
   * @throws IOException if an I/O error occurs during deserialization
   * @throws JacksonException if a JSON parsing error occurs
   */
  @Override
  public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
    byte[] data = p.getBinaryValue();
    if (data == null) {
      return null;
    }
    try (ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(data))) {
      return (Serializable) objectStream.readObject(); // Deserialize the object
    } catch (ClassNotFoundException e) {
      throw new IOException("Class not found during deserialization", e);
    }
  }
}
