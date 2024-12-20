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
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

/**
 * Custom serializer for handling date and time serialization in JSON.
 * Extends the Jackson `DateSerializer` to provide additional handling for
 * temporal timestamps and time values based on specific annotations.
 *
 * This class supports:
 * - Serialization of temporal timestamps with fractional seconds.
 * - Serialization of temporal time objects in HH:mm:ss format.
 * - Contextual serialization based on the `@Temporal` annotation.
 */
public class OsonDateSerializer extends DateSerializer {

  /**
   * Singleton instance of the OsonDateSerializer with default configuration.
   */
  public static final OsonDateSerializer INSTANCE =
          new OsonDateSerializer(false,false);

  /**
   * Indicates whether the serializer should handle temporal timestamps.
   */
  private boolean isTemporalTimeStamp = false;

  /**
   * Indicates whether the serializer should handle temporal time objects.
   */

  private boolean isTemporalTime = false;

  @Override
  public OsonDateSerializer withFormat(Boolean timestamp, DateFormat customFormat) {
    return new OsonDateSerializer(timestamp,customFormat);
  }

  /**
   * Constructs a serializer with specific handling for temporal timestamps or time objects.
   *
   * @param isTemporalTimeStamp whether to handle temporal timestamps.
   * @param isTemporalTime    whether to handle temporal time objects.
   */
  public OsonDateSerializer(boolean isTemporalTimeStamp, boolean isTemporalTime) {
    this(null,null);
    this.isTemporalTimeStamp = isTemporalTimeStamp;
    this.isTemporalTime = isTemporalTime;
  }

  /**
   * Constructs a serializer with a custom timestamp usage and date format.
   *
   * @param useTimestamp  whether to use timestamps for serialization.
   * @param customFormat  the custom date format for serialization.
   */
  public OsonDateSerializer(Boolean useTimestamp, DateFormat customFormat) {
    super(useTimestamp, customFormat);
  }

  /**
   * Serializes a `Date` object into JSON, handling special cases for timestamps and time objects.
   *
   * @param value  the date value to serialize.
   * @param g    the JSON generator.
   * @param provider the serializer provider.
   * @throws IOException if an I/O error occurs during serialization.
   */
  @Override
  public void serialize(Date value, JsonGenerator g, SerializerProvider provider)
          throws IOException {
    if(_customFormat != null) {
      super.serialize(value, g, provider);
      return;
    }
    if( value instanceof java.sql.Time) {
      g.writeString(value.toString());
      return;
    }
    if (g instanceof OsonGenerator) {
      if(value instanceof Timestamp) {
        ((OsonGenerator)g).writeTimeStamp((Timestamp) value);
        return;
      }
      ((OsonGenerator)g).writeDate(value);
      return;
    }
    super.serialize(value, g, provider);
  }

  /**
   * Creates a contextual serializer based on the `@Temporal` annotation.
   *
   * @param serializers the serializer provider.
   * @param property  the bean property being serialized.
   * @return a contextual instance of `OsonDateSerializer`.
   * @throws JsonMappingException if contextual serialization fails.
   */
  @Override
  public JsonSerializer<?> createContextual(SerializerProvider serializers,
         BeanProperty property) throws JsonMappingException {
    if (property != null) {
      AnnotatedMember introspector = property.getMember();
      if (introspector != null) {
        Temporal temporal = introspector.getAnnotation(Temporal.class);
        if (temporal != null) {
          if (temporal.value() == TemporalType.TIMESTAMP) {
            return new OsonDateSerializer(true,false);
          }
          if (temporal.value() == TemporalType.TIME) {
            return new OsonDateSerializer(false,true);
          }
        }
      }
    }
    return super.createContextual(serializers, property);
  }
}
