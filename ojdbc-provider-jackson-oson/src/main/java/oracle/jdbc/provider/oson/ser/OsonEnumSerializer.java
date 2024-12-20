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

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * A custom serializer for Java `Enum` types, allowing them to be serialized as either their
 * ordinal value or their name (as a string), based on annotations or provided flags.
 * This serializer supports both the default ordinal-based serialization and string-based serialization
 * for enumerated types.
 */
public class OsonEnumSerializer extends JsonSerializer<Enum<?>> implements ContextualSerializer {
  /**
   * Flag indicating if the enum should be serialized as its ordinal value or name.
   * When true, the enum is serialized as a number (ordinal).
   */
  private final boolean isEnumerated;

  /**
   * Flag indicating if the enum should be serialized as its name (string) instead of its ordinal.
   */
  private final boolean isEnumeratedString;

  /**
   * Constructs an `OsonEnumSerializer` with specific flags for enum serialization.
   *
   * @param isEnumerated whether the enum should be serialized as its ordinal (if true)
   * @param isEnumeratedString whether the enum should be serialized as its name (if true)
   */
  public OsonEnumSerializer(boolean isEnumerated, boolean isEnumeratedString) {
    this.isEnumerated = isEnumerated;
    this.isEnumeratedString = isEnumeratedString;
  }

  /**
   * Default constructor, which assumes the enum should not be serialized as either ordinal or string.
   */
  public OsonEnumSerializer() {
    this(false,false);
  }

  /**
   * Serializes an enum based on the configured flags:
   * - If both `isEnumerated` and `isEnumeratedString` are true, serialize the enum as a string (name).
   * - If only `isEnumerated` is true, serialize the enum as its ordinal (number).
   * - Otherwise, the enum is serialized by its ordinal value.
   *
   * @param value the enum value to serialize
   * @param gen the JSON generator
   * @param serializers the serializer provider
   * @throws IOException if an I/O error occurs during serialization
   */
  @Override
  public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    if (value == null) {
      gen.writeNull();
      return;
    }
    if (isEnumerated && isEnumeratedString) {
      gen.writeString(value.name());
      return;
    }
    gen.writeNumber(value.ordinal());
  }

  /**
   * Contextually configures the enum serializer based on annotations present in the property.
   * Specifically, it checks for the `@Enumerated` annotation to determine whether the enum should
   * be serialized as a string (name) or as its ordinal.
   *
   * @param prov the serializer provider
   * @param property the bean property being serialized
   * @return a contextualized enum serializer
   */
  @Override
  public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
    boolean isEnumeratedCT = false, isEnumeratedStringCT = false;
    if(property != null) {
      AnnotatedMember member = property.getMember();
      if (member != null){
        Enumerated enumerated = member.getAnnotation(Enumerated.class);
        if (enumerated != null){
          isEnumeratedCT = true;
          if (enumerated.value() == EnumType.STRING) {
            isEnumeratedStringCT = true;
          }
          return new OsonEnumSerializer(isEnumeratedCT, isEnumeratedStringCT);
        }
      }
    }
    return new OsonEnumSerializer(false,false);
  }
}
