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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import oracle.jdbc.provider.oson.OsonParser;
import oracle.sql.json.OracleJsonParser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom deserializer for `LocalDate` that extends Jackson's `LocalDateDeserializer`.
 * This implementation is specifically designed to handle JSON input in the context of
 * `OsonParser` and supports Oracle JSON-specific events like `VALUE_DATE`.
 *
 * <p>It provides additional flexibility by allowing the use of custom `DateTimeFormatter`
 * and supports configurations for leniency and shape.</p>
 */
public class OsonLocalDateDeserializer extends LocalDateDeserializer {
  /**
   * Singleton instance for default usage.
   */
  public static final OsonLocalDateDeserializer INSTANCE = new OsonLocalDateDeserializer();
  /**
   * Default constructor using ISO_LOCAL_DATE as the default formatter.
   */
  public OsonLocalDateDeserializer() {
    super();
  }

  /**
   * Constructor to initialize with a custom `DateTimeFormatter`.
   *
   * @param dateFormat the custom `DateTimeFormatter` to use
   */
  public OsonLocalDateDeserializer(DateTimeFormatter dateFormat) {
    super(dateFormat);
  }

  /**
   * Copy constructor with a custom `DateTimeFormatter`.
   *
   * @param base the base deserializer
   * @param dtf  the custom `DateTimeFormatter`
   */
  public OsonLocalDateDeserializer(OsonLocalDateDeserializer base, DateTimeFormatter dtf) {
    super(base, dtf);
  }

  /**
   * Copy constructor with leniency configuration.
   *
   * @param base     the base deserializer
   * @param leniency the leniency setting
   */
  public OsonLocalDateDeserializer(OsonLocalDateDeserializer base, Boolean leniency) {
    super(base, leniency);
  }

  /**
   * Copy constructor with shape configuration.
   *
   * @param base  the base deserializer
   * @param shape the JSON format shape
   */
  public OsonLocalDateDeserializer(OsonLocalDateDeserializer base, JsonFormat.Shape shape) {
    super(base, shape);
  }

  /**
   * Creates a new deserializer with the specified `DateTimeFormatter`.
   *
   * @param dtf the custom `DateTimeFormatter`
   * @return a new `OsonLocalDateDeserializer` with the given formatter
   */
  @Override
  protected OsonLocalDateDeserializer withDateFormat(DateTimeFormatter dtf) {
    return new OsonLocalDateDeserializer(dtf);
  }

  /**
   * Creates a new deserializer with the specified JSON format shape.
   *
   * @param shape the JSON format shape
   * @return a new `OsonLocalDateDeserializer` with the given shape
   */
  @Override
  protected OsonLocalDateDeserializer withShape(JsonFormat.Shape shape) {
    return new OsonLocalDateDeserializer(this,shape);
  }

  /**
   * Deserializes a JSON input into a `LocalDate` instance.
   *
   * <p>If the input is parsed using `OsonParser` and contains an Oracle JSON-specific
   * `VALUE_DATE` event, it processes the input as a `LocalDate`. For all other cases,
   * it delegates to the base `LocalDateDeserializer`.</p>
   *
   * @param parser  the JSON parser
   * @param context the deserialization context
   * @return the deserialized `LocalDate`
   * @throws IOException if a low-level I/O problem occurs
   */
  @Override
  public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    if(_shape != null || _formatter!= DateTimeFormatter.ISO_LOCAL_DATE)
      return super.deserialize(parser, context);

    if (parser instanceof OsonParser) {
      OsonParser _parser = (OsonParser) parser;

      if(_parser.currentOsonEvent().equals(OracleJsonParser.Event.VALUE_DATE)) {
        LocalDateTime dateTime = _parser.readLocalDateTime();
        return dateTime.toLocalDate();
      }
    }
    else {
      return super.deserialize(parser, context);
    }
    return super.deserialize(parser, context);
  }
}
