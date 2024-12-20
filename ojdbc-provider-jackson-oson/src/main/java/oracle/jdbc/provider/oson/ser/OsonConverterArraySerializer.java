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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.std.ArraySerializerBase;
import jakarta.persistence.AttributeConverter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * A custom serializer for arrays of objects, using an `AttributeConverter` to convert each element
 * of the array into its database-compatible representation before serializing it to JSON.
 * This serializer is an extension of Jackson's `ArraySerializerBase`.
 */
public class OsonConverterArraySerializer extends ArraySerializerBase<Object[]> {

  /**
   * The attribute converter used to transform array elements during serialization.
   */
  private final AttributeConverter<Object, Object> attributeConverter;

  /**
   * Constructs an instance of `OsonConverterArraySerializer` with the provided `AttributeConverter` class.
   *
   * @param converter the class of the `AttributeConverter` to use
   * @throws RuntimeException if the converter cannot be instantiated
   */
  public OsonConverterArraySerializer(Class<? extends AttributeConverter> converter) {
    super(Object[].class);
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
   * Serializes the array of objects by converting each element using the `AttributeConverter`
   * and writing the resulting values as JSON array.
   *
   * @param value    the array of objects to serialize
   * @param gen      the JSON generator used to write the JSON output
   * @param provider the serializer provider
   * @throws IOException if an I/O error occurs during serialization
   */
  @Override
  public void serialize(Object[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    int len = value.length;
//    if (len == 1) {
//      Object convertedValue = attributeConverter.convertToDatabaseColumn(value[0]);
//      gen.writeObject(convertedValue);
//      return;
//    }
    gen.writeStartArray();

    for (int i = 0; i < len; i++) {
      if (value[i] == null) {
        gen.writeNull();
      }else {
        Object convertedValue = attributeConverter.convertToDatabaseColumn(value[i]);
        gen.writeObject(convertedValue);
      }
    }
    gen.writeEndArray();
  }

  // The following methods are inherited but are not used in this serializer.

  @Override
  public JsonSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
    return null;
  }

  @Override
  protected void serializeContents(Object[] value, JsonGenerator jgen, SerializerProvider provider){
    // no-op
  }

  @Override
  public JavaType getContentType() {
    return null;
  }

  @Override
  public JsonSerializer<?> getContentSerializer() {
    return null;
  }

  @Override
  public boolean hasSingleElement(Object[] value) {
    return false;
  }

  @Override
  protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
    return null;
  }

}
