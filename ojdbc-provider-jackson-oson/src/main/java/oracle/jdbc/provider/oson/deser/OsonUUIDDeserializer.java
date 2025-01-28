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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;
import oracle.jdbc.provider.oson.OsonParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * A custom deserializer for `UUID` objects that extends Jackson's `StdScalarDeserializer`.
 * This deserializer is designed to handle Oracle JSON-specific binary UUID representations
 * when used with `OsonParser`.
 *
 * <p>When encountering an embedded object token (`JsonToken.VALUE_EMBEDDED_OBJECT`) in
 * `OsonParser`, it reads the binary data, extracts the most significant and least significant
 * bits, and constructs a `UUID` object. For other scenarios, it delegates to Jackson's
 * `UUIDDeserializer`.</p>
 */
public class OsonUUIDDeserializer extends StdScalarDeserializer<UUID> {
  /**
   * Singleton instance for reuse.
   */
  public static final OsonUUIDDeserializer INSTANCE = new OsonUUIDDeserializer();
  /**
   * Fallback Jackson `UUIDDeserializer` for standard cases.
   */
  public static final UUIDDeserializer UUID_DESERIALIZER = new UUIDDeserializer();

  /**
   * Default constructor.
   */
  public OsonUUIDDeserializer() {
    super(UUID.class);
  }

  /**
   * Deserializes a JSON input into a `UUID` object.
   *
   * <p>If the parser is an instance of `OsonParser` and the current token is an embedded
   * object (`JsonToken.VALUE_EMBEDDED_OBJECT`), it interprets the binary data as a
   * serialized UUID. For all other scenarios, it delegates to the standard
   * `UUIDDeserializer`.</p>
   *
   * @param p      the JSON parser
   * @param ctxt   the deserialization context
   * @return the deserialized `UUID` object
   * @throws IOException if an I/O error occurs during deserialization
   */
  @Override
  public UUID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException{
    if (p instanceof OsonParser
            && p.currentToken().equals(JsonToken.VALUE_EMBEDDED_OBJECT)) {
      byte[] bytes = p.getBinaryValue();
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      long mostSignificantBits = byteBuffer.getLong();
      long leastSignificantBits = byteBuffer.getLong();
      return new UUID(mostSignificantBits, leastSignificantBits);
    } else {
      return UUID_DESERIALIZER.deserialize(p,ctxt);
    }
  }
}
