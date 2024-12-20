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
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * A custom serializer for `LocalDate` types, extending `LocalDateSerializer` to add custom serialization behavior.
 * This serializer is designed to handle `LocalDate` objects either using a provided `DateTimeFormatter` or a custom method
 * to write `LocalDate` values in a specific format.
 */
public class OsonLocalDateSerializer extends LocalDateSerializer {

  /**
   * A singleton instance of the `OsonLocalDateSerializer` for global use.
   */
  public static final OsonLocalDateSerializer INSTANCE = new OsonLocalDateSerializer();

  /**
   * Default constructor that initializes the serializer with default settings.
   */
  public OsonLocalDateSerializer() {
    super();
  }

  /**
   * Constructor that allows customizing the serializer with specific settings such as timestamp usage,
   * date format, and the desired shape of the output (e.g., as a string or as a numeric timestamp).
   *
   * @param base the base serializer to copy settings from
   * @param useTimestamp whether to serialize as a timestamp
   * @param dtf the custom date-time formatter to use for serialization
   * @param shape the desired output shape (e.g., string or numeric)
   */
  public OsonLocalDateSerializer(OsonLocalDateSerializer base, Boolean useTimestamp,
                                 DateTimeFormatter dtf, JsonFormat.Shape shape) {
    super(base, useTimestamp, dtf, shape);
  }

  /**
   * Constructor that uses a custom date-time formatter for serialization.
   *
   * @param formatter the custom date-time formatter to use
   */
  public OsonLocalDateSerializer(DateTimeFormatter formatter) {
    super(formatter);
  }

  /**
   * Creates a new instance of the `OsonLocalDateSerializer` with the specified settings.
   * This method is typically used when the format or shape of the output needs to be adjusted.
   *
   * @param useTimestamp whether to serialize as a timestamp
   * @param dtf the custom date-time formatter
   * @param shape the desired output shape (e.g., string or numeric)
   * @return a new instance of `OsonLocalDateSerializer`
   */
  @Override
  protected OsonLocalDateSerializer withFormat(Boolean useTimestamp, DateTimeFormatter dtf, JsonFormat.Shape shape) {
    return new OsonLocalDateSerializer(this, useTimestamp, dtf, shape);
  }

  /**
   * Serializes a `LocalDate` object to JSON.
   * If custom formatting or shape is specified, it uses the parent class (`LocalDateSerializer`) to perform the serialization.
   * If no custom format is provided, it writes the `LocalDate` using the `OsonGenerator`'s `writeLocalDate` method.
   *
   * @param date the `LocalDate` object to serialize
   * @param g the JSON generator used for serialization
   * @param provider the serializer provider
   * @throws IOException if an I/O error occurs during serialization
   */
  @Override
  public void serialize(LocalDate date, JsonGenerator g, SerializerProvider provider) throws IOException {
    if(_formatter != null || _shape != null) {
      super.serialize(date, g, provider);
      return;
    }
    if (g instanceof OsonGenerator) {
      OsonGenerator generator = (OsonGenerator) g;
      generator.writeLocalDate(date);
    }else {
      super.serialize(date, g, provider);
    }
  }
}
