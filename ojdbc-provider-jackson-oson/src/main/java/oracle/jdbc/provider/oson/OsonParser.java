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

public class OsonParser extends ParserBase {
	
	private static final boolean DEBUG = false;
	private Logger logger = Logger.getLogger("OsonLogger");

	private OracleJsonParser parser = null;

	boolean closed;
	private String fieldName;

	private OracleJsonParser.Event currentEvent;
	private OracleJsonParser.Event lastClearedEvent;
	
	protected ObjectCodec _codec;

	private static final Map<OracleJsonParser.Event, JsonToken> OSON_EVENT_TO_JSON_TOKEN = new HashMap<>();

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
	
	protected OsonParser(IOContext ctxt, int features, OracleJsonParser parser) {
		super(ctxt, features);
		this.parser = parser;
	}

	@Override
	protected void _closeInput() throws IOException {
		parser.close();
	}

	
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

	@Override
	public ObjectCodec getCodec() {
		return _codec;
	}

	@Override
	public void setCodec(ObjectCodec oc) {
		_codec = oc;
	}

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

	public boolean isExpectedStartObjectToken() {
		return currentEvent == OracleJsonParser.Event.START_OBJECT;
	}

	public boolean isExpectedStartArrayToken() {
		return currentEvent == OracleJsonParser.Event.START_ARRAY;
	}


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

	@Override
	public boolean hasCurrentToken() {
		if(DEBUG) logger.log(Level.FINEST, "hasCurrentToken");
		return currentEvent != null;
	}

	@Override
	public boolean hasTokenId(final int id) {
		if(DEBUG) logger.log(Level.FINEST, "hasTokenId( " + id + " )");
		if (id == JsonTokenId.ID_FIELD_NAME) {
			return currentEvent == OracleJsonParser.Event.KEY_NAME;
		}

		return false;
	}

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

	@Override
	public void clearCurrentToken() {
		lastClearedEvent = currentEvent;
		currentEvent = null;
	}

	@Override
	public JsonToken getLastClearedToken() {
		if(DEBUG) logger.log(Level.FINEST, "getLastClearedToken");
		if (lastClearedEvent == null) {
			return null;
		}

		return OSON_EVENT_TO_JSON_TOKEN.get(lastClearedEvent);
	}

	@Override
	public void overrideCurrentName(String s) {
		this.fieldName = s;
	}

	@Override
	public String getCurrentName() throws IOException {
		return fieldName;
	}

	@Override
	public String getText() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "getText");
		return parser.getString();
	}

	//discuss
	@Override
	public char[] getTextCharacters() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "getTextCharacters");
		return parser.getString().toCharArray();
	}

	//discuss
	@Override
	public int getTextLength() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "getTextLength");
		return parser.getString().length();
	}

	// discuss
	@Override
	public int getTextOffset() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "getTextOffset");
		return 0;
	}

	@Override
	public Number getNumberValue() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "getNumberValue");
		return parser.getBigDecimal();
	}

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

	@Override
	public int getIntValue() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "getIntValue");
		return parser.getInt();
	}

	@Override
	public long getLongValue() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "getLongValue");
		return parser.getLong();
	}

	@Override
	public BigInteger getBigIntegerValue() throws IOException {
		return parser.getBigInteger();
	}

	@Override
	public float getFloatValue() throws IOException {
		return parser.getFloat();
	}

	@Override
	public double getDoubleValue() throws IOException {
		return parser.getDouble();
	}

	@Override
	public BigDecimal getDecimalValue() throws IOException {
		return parser.getBigDecimal();
	}

	//discuss
	@Override
	public byte[] getBinaryValue(Base64Variant base64Variant) throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "getBinaryValue");
		return parser.getBytes();
	}

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

	public LocalDateTime readLocalDateTime() {
		if(DEBUG) logger.log(Level.FINEST, "readLocalDateTime " + currentEvent);
		if(currentEvent == OracleJsonParser.Event.VALUE_STRING) {
			return LocalDateTime.parse( parser.getString() );
		} else {
			return parser.getLocalDateTime();
		}
	}

	public OffsetDateTime readOffsetDateTime() {
		if(DEBUG) logger.log(Level.FINEST, "readOffsetDateTime " + currentEvent);
		if(currentEvent == OracleJsonParser.Event.VALUE_STRING) {
			return OffsetDateTime.parse( parser.getString() );
		} else {
			return parser.getOffsetDateTime();
		}
	}
	
	public Duration readDuration() {
		if(DEBUG) logger.log(Level.FINEST, "readDuration " + currentEvent);
		if(currentEvent == OracleJsonParser.Event.VALUE_STRING) {
			return Duration.parse( parser.getString() );
		} else {
			return parser.getDuration();
		}
	}

	public Period readPeriod() {
		if(DEBUG) logger.log(Level.FINEST,"readPeriod " + currentEvent);
		if(currentEvent == OracleJsonParser.Event.VALUE_STRING) {
			return Period.parse( parser.getString() );
		} else {
			return parser.getPeriod();
		}
	}
}
