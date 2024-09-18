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

package oracle.jdbc.provider.oson;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import oracle.sql.json.OracleJsonParser;
import oracle.sql.json.OracleJsonParser.Event;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OsonParser is a custom implementation of Jackson's ParserBase class that
 * translates events from Oracle's OracleJsonParser into JsonToken objects
 * compatible with the Jackson library. It used for integration
 * of Oracle's JSON parsing with Jackson's object mapping system.
 */
public class OsonParser extends ParserBase {

  /** Logger for debugging purposes */
  private static final boolean DEBUG = false;

  /** Logger for debugging purposes */
  private Logger logger = Logger.getLogger("OsonLogger");

  /** The OracleJsonParser instance to parse Oracle JSON data */
  private OracleJsonParser parser = null;

  /** Flag to check if the parser has been closed */
  boolean closed;

  private String fieldName;

  private OracleJsonParser.Event currentEvent;
  private OracleJsonParser.Event lastClearedEvent;
  
  protected ObjectCodec _codec;

  private static final Map<OracleJsonParser.Event, JsonToken> OSON_EVENT_TO_JSON_TOKEN = new HashMap<>();

  /**
   * A static map to map OracleJsonParser events to Jackson's JsonToken values.
   */
  static {
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.START_ARRAY, JsonToken.START_ARRAY);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.END_ARRAY, JsonToken.END_ARRAY);

    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.START_OBJECT, JsonToken.START_OBJECT);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.END_OBJECT, JsonToken.END_OBJECT);

    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.KEY_NAME, JsonToken.FIELD_NAME);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_STRING, JsonToken.VALUE_STRING);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_NULL, JsonToken.VALUE_NULL);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_TRUE, JsonToken.VALUE_TRUE);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_FALSE, JsonToken.VALUE_FALSE);

    // Different
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_DATE, JsonToken.VALUE_STRING);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_DOUBLE, JsonToken.VALUE_NUMBER_FLOAT);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_FLOAT, JsonToken.VALUE_NUMBER_FLOAT);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_TIMESTAMP, JsonToken.VALUE_STRING);
    OSON_EVENT_TO_JSON_TOKEN.put(OracleJsonParser.Event.VALUE_TIMESTAMPTZ, JsonToken.VALUE_STRING);
  }

  /**
   * Constructor for OsonParser.
   *
   * @param ctxt The IOContext object.
   * @param features The parser features.
   * @param parser The OracleJsonParser instance.
   */
  protected OsonParser(IOContext ctxt, int features, OracleJsonParser parser) {
    super(ctxt, features);
    this.parser = parser;
  }

  /**
   * Closes the underlying OracleJsonParser.
   *
   * @throws IOException if an I/O error occurs.
   */
  @Override
  protected void _closeInput() throws IOException {
    parser.close();
  }

  /**
   * Returns the next token from the OracleJsonParser.
   *
   * @return The next JsonToken.
   * @throws IOException if an I/O error occurs.
   */
  @Override
  public JsonToken nextToken() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "nextToken");
    if (parser.hasNext()) {
      currentEvent = parser.next();

      switch (currentEvent) {
        case START_OBJECT:
          return JsonToken.START_OBJECT;
          
        case END_OBJECT:
          return JsonToken.END_OBJECT;

        case START_ARRAY:
          return JsonToken.START_ARRAY;
          
        case END_ARRAY:
          return JsonToken.END_ARRAY;

        case KEY_NAME:
          if(DEBUG) logger.log(Level.FINEST, "KEY_NAME> " + parser.getString());
          this.fieldName = parser.getString();
          return JsonToken.FIELD_NAME;

        case VALUE_STRING:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_STRING> " + parser.getString());
          return JsonToken.VALUE_STRING;

        case VALUE_DECIMAL:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_DECIMAL> " + parser.getBigDecimal()+" / "+parser.isIntegralNumber());
          return parser.isIntegralNumber() ? JsonToken.VALUE_NUMBER_INT : JsonToken.VALUE_NUMBER_FLOAT;

        case VALUE_FLOAT:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_FLOAT> " + parser.getFloat());
          return JsonToken.VALUE_NUMBER_FLOAT;

        case VALUE_DOUBLE:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_DOUBLE> " + parser.getDouble());
          return JsonToken.VALUE_NUMBER_FLOAT;

        case VALUE_NULL:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_NULL");
          return JsonToken.VALUE_NULL;

        case VALUE_TRUE:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_TRUE");
          return JsonToken.VALUE_TRUE;

        case VALUE_FALSE:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_FALSE");
          return JsonToken.VALUE_FALSE;

        case VALUE_TIMESTAMP:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_TIMESTAMP");
          return JsonToken.VALUE_STRING;

        case VALUE_TIMESTAMPTZ:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_TIMESTAMPTZ");
          return JsonToken.VALUE_STRING;

        case VALUE_BINARY:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_BINARY");
          return JsonToken.VALUE_STRING;
          
        case VALUE_INTERVALDS:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_INTERVALDS");
          return JsonToken.VALUE_STRING;
          
        case VALUE_INTERVALYM:
          if(DEBUG) logger.log(Level.FINEST, "VALUE_INTERVALYM");
          return JsonToken.VALUE_STRING;
          
        default:
          throw new IllegalStateException("event: " + currentEvent);
      }
    }

    return null;
  }

  /**
   * Returns the current object codec.
   *
   * @return The ObjectCodec instance.
   */
  @Override
  public ObjectCodec getCodec() {
    return _codec;
  }

  /**
   * Sets the object codec.
   *
   * @param oc The ObjectCodec instance to be set.
   */
  @Override
  public void setCodec(ObjectCodec oc) {
    _codec = oc;
  }

  /**
   * Closes the parser.
   *
   * @throws IOException if an I/O error occurs.
   */
  @Override
  public void close() throws IOException {
    parser.close();
    closed = true;
  }

  @Override
  public JsonToken nextValue() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "nextValue");
    return null;
  }

  /**
   * Skips the children nodes in the current JSON structure.
   *
   * @return The current JsonParser instance.
   * @throws IOException if an I/O error occurs.
   */
  @Override
  public JsonParser skipChildren() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "skipChildren");
    if (currentEvent == null && parser.hasNext()) {
      currentEvent = parser.next();
    }
    if(currentEvent == Event.START_ARRAY) {
      parser.skipArray();
    }
    if(currentEvent == Event.START_OBJECT) {
      parser.skipObject();
    }
    return this;
  }

  /**
   * Checks if the current token is the expected start of an object.
   *
   * @return true if the current token is a start object token, false otherwise.
   */
  public boolean isExpectedStartObjectToken() {
    return currentEvent == OracleJsonParser.Event.START_OBJECT;
  }

  /**
   * Checks if the current token is the expected start of an array.
   *
   * @return true if the current token is a start array token, false otherwise.
   */
  public boolean isExpectedStartArrayToken() {
    return currentEvent == OracleJsonParser.Event.START_ARRAY;
  }

  /**
   * Returns the current JsonToken.
   *
   * @return The current JsonToken.
   */
  @Override
  public JsonToken getCurrentToken() {
    if(DEBUG) logger.log(Level.FINEST, "getCurrentToken");
    if (currentEvent == null && parser.hasNext()) {
      currentEvent = parser.next();
    }
    switch (currentEvent) {
      case START_OBJECT:
        return JsonToken.START_OBJECT;
      case END_OBJECT:
        return JsonToken.END_OBJECT;

      case START_ARRAY:
        return JsonToken.START_ARRAY;
      case END_ARRAY:
        return JsonToken.END_ARRAY;

      case KEY_NAME:
        if(DEBUG) logger.log(Level.FINEST, "KEY_NAME> " + parser.getString());
        this.fieldName = parser.getString();
        return JsonToken.FIELD_NAME;

      case VALUE_STRING:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_STRING> " + parser.getString());
        return JsonToken.VALUE_STRING;

      case VALUE_DECIMAL:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_DECIMAL> " + parser.getBigDecimal()+" / "+parser.isIntegralNumber());
        return parser.isIntegralNumber() ? JsonToken.VALUE_NUMBER_INT : JsonToken.VALUE_NUMBER_FLOAT;

      case VALUE_FLOAT:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_FLOAT> " + parser.getFloat());
        return JsonToken.VALUE_NUMBER_FLOAT;

      case VALUE_DOUBLE:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_DOUBLE> " + parser.getDouble());
        return JsonToken.VALUE_NUMBER_FLOAT;

      case VALUE_NULL:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_NULL");
        return JsonToken.VALUE_NULL;

      case VALUE_TRUE:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_TRUE");
        return JsonToken.VALUE_TRUE;

      case VALUE_FALSE:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_FALSE");
        return JsonToken.VALUE_FALSE;

      case VALUE_TIMESTAMP:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_TIMESTAMP");
        return JsonToken.VALUE_STRING;

      case VALUE_TIMESTAMPTZ:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_TIMESTAMPTZ");
        return JsonToken.VALUE_STRING;
        
      case VALUE_BINARY:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_BINARY");
        return JsonToken.VALUE_STRING;
        
      case VALUE_INTERVALDS:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_INTERVALDS");
        return JsonToken.VALUE_STRING;
        
      case VALUE_INTERVALYM:
        if(DEBUG) logger.log(Level.FINEST, "VALUE_INTERVALYM");
        return JsonToken.VALUE_STRING;

      default:
        throw new IllegalStateException("event: " + currentEvent);
    }

  }

  /**
   * Get the current Token ID.
   */
  @Override
  public int getCurrentTokenId() {
    if(DEBUG) logger.log(Level.FINEST, "getCurrentTokenId");
    JsonToken jt;
    if ((jt = OSON_EVENT_TO_JSON_TOKEN.get(currentEvent)) != null) {
      return jt.id();
    } else if( currentEvent == OracleJsonParser.Event.VALUE_DECIMAL ) {
      return parser.isIntegralNumber() ? JsonToken.VALUE_NUMBER_INT.id() : JsonToken.VALUE_NUMBER_FLOAT.id();
    }

    return -1;
  }

  /**
   * Get the current Token ID.
   */
  @Override
  public int currentTokenId() {
    if(DEBUG) logger.log(Level.FINEST, "getCurrentTokenId");
    JsonToken jt;
    if ((jt = OSON_EVENT_TO_JSON_TOKEN.get(currentEvent)) != null) {
      return jt.id();
    } else if( currentEvent == OracleJsonParser.Event.VALUE_DECIMAL ) {
      return parser.isIntegralNumber() ? JsonToken.VALUE_NUMBER_INT.id() : JsonToken.VALUE_NUMBER_FLOAT.id();
    }

    return JsonTokenId.ID_NO_TOKEN;
  }

  /**
   * Get the current Token ID.
   */
  @Override
  public boolean hasCurrentToken() {
    if(DEBUG) logger.log(Level.FINEST, "hasCurrentToken");
    return currentEvent != null;
  }

  /**
   * Checks if the provided id matches the ID FIELD.
   */
  @Override
  public boolean hasTokenId(final int id) {
    if(DEBUG) logger.log(Level.FINEST, "hasTokenId( " + id + " )");
    if (id == JsonTokenId.ID_FIELD_NAME) {
      return currentEvent == OracleJsonParser.Event.KEY_NAME;
    }

    return false;
  }

  /**
   * Checks if the current token matches the provided JsonToken.
   *
   * @param jsonToken The JsonToken to check against.
   * @return true if the current token matches the provided JsonToken, false otherwise.
   */
  @Override
  public boolean hasToken(JsonToken jsonToken) {
    if(DEBUG) logger.log(Level.FINEST, "hasToken( " + jsonToken + " )");
    JsonToken jt = OSON_EVENT_TO_JSON_TOKEN.get(currentEvent);

    if (jt != null) {
      return jt.id() == jsonToken.id();
    } else if( currentEvent == OracleJsonParser.Event.VALUE_DECIMAL ) {
      return parser.isIntegralNumber() ? JsonToken.VALUE_NUMBER_INT.id() == jsonToken.id() : JsonToken.VALUE_NUMBER_FLOAT.id() == jsonToken.id();
    }

    return false;
  }

  /**
   * Clears the current token.
   */
  @Override
  public void clearCurrentToken() {
    lastClearedEvent = currentEvent;
    currentEvent = null;
  }

  /**
   * Get the last cleared token.
   */
  @Override
  public JsonToken getLastClearedToken() {
    if(DEBUG) logger.log(Level.FINEST, "getLastClearedToken");
    if (lastClearedEvent == null) {
      return null;
    }

    return OSON_EVENT_TO_JSON_TOKEN.get(lastClearedEvent);
  }


  /**
   * Overrides the current field name with the provided string.
   *
   * @param s The new field name to override the current one.
   */
  @Override
  public void overrideCurrentName(String s) {
    this.fieldName = s;
  }

  /**
   * Get the current filed name.
   */
  @Override
  public String getCurrentName() throws IOException {
    return fieldName;
  }

  /**
   * Get the text from the parser.
   */
  @Override
  public String getText() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getText");
    return parser.getString();
  }

  /**
   * Get the text as character array.
   */
  //discuss
  @Override
  public char[] getTextCharacters() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getTextCharacters");
    return parser.getString().toCharArray();
  }

  /**
   * Get the text length.
   */
  //discuss
  @Override
  public int getTextLength() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getTextLength");
    return parser.getString().length();
  }

  /**
   * Get the text offset.
   */
  // discuss
  @Override
  public int getTextOffset() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getTextOffset");
    return 0;
  }

  /**
   * Get the Bigdecimal  value.
   */
  @Override
  public Number getNumberValue() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getNumberValue");
    return parser.getBigDecimal();
  }

  /**
   * Get the type of Number.
   */
  @Override
  public NumberType getNumberType() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getNumberType "+parser.isIntegralNumber());
    switch(currentEvent) {
      case VALUE_FLOAT:
        return NumberType.FLOAT;
      case VALUE_DECIMAL:
        return NumberType.BIG_DECIMAL;
    }

    return null;
  }

  /**
   * Get the Int Value.
   */
  @Override
  public int getIntValue() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getIntValue");
    return parser.getInt();
  }

  /**
   * Get the Long Value.
   */
  @Override
  public long getLongValue() throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getLongValue");
    return parser.getLong();
  }

  /**
   * Get the BigInteger Value.
   */
  @Override
  public BigInteger getBigIntegerValue() throws IOException {
    return parser.getBigInteger();
  }

  /**
   * Get the Float Value.
   */
  @Override
  public float getFloatValue() throws IOException {
    return parser.getFloat();
  }

  /**
   * Get the Double Value.
   */
  @Override
  public double getDoubleValue() throws IOException {
    return parser.getDouble();
  }

  /**
   * Get the Decimal Value.
   */
  @Override
  public BigDecimal getDecimalValue() throws IOException {
    return parser.getBigDecimal();
  }

  /**
   * Get the Binary bytes.
   */
  //discuss
  @Override
  public byte[] getBinaryValue(Base64Variant base64Variant) throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getBinaryValue");
    return parser.getBytes();
  }

  /**
   * Get the String Value.
   */
  @Override
  public String getValueAsString(String s) throws IOException {
    if(DEBUG) logger.log(Level.FINEST, "getValueAsString");
    if (currentToken() == JsonToken.FIELD_NAME) {
      return fieldName;
    }
    else {
      return parser.getString();
    }
  }

  /**
   * Reads a LocalDateTime value from the OracleJsonParser.
   *
   * @return The LocalDateTime value parsed.
   */
  public LocalDateTime readLocalDateTime() {
    if(DEBUG) logger.log(Level.FINEST, "readLocalDateTime " + currentEvent);
    if(currentEvent == OracleJsonParser.Event.VALUE_STRING) {
      return LocalDateTime.parse( parser.getString() );
    } else {
      return parser.getLocalDateTime();
    }
  }
  /**
   * Reads an OffsetDateTime value from the OracleJsonParser.
   *
   * @return The OffsetDateTime value parsed.
   */

  public OffsetDateTime readOffsetDateTime() {
    if(DEBUG) logger.log(Level.FINEST, "readOffsetDateTime " + currentEvent);
    if(currentEvent == OracleJsonParser.Event.VALUE_STRING) {
      return OffsetDateTime.parse( parser.getString() );
    } else {
      return parser.getOffsetDateTime();
    }
  }

  /**
   * Reads a Duration value from the OracleJsonParser.
   *
   * @return The Duration value parsed.
   */
  public Duration readDuration() {
    if(DEBUG) logger.log(Level.FINEST, "readDuration " + currentEvent);
    if(currentEvent == OracleJsonParser.Event.VALUE_STRING) {
      return Duration.parse( parser.getString() );
    } else {
      return parser.getDuration();
    }
  }

  /**
   * Reads a Period value from the OracleJsonParser.
   *
   * @return The Period value parsed.
   */
  public Period readPeriod() {
    if(DEBUG) logger.log(Level.FINEST,"readPeriod " + currentEvent);
    if(currentEvent == OracleJsonParser.Event.VALUE_STRING) {
      return Period.parse( parser.getString() );
    } else {
      return parser.getPeriod();
    }
  }
}
