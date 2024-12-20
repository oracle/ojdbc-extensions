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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import oracle.jdbc.provider.oson.OsonParser;
import oracle.sql.json.OracleJsonParser;

import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.time.LocalDateTime;

/**
 * A custom deserializer for SQL `Date` objects that extends Jackson's `SqlDateDeserializer`.
 * This implementation is designed to handle Oracle JSON-specific events when using `OsonParser`,
 * particularly the `VALUE_DATE` event.
 *
 * <p>If the parser is an instance of `OsonParser` and encounters a `VALUE_DATE` event, the
 * deserializer reads the date and converts it into an SQL `Date` object. For all other cases,
 * it delegates to the base `SqlDateDeserializer`.</p>
 */
public class OsonSqlDateDeserializer extends DateDeserializers.SqlDateDeserializer {
  /**
   * Singleton instance for reuse.
   */
  public static final OsonSqlDateDeserializer INSTANCE = new OsonSqlDateDeserializer();

  /**
   * Default constructor using the base deserializer's behavior.
   */
  public OsonSqlDateDeserializer() {
    super();
  }

  /**
   * Constructor with a custom date format.
   *
   * @param src          the base `SqlDateDeserializer` instance
   * @param df           the `DateFormat` to use for deserialization
   * @param formatString the format string for the date
   */
  public OsonSqlDateDeserializer(DateDeserializers.SqlDateDeserializer src, DateFormat df, String formatString) {
    super(src, df, formatString);
  }

  /**
   * Creates a new deserializer with the specified date format.
   *
   * @param df           the `DateFormat` to use
   * @param formatString the format string for the date
   * @return a new `SqlDateDeserializer` with the given format
   */
  @Override
  protected DateDeserializers.SqlDateDeserializer withDateFormat(DateFormat df, String formatString) {
    return super.withDateFormat(df, formatString);
  }

  /**
   * Deserializes a JSON input into an SQL `Date` object.
   *
   * <p>Handles `OsonParser`-specific `VALUE_DATE` events to directly parse the date into an
   * SQL `Date` object. For all other scenarios, the base deserialization behavior is used.</p>
   *
   * @param p      the JSON parser
   * @param ctxt   the deserialization context
   * @return the deserialized SQL `Date` object
   * @throws IOException if an I/O error occurs during deserialization
   */
  @Override
  public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if( p instanceof OsonParser) {
      OsonParser parser = (OsonParser) p;
      if(parser.currentOsonEvent().equals(OracleJsonParser.Event.VALUE_DATE)) {
        LocalDateTime dateTime = parser.readLocalDateTime();
        return Date.valueOf(dateTime.toLocalDate());
      }
    }
    return super.deserialize(p, ctxt);
  }
}
