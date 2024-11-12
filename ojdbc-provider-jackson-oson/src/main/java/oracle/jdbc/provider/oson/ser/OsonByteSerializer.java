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
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.IOException;

/**
 * Serializer class for handling byte array ({@code byte[]}) objects using {@link OsonGenerator}.
 * This class extends {@link StdSerializer} to enable the serialization of byte arrays into JSON format.
 * <p>
 * It overrides the {@link #serialize(byte[], JsonGenerator, SerializerProvider)} method to provide
 * a custom implementation that uses the Oson library's {@link OsonGenerator#writeBinary(byte[])} method
 * for writing byte arrays as binary data in the JSON output.
 * </p>
 *
 * @see StdSerializer
 * @see OsonGenerator
 * @see byte[]
 */
public class OsonByteSerializer extends StdSerializer<byte[]> {

  /**
   * A singleton instance of the serializer.
   */
  public static final OsonByteSerializer INSTANCE = new OsonByteSerializer();

  /**
   * Default constructor that initializes the serializer for the byte array ({@code byte[]}) class.
   */
  public OsonByteSerializer() {
    super(byte[].class);
  }

  /**
   * Serializes a byte array ({@code byte[]}) into JSON format using the {@link OsonGenerator}.
   *
   * @param value the byte array to serialize
   * @param gen the {@link JsonGenerator} for writing JSON output
   * @param provider the serializer provider
   * @throws IOException if there is a problem with writing the output
   */
  @Override
  public void serialize(byte[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeBinary(provider.getConfig().getBase64Variant(),
            value, 0, value.length);
    }
}
