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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.persistence.AttributeConverter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * A custom serializer for converting Java objects using an `AttributeConverter` during serialization.
 * This serializer leverages JPA-style `AttributeConverter` to convert an object into its
 * database-compatible representation before writing it to JSON.
 */
public class OsonConverterSerializer extends JsonSerializer<Object> {

  /**
   * The attribute converter used to transform objects during serialization.
   */
  private AttributeConverter<Object, Object> attributeConverter;

  /**
   * Constructs an instance of `OsonConverterSerializer` with the provided `AttributeConverter` class.
   *
   * @param converter the class of the `AttributeConverter` to use
   * @throws RuntimeException if the converter cannot be instantiated
   */
  public OsonConverterSerializer(Class<? extends AttributeConverter> converter) {
    try {
      this.attributeConverter = converter.getConstructor().newInstance();
    } catch (InstantiationException
         | IllegalAccessException
         | InvocationTargetException
         | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Serializes a given value by converting it with the `AttributeConverter` and writing the result
   * to the JSON generator.
   *
   * @param value       the object to serialize
   * @param gen         the JSON generator used to write the JSON output
   * @param serializers the serializer provider
   * @throws IOException if an I/O error occurs during serialization
   */
  @Override
  public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    if (value == null) {
      gen.writeNull();
      return;
    }

    Object convertedValue = attributeConverter.convertToDatabaseColumn(value);
    gen.writeObject(convertedValue);
  }
}
