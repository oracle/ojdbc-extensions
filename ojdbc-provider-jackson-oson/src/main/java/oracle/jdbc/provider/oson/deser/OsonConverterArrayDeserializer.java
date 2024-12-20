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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import jakarta.persistence.AttributeConverter;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;

/**
 * A custom deserializer for arrays of elements that use a specified `AttributeConverter` to
 * handle the deserialization of individual elements. This class integrates with the
 * `OsonConverterDeserializer` to support complex or custom mappings for arrays.
 *
 * @param <T> the type of the array elements
 */
public class OsonConverterArrayDeserializer<T> extends JsonDeserializer<T[]> {

  /**
   * The type of the elements in the array.
   */
  private final Class<?> elementType;

  /**
   * The deserializer used to process individual elements of the array.
   */
  private final OsonConverterDeserializer<T> deserializer;

  /**
   * Constructs a deserializer for an array of elements with a given `AttributeConverter`.
   *
   * @param converter the class of the `AttributeConverter` to use for element deserialization
   */
  public OsonConverterArrayDeserializer(Class<? extends AttributeConverter> converter) {
    elementType = resolveType(converter);
    deserializer = new OsonConverterDeserializer<>(converter);
  }

  /**
   * Resolves the element type of the array by examining the `convertToEntityAttribute`
   * method of the provided `AttributeConverter`.
   *
   * @param converter the class of the `AttributeConverter`
   * @return the return type of the `convertToEntityAttribute` method, or `Object.class` if not resolvable
   */
  private Class<?> resolveType(Class<? extends AttributeConverter> converter) {
    Class<?> iter = converter;
    do{
      Method[] methods = iter.getDeclaredMethods();
      for(Method method : methods) {
        if (method.getName().equals("convertToEntityAttribute")
            && method.getReturnType() != Object.class){
          return method.getReturnType();
        }
      }
      iter = iter.getSuperclass();
    }while(iter.getSuperclass() != null);

    return Object.class;
  }

  /**
   * Deserializes JSON input into an array of elements of type `T` using the custom converter.
   *
   * <p>The method processes JSON arrays and delegates the deserialization of individual
   * elements to the `OsonConverterDeserializer`.</p>
   *
   * @param p     the JSON parser
   * @param ctxt  the deserialization context
   * @return the deserialized array of elements
   * @throws IOException        if a low-level I/O problem occurs
   * @throws JacksonException   if there is a problem with deserialization
   */
  @Override
  public T[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
    ArrayList<T> result = new ArrayList<>();
    if (p.getCurrentToken() == JsonToken.START_ARRAY) {
      while (p.nextToken() != JsonToken.END_ARRAY) {
        result.add(deserializer.deserialize(p, ctxt));
      }
    }
    T[] array = (T[]) Array.newInstance(elementType, result.size());
    return result.toArray(array);
  }
}
