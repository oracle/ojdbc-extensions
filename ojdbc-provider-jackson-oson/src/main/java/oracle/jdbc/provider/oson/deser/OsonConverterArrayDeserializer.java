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

public class OsonConverterArrayDeserializer<T> extends JsonDeserializer<T[]> {

    private final Class<?> elementType;
    private final OsonConverterDeserializer<T> deserializer;

    public OsonConverterArrayDeserializer(Class<? extends AttributeConverter> converter) {
        elementType = resolveType(converter);
        deserializer = new OsonConverterDeserializer<>(converter);
    }

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
