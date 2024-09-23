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
import com.fasterxml.jackson.databind.util.TokenBuffer;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.IOException;
import java.time.Year;

/**
 * Serializer class for handling {@link Year} objects using {@link OsonGenerator}.
 * This class extends {@link StdSerializer} to enable the serialization of {@link Year} values into JSON format.
 * <p>
 * It overrides the {@link #serialize(Year, JsonGenerator, SerializerProvider)} method to provide
 * a custom implementation that uses the Oson library's {@link OsonGenerator#writeNumber(int)} method
 * for writing the year value as an integer in the JSON output.
 * </p>
 *
 * @see StdSerializer
 * @see OsonGenerator
 * @see Year
 */
public class OsonYearSerializer extends StdSerializer<Year> {

  /**
   * A singleton instance of the serializer.
   */
  public static final OsonYearSerializer INSTANCE = new OsonYearSerializer();

  /**
   * Default constructor that initializes the serializer for the {@link Year} class.
   */
  public OsonYearSerializer() {
    super(Year.class);
  }

  /**
   * Serializes a {@link Year} object into JSON format using the {@link OsonGenerator}.
   * The year is written as an integer value.
   *
   * @param value the {@link Year} value to serialize
   * @param gen the {@link JsonGenerator} for writing JSON output
   * @param provider the serializer provider
   * @throws IOException if there is a problem with writing the output
   */
  @Override
  public void serialize(Year value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    if (gen instanceof TokenBuffer) {
      gen.writeNumber(value.getValue());
    } else {
      final OsonGenerator _gen = (OsonGenerator)gen;

      _gen.writeNumber(value.getValue());
    }
  }
}
