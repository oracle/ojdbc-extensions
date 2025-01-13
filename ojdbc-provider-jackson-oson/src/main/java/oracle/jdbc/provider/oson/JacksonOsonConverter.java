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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import oracle.jdbc.spi.OsonConverter;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converter class that facilitates the integration of Jackson and Oson libraries.
 * This class implements the {@link OsonConverter} interface and uses Jackson's {@link ObjectMapper}
 * for serializing and deserializing objects in the Oson format.
 * <p>
 * The class provides methods to serialize objects into Oson format and deserialize Oson format
 * data into Java objects. It uses {@link OsonFactory} to create Oson-compatible generators and parsers.
 * </p>
 *
 * Usage Example:
 * <pre>
 * <code>
 *     Employee employee = getEmployee(); // sample POJO
 *     JacksonOsonConverter conv = new JacksonOsonConverter();
 *     OracleJsonFactory jsonFactory = new OracleJsonFactory();
 *     try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
 *       try (OracleJsonGenerator generator = jsonFactory.createJsonBinaryGenerator(out)) {
 *           conv.serialize(generator, employee);
 *        }
 *        try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
 *            try (OracleJsonParser oParser = jsonFactory.createJsonBinaryParser(in)) {
 *               Employee deserEmp = (Employee) conv.deserialize(oParser, Employee.class);
 *             }
 *         }
 *       }
 * </code>
 * </pre>
 * @see OsonConverter
 * @see ObjectMapper
 * @see OsonFactory
 * @see OracleJsonGenerator
 * @see OracleJsonParser
 * @see JavaType
 */
public class JacksonOsonConverter implements OsonConverter{

  private static final OsonFactory osonFactory = new OsonFactory();
  private static final ObjectMapper om = new ObjectMapper(osonFactory);
  private static final Logger logger = Logger.getLogger(JacksonOsonConverter.class.getName());
  
  static {
    om.findAndRegisterModules();
    om.registerModule(new OsonModule());
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  /**
   * Default constructor.
   */
  public JacksonOsonConverter(){}

  /**
   * Serializes an object into Oson format using the provided {@link OracleJsonGenerator}.
   *
   * @param oGen the {@link OracleJsonGenerator} for writing Oson output
   * @param object the object to serialize
   * @throws IllegalStateException if serialization fails
   */
  @Override
  public void serialize(OracleJsonGenerator oGen, Object object) throws IllegalStateException {
    logger.log(Level.FINEST, "Serializing to OSON");
    try {
      om.writeValue(osonFactory.createGenerator(oGen), object);
    } 
    catch (IOException e) {
      throw new IllegalStateException("Oson conversion failed", e);
    }
  }

  /**
   * Deserializes an object from Oson format using the provided {@link OracleJsonParser}.
   *
   * @param oParser the {@link OracleJsonParser} for reading Oson input
   * @param type the type of the object to deserialize
   * @return the deserialized object
   * @throws IllegalStateException if deserialization fails
   */
  @Override
  public Object deserialize(OracleJsonParser oParser, Class<?> type) throws IllegalStateException {
    logger.log(Level.FINEST, "Deserializing OSON");
    if(!oParser.hasNext()) return null;
    try {
      return om.readValue(osonFactory.createParser(oParser), type);
    } 
    catch (IOException e) {
      throw new IllegalArgumentException("Object parsing from oson failed", e);
    }
  }

  /**
   * Converts a value from one type to another using Jackson's {@link ObjectMapper}.
   *
   * @param fromValue the value to convert
   * @param javaType the target type
   * @return the converted value
   */
  public static Object convertValue(Object fromValue, JavaType javaType) {
    logger.log(Level.FINEST, "Converting value to JavaType");
    return om.convertValue(fromValue, javaType);
  }

  /**
   * Get the object mapper instances with registered custom modules.
   * @return the Object mapper with registered modules
   */
  public static ObjectMapper getObjectMapper() {
    return om;
  }

  /**
   * Get the OsonFactory instance.
   * @return the OsonFactory
   */
  public static OsonFactory getOsonFactory() {
    return osonFactory;
  }
}
