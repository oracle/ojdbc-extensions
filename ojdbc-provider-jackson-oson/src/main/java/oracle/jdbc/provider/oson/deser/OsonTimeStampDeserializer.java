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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.LocalDateTime;

/**
 * A custom deserializer for `Timestamp` objects that extends Jackson's `TimestampDeserializer`.
 * This implementation is tailored to work with `OsonParser` and handle Oracle JSON-specific
 * events such as `VALUE_TIMESTAMP`.
 *
 * <p>For standard JSON input, it delegates to the base `TimestampDeserializer`. When using
 * `OsonParser` and encountering a `VALUE_TIMESTAMP` event, it converts the timestamp into a
 * `Timestamp` object directly.</p>
 */
public class OsonTimeStampDeserializer extends DateDeserializers.TimestampDeserializer {
  /**
   * Singleton instance for reuse.
   */
  public final static OsonTimeStampDeserializer INSTANCE = new OsonTimeStampDeserializer();

  /**
   * Default constructor using the base deserializer's behavior.
   */
  public OsonTimeStampDeserializer() {
    super();
  }

  /**
   * Constructor with a custom date format.
   *
   * @param src          the base `TimestampDeserializer` instance
   * @param df           the `DateFormat` to use for deserialization
   * @param formatString the format string for the timestamp
   */
  public OsonTimeStampDeserializer(DateDeserializers.TimestampDeserializer src,
                                   DateFormat df, String formatString) {
    super(src, df, formatString);
  }

  /**
   * Creates a new deserializer with the specified date format.
   *
   * @param df           the `DateFormat` to use
   * @param formatString the format string for the timestamp
   * @return a new `TimestampDeserializer` with the given format
   */
  @Override
  protected OsonTimeStampDeserializer withDateFormat(DateFormat df, String formatString) {
    return new OsonTimeStampDeserializer(this, df, formatString);
  }

  /**
   * Returns an empty value for the deserializer, delegating to the base implementation.
   *
   * @param ctxt the deserialization context
   * @return the empty value
   */
  @Override
  public Object getEmptyValue(DeserializationContext ctxt) {
    return super.getEmptyValue(ctxt);
  }

  /**
   * Deserializes a JSON input into a `Timestamp` object.
   *
   * <p>Handles `OsonParser`-specific `VALUE_TIMESTAMP` events to directly parse the timestamp
   * into a `Timestamp` object. For all other scenarios, it falls back to the base deserialization
   * behavior.</p>
   *
   * @param p      the JSON parser
   * @param ctxt   the deserialization context
   * @return the deserialized `Timestamp` object
   * @throws IOException if an I/O error occurs during deserialization
   */
  @Override
  public Timestamp deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if (_customFormat != null){
      return super.deserialize(p, ctxt);
    }
    if( p instanceof OsonParser) {
      OsonParser parser = (OsonParser) p;
      if(parser.currentOsonEvent().equals(OracleJsonParser.Event.VALUE_TIMESTAMP)) {
        LocalDateTime dateTime = parser.readLocalDateTime();
        return Timestamp.valueOf(dateTime);

      }
    }
    return super.deserialize(p, ctxt);
  }
}
