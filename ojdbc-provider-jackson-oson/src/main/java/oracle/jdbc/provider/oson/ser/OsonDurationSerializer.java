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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import com.fasterxml.jackson.datatype.jsr310.util.DurationUnitConverter;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * Serializer class for handling {@link Duration} objects using Oson's generation system.
 * This class extends {@link StdSerializer} to enable the serialization of {@link Duration} values into JSON format.
 * <p>
 * It overrides the {@link #serialize(Duration, JsonGenerator, SerializerProvider)} method to provide
 * a custom implementation that uses the Oson library's {@link OsonGenerator#writeDuration(Duration)} method
 * for writing {@link Duration} values to the JSON output.
 * </p>
 *
 * @see StdSerializer
 * @see OsonGenerator
 * @see Duration
 */
public class OsonDurationSerializer extends DurationSerializer {

  /**
   * A singleton instance of the serializer.
   */
  public static final OsonDurationSerializer INSTANCE = new OsonDurationSerializer();

  /**
   * Default constructor that initializes the serializer for the {@link Duration} class.
   */
  public OsonDurationSerializer() {
    super();
  }

  protected OsonDurationSerializer(OsonDurationSerializer base,
                               Boolean useTimestamp, DateTimeFormatter dtf) {
    super(base, useTimestamp, dtf);
  }

  protected OsonDurationSerializer(OsonDurationSerializer base,
                               Boolean useTimestamp, Boolean useNanoseconds, DateTimeFormatter dtf) {
    super(base, useTimestamp, useNanoseconds, dtf);
  }

  protected OsonDurationSerializer(OsonDurationSerializer base, DurationUnitConverter converter) {
    super(base, converter);
  }

  @Override
  protected DurationSerializer withFormat(Boolean useTimestamp, DateTimeFormatter dtf, JsonFormat.Shape shape) {
    return new OsonDurationSerializer(this, useTimestamp, dtf);
  }

  protected DurationSerializer withConverter(DurationUnitConverter converter) {
    return new OsonDurationSerializer(this, converter);
  }



  /**
   * Serializes a {@link Duration} object into JSON format using the {@link OsonGenerator}.
   *
   * @param value the {@link Duration} value to serialize
   * @param gen the {@link JsonGenerator} for writing JSON output
   * @param provider the serializer provider
   * @throws IOException if there is a problem with writing the output
   */
  @Override
  public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    if (useTimestamp(provider) && gen instanceof OsonGenerator) {
      ((OsonGenerator) gen).writeDuration(value);
    }
    else {
      super.serialize(value, gen, provider);
    }

  }
}