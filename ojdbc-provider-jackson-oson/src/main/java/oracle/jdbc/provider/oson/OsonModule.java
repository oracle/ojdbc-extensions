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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import jakarta.persistence.Convert;
import jakarta.persistence.OneToMany;
import oracle.jdbc.provider.oson.deser.*;
import oracle.jdbc.provider.oson.ser.*;

import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;


/**
 * Jackson module that registers serializers and deserializers for specific data types
 * of the OsonProvider library. This module extends {@link SimpleModule} and is used
 * to integrate Oson's JSON processing capabilities with Jackson's serialization and deserialization.
 *
 * The module registers the following Oson custom serializers and deserializers:
 * <ul>
 *   <li>{@link LocalDateTime} - {@link OsonLocalDateTimeDeserializer} and {@link OsonLocalDateTimeSerializer}</li>
 *   <li>{@link OffsetDateTime} - {@link OsonOffsetDateTimeDeserializer} and {@link OsonOffsetDateTimeSerializer}</li>
 *   <li>{@link Period} - {@link OsonPeriodDeserializer} and {@link OsonPeriodSerializer}</li>
 *   <li>{@link Duration} - {@link OsonDurationDeserializer} and {@link OsonDurationSerializer}</li>
 *   <li>{@link BigInteger} - {@link OsonBigIntegerDeserializer} and {@link OsonBigIntegerSerializer}</li>
 *   <li>{@link Year} - {@link OsonYearDeserializer} and {@link OsonYearSerializer}</li>
 *   <li>{@code byte[]} - {@link OsonByteDeserializer} and {@link OsonByteSerializer}</li>
 * </ul>
 *
 */
public class OsonModule extends SimpleModule {
  public static String providerVersion ;
  public static String groupId;
  public static String artifactId;
  public static Version VERSION;

  static  {
    instantiateProviderVersionInfo();
    VERSION = VersionUtil.parseVersion(providerVersion, groupId, artifactId);
  }

  private static void instantiateProviderVersionInfo() {
    String propertiesFilePath = "/META-INF/maven/com.oracle.database.jdbc/ojdbc-provider-jackson-oson/pom.properties";
    Properties properties = new Properties();
    try (InputStream inputStream = OsonModule.class.getResourceAsStream(propertiesFilePath)) {
      if (inputStream != null) {
        properties.load(inputStream);

        providerVersion  = properties.getProperty("version");
        groupId = properties.getProperty("artifactId");
        artifactId = properties.getProperty("groupId");

      } else {
        // when the tests are run locally using IDE.
        providerVersion = "1.0";
        groupId = "com.oracle.database.jdbc";
        artifactId = "ojdbc-provider-jackson-oson";
      }
    } catch (Exception e) {
    }
  }

  public OsonModule() {
    super(VERSION);

    addDeserializer(LocalDateTime.class, OsonLocalDateTimeDeserializer.INSTANCE);
    addSerializer(LocalDateTime.class, OsonLocalDateTimeSerializer.INSTANCE);

    addDeserializer(OffsetDateTime.class, OsonOffsetDateTimeDeserializer.INSTANCE);
    addSerializer(OffsetDateTime.class, OsonOffsetDateTimeSerializer.INSTANCE);

    addDeserializer(Period.class, OsonPeriodDeserializer.INSTANCE);
    addSerializer(Period.class, OsonPeriodSerializer.INSTANCE);

    addDeserializer(Duration.class, OsonDurationDeserializer.INSTANCE);
    addSerializer(Duration.class, OsonDurationSerializer.INSTANCE);

    addDeserializer(BigInteger.class, OsonBigIntegerDeserializer.INSTANCE);
    addSerializer(BigInteger.class, OsonBigIntegerSerializer.INSTANCE);

    addDeserializer(Year.class, OsonYearDeserializer.INSTANCE);
    addSerializer(Year.class, OsonYearSerializer.INSTANCE);

    addDeserializer(byte[].class, OsonByteDeserializer.INSTANCE);
    addSerializer(byte[].class, OsonByteSerializer.INSTANCE);

    addDeserializer(java.util.Date.class, OsonDateDeserializer.INSTANCE);
    addSerializer(java.util.Date.class, OsonDateSerializer.INSTANCE);

    addDeserializer(java.sql.Date.class, OsonSqlDateDeserializer.INSTANCE);
    addDeserializer(Timestamp.class, OsonTimeStampDeserializer.INSTANCE);

    addDeserializer(UUID.class, OsonUUIDDeserializer.INSTANCE);
    addDeserializer(Boolean.class, OsonBooleanDeserializer.INSTANCE);


  }

  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);

    context.addBeanSerializerModifier(new BeanSerializerModifier() {
      @Override
      public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        Iterator<PropertyWriter> properties = serializer.properties();
        while (properties.hasNext()) {
          BeanPropertyWriter writer = (BeanPropertyWriter) properties.next();
          if (writer.getMember().hasAnnotation(Convert.class)) {
            Convert annotation = writer.getMember().getAnnotation(Convert.class);
            Class<? extends jakarta.persistence.AttributeConverter> converterClass = annotation.converter();
            if (writer.getType().isArrayType()) {
              JsonSerializer<?> mySerializer = new OsonConverterArraySerializer(converterClass);
              writer.assignSerializer((JsonSerializer<Object>) mySerializer);
            } else {
              JsonSerializer<Object> mySerializer = new OsonConverterSerialiser(converterClass);
              writer.assignSerializer(mySerializer);
            }


          }
        }
        return serializer;
      }

      @Override
      public JsonSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        return new OsonEnumSerializer(false,false);
      }
    });

    context.addBeanDeserializerModifier(new BeanDeserializerModifier() {

      @Override
      public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        if(deserializer instanceof BeanDeserializer){
          Iterator<SettableBeanProperty> properties = ((BeanDeserializer) deserializer).properties();
          while (properties.hasNext()) {
            SettableBeanProperty property = properties.next();
            if (property.getMember().hasAnnotation(Convert.class)) {
              Convert annotation = property.getMember().getAnnotation(Convert.class);
              Class<? extends jakarta.persistence.AttributeConverter> converterClass = annotation.converter();

              if(property.getType().isArrayType()){
                JsonDeserializer<Object[]> deser = new OsonConverterArrayDeserializer(converterClass);
                ((BeanDeserializer) deserializer).replaceProperty(property,property.withValueDeserializer(deser));
              } else {
                JsonDeserializer<Object> deser = new OsonConverterDeserializer(converterClass);
                ((BeanDeserializer) deserializer).replaceProperty(property,property.withValueDeserializer(deser));
              }
            }
          }
          return deserializer;
        }
        return deserializer;
      }
    });
  }
}
