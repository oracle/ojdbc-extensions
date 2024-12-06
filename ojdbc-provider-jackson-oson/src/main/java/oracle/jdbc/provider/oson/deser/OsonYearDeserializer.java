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
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.YearDeserializer;
import oracle.jdbc.provider.oson.OsonParser;

import java.io.IOException;
import java.time.Year;
import java.time.format.DateTimeFormatter;

/**
 * Deserializer class for handling {@link Year} objects using {@link OsonParser}.
 * This class extends {@link StdScalarDeserializer} to enable the deserialization of {@link Year} values from JSON data.
 * <p>
 * It overrides the {@link #deserialize(JsonParser, DeserializationContext)} method to provide
 * a custom implementation that uses the Oson library's {@link OsonParser#getIntValue()} method
 * to extract the year as an integer and convert it to a {@link Year} object.
 * </p>
 *
 * @see StdScalarDeserializer
 * @see OsonParser
 * @see Year
 */
public class OsonYearDeserializer extends YearDeserializer {

  /**
   * A singleton instance of the deserializer.
   */
  public static final OsonYearDeserializer INSTANCE = new OsonYearDeserializer();

  /**
   * Default constructor that initializes the deserializer for the {@link Year} class.
   */
  protected OsonYearDeserializer() {
    super();
  }

  /**
   *  Constructor that initializes the deserializer for the {@link Year} class
   *  with {@link DateTimeFormatter}.
   */
  public OsonYearDeserializer(DateTimeFormatter formatter) {
    super(formatter);
  }

  /**
   *  Constructor that initializes the deserializer for the {@link Year} class
   *  with leniency.
   */
  protected OsonYearDeserializer(OsonYearDeserializer base, Boolean leniency) {
    super(base, leniency);
  }

  /**
   *  Initializes the deserializer for the {@link Year} class
   *  with {@link DateTimeFormatter}.
   */
  @Override
  protected OsonYearDeserializer withDateFormat(DateTimeFormatter dtf) {
    return new OsonYearDeserializer(dtf);
  }

  /**
   *  Initializes the deserializer for the {@link Year} class
   *  with leniency.
   */
  @Override
  protected OsonYearDeserializer withLeniency(Boolean leniency) {
    return new OsonYearDeserializer(this, leniency);
  }

  /**
   *  Initializes the deserializer for the {@link Year} class
   *  with {@link JsonFormat.Shape}.
   */
  @Override
  protected OsonYearDeserializer withShape(JsonFormat.Shape shape) { return this; }



  /**
   * Deserializes a {@link Year} object from the JSON input using the {@link OsonParser}.
   * The year is extracted as an integer and converted into a {@link Year} object.
   *
   * @param p the {@link JsonParser} for reading the JSON content
   * @param ctxt the deserialization context
   * @return the deserialized {@link Year} object
   * @throws IOException if there is a problem with reading the input
   */
  @Override
  public Year deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

    if(p instanceof OsonParser) {
      final OsonParser _parser = (OsonParser)p;

      return Year.of(_parser.getIntValue());
    } else {
      return super.deserialize(p, ctxt);
    }


  }
}