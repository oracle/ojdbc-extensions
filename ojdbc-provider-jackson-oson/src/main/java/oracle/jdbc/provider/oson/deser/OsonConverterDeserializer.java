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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import jakarta.persistence.AttributeConverter;
import oracle.jdbc.provider.oson.OsonParser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A generic deserializer that integrates with a custom `AttributeConverter` for deserializing
 * JSON input into Java objects. This class is useful for handling complex or custom mappings
 * of JSON data types to entity attributes in Java.
 *
 * @param <T> the type of the entity attribute to deserialize into
 */
public class OsonConverterDeserializer<T> extends JsonDeserializer<T> {

  /**
   * The attribute converter instance used for the deserialization process.
   */
  private final AttributeConverter converter;

  /**
   * The return type of the `convertToEntityAttribute` method in the converter.
   */
  private final Class<?> elementReturnType;

  /**
   * The return type of the `convertToEntityAttribute` method in the converter.
   */
  private final Class<?> elementInputType;

  /**
   * Constructs an instance of the deserializer for a given `AttributeConverter` class.
   *
   * @param converter the class of the `AttributeConverter` to use
   * @throws RuntimeException if the converter cannot be instantiated or its methods cannot
   *                          be resolved
   */
  public OsonConverterDeserializer(Class<? extends AttributeConverter> converter) {
    try {
      this.converter = converter.getConstructor().newInstance();
      this.elementReturnType = resolveReturnType(converter);;
      this.elementInputType = resolveInputType(converter);
    } catch (InstantiationException 
         | IllegalAccessException 
         | InvocationTargetException 
         | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Resolves the input type of the `convertToDatabaseColumn` method from the given converter class.
   *
   * @param converter the class of the `AttributeConverter`
   * @return the input type of the method, or `Object.class` if not resolvable
   */
  private Class<?> resolveInputType(Class<? extends AttributeConverter> converter) {
    Class<?> iter = converter;
    do{
      Method[] methods = iter.getDeclaredMethods();
      for(Method method : methods) {
        if (method.getName().equals("convertToDatabaseColumn")
            && method.getReturnType() != Object.class){
          Class<T> returnType = (Class<T>) method.getReturnType();
          return returnType;
        }
      }
      iter = iter.getSuperclass();
    }while(iter.getSuperclass() != null);

    return (Class<T>) Object.class;
  }

  /**
   * Resolves the return type of the `convertToEntityAttribute` method from the given converter class.
   *
   * @param converter the class of the `AttributeConverter`
   * @return the return type of the method, or `Object.class` if not resolvable
   */
  private Class<T> resolveReturnType(Class<? extends AttributeConverter> converter) {
    Class<?> iter = converter;
    do{
      Method[] methods = iter.getDeclaredMethods();
      for(Method method : methods) {
        if (method.getName().equals("convertToEntityAttribute")
            && method.getReturnType() != Object.class){
          Class<T> returnType = (Class<T>) method.getReturnType();
          return returnType;
        }
      }
      iter = iter.getSuperclass();
    }while(iter.getSuperclass() != null);

    return (Class<T>) Object.class;
  }

  /**
   * Deserializes JSON input into an entity attribute of type `T` using the custom converter.
   *
   * <p>The method handles various token types, including integers, strings, timestamps,
   * dates, embedded objects, and more, mapping them to the appropriate attribute type
   * using the `AttributeConverter`.</p>
   *
   * @param p     the JSON parser
   * @param ctxt  the deserialization context
   * @return the deserialized entity attribute
   * @throws IOException        if a low-level I/O problem occurs
   * @throws JacksonException   if there is a problem with deserialization
   */
  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
    T result = null;
    switch (p.getCurrentToken()) {

      case VALUE_NUMBER_INT:
        int pInt = p.getIntValue();
        result = ((T) converter.convertToEntityAttribute(pInt));
        break;
      case VALUE_STRING:
      {
        OsonParser parser= (OsonParser)p;
        switch (parser.currentOsonEvent()) {
          case VALUE_TIMESTAMP:
            LocalDateTime dateTime = parser.readLocalDateTime();
            Timestamp timestamp = Timestamp.valueOf(dateTime);
            result = (T) converter.convertToEntityAttribute(timestamp);
            break;
          case VALUE_DATE:
            LocalDate localDate = parser.readLocalDateTime().toLocalDate();
            Date date = Date.valueOf(localDate);
            result = (T) converter.convertToEntityAttribute(date);
            break;

          default:
            String pString = p.getText();
            if (elementInputType.equals(Time.class)) {
              result = (T) converter.convertToEntityAttribute(Time.valueOf(pString));
            }else if (elementInputType.equals(LocalTime.class)) {
              result = (T) converter.convertToEntityAttribute(LocalTime.parse(pString));
            }
            else {
              if (pString.length() == 1) {
                result = ((T) converter.convertToEntityAttribute(Character.valueOf(pString.charAt(0))));
                break;
              }
              result = ((T) converter.convertToEntityAttribute(pString));
            }
            break;
        }
        break;
      }
      case VALUE_EMBEDDED_OBJECT:
        byte[] bytes = p.getBinaryValue();
        result = (T) converter.convertToEntityAttribute(bytes);
        break;

      default:
        String pDString = p.getText();
        if (pDString.length() == 1) {
          result = ((T) converter.convertToEntityAttribute(Character.valueOf(pDString.charAt(0))));
          break;
        }
        result = ((T) converter.convertToEntityAttribute(pDString));

    }
    return result;
  }

}
