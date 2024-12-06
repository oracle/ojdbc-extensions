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
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Pattern;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import oracle.jdbc.provider.oson.OsonParser;
import oracle.sql.json.OracleJsonParser;

/**
 * Custom deserializer for handling date and time deserialization in JSON.
 * Extends the Jackson `DateDeserializers.DateDeserializer` to provide additional handling for
 * temporal timestamp and time values based on specific annotations.
 *
 * This class supports:
 * - Deserialization of temporal timestamps with fractional seconds.
 * - Deserialization of temporal time strings in HH:mm:ss format.
 * - Contextual deserialization based on the `@Temporal` annotation.
 */
public class OsonDateDeserializer extends DateDeserializers.DateDeserializer {

    /**
     * Singleton instance of the OsonDateDeserializer with default configuration.
     */
    public static final OsonDateDeserializer INSTANCE = new OsonDateDeserializer(false,false);

    /**
     * Indicates whether the deserializer should handle temporal timestamps.
     */
    private boolean isTemporalTimeStamp = false;

    /**
     * Indicates whether the deserializer should handle temporal time strings.
     */
    private boolean isTemporalTime = false;

    /**
     * Default constructor. Initializes a standard OsonDateDeserializer.
     */
    public OsonDateDeserializer() {
        super();
    }

    /**
     * Constructs a deserializer with specific handling for temporal timestamps or time strings.
     *
     * @param isTemporalTimeStamp whether to handle temporal timestamps.
     * @param isTemporalTime      whether to handle temporal time strings.
     */
    public OsonDateDeserializer(boolean isTemporalTimeStamp, boolean isTemporalTime) {
        this();
        this.isTemporalTimeStamp = isTemporalTimeStamp;
        this.isTemporalTime = isTemporalTime;
    }

    /**
     * Constructs a deserializer with a custom date format.
     *
     * @param base          the base deserializer.
     * @param df            the custom date format.
     * @param formatString  the format string used for deserialization.
     */
    public OsonDateDeserializer(DateDeserializers.DateDeserializer base, DateFormat df, String formatString) {
        super(base, df, formatString);
    }


    @Override
    protected OsonDateDeserializer withDateFormat(DateFormat df, String formatString) {
        return new OsonDateDeserializer(this, df, formatString);
    }

    /**
     * Overrides the default method to return an empty `Date` instance with epoch time.
     *
     * @param ctxt the deserialization context.
     * @return a `Date` object representing epoch time (1970-01-01T00:00:00Z).
     */
    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return new Date(0L);
    }

    /**
     * Deserializes a JSON token into a `Date`, `Time`, or `Timestamp` object.
     *
     * @param p     the JSON parser.
     * @param ctxt  the deserialization context.
     * @return the deserialized date object.
     * @throws IOException if an I/O error occurs during deserialization.
     */
    @Override
    public java.util.Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if(_customFormat != null) {
            return super.deserialize(p, ctxt);
        }
        if( p instanceof OsonParser) {
            OsonParser parser = (OsonParser) p;

            if(parser.currentOsonEvent().equals(OracleJsonParser.Event.VALUE_DATE)) {
                LocalDateTime dateTime = parser.getLocalDateTime();
                return Date.from(dateTime.atZone(ZoneId.of("UTC")).toInstant());
            }
            if(parser.currentOsonEvent().equals(OracleJsonParser.Event.VALUE_TIMESTAMP)) {
                LocalDateTime dateTime = parser.getLocalDateTime();
                return Timestamp.valueOf(dateTime);
            }
            switch (p.getCurrentTokenId()) {
                case JsonTokenId.ID_STRING: {
                    if (isTemporalTime) {
                        return Time.valueOf(p.getText().trim());
                    }
                    String dateTimeString = p.getText().trim();
                    LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return Date.from(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
                }
                default: return super.deserialize(p, ctxt);
            }
        }
        return super.deserialize(p, ctxt);

    }

    /**
     * Creates a contextual deserializer based on the `@Temporal` annotation.
     *
     * @param ctxt     the deserialization context.
     * @param property the bean property being deserialized.
     * @return a contextual instance of `OsonDateDeserializer`.
     * @throws JsonMappingException if contextual deserialization fails.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            AnnotatedMember introspector = property.getMember();
            if (introspector != null) {
                Temporal temporal = introspector.getAnnotation(Temporal.class);
                if (temporal != null) {
                    if (temporal.value() == TemporalType.TIMESTAMP) {
                        return new OsonDateDeserializer(true,false);
                    }
                    if (temporal.value() == TemporalType.TIME) {
                        return new OsonDateDeserializer(false,true);
                    }
                }
            }
        }
        return super.createContextual(ctxt, property);
    }

}
