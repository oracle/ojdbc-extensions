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

public class OsonConverterDeserializer<T> extends JsonDeserializer<T> {

    private final AttributeConverter converter;
    private final Class<?> elementReturnType;
    private final Class<?> elementInputType;

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
                        LocalDateTime dateTime = parser.getLocalDateTime();
                        Timestamp timestamp = Timestamp.valueOf(dateTime);
                        result = (T) converter.convertToEntityAttribute(timestamp);
                        break;
                    case VALUE_DATE:
                        LocalDate localDate = parser.getLocalDateTime().toLocalDate();
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
