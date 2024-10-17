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

package oracle.jdbc.provider.oson.test;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.provider.oson.JacksonOsonConverter;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.jdbc.provider.oson.model.AnnonationTest;
import oracle.jdbc.provider.oson.model.AnnotationTestInstances;
import oracle.jdbc.provider.oson.model.Employee;
import oracle.jdbc.provider.oson.model.EmployeeInstances;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.regex.Pattern;

/**
 * The {@code SerializerTest} class tests the serialization and deserialization of
 * {@link Employee} objects using the {@link JacksonOsonConverter} and Oracle JSON .
 * 
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SerializerTest {

  /**
   * Tests the serialization and deserialization of an {@link Employee} object.
   * <p>
   * This test uses the {@link JacksonOsonConverter} to serialize an {@code Employee} object
   * into binary JSON format using an {@link OracleJsonGenerator}, and then deserializes it
   * back into an {@code Employee} object using an {@link OracleJsonParser}.
   * The test verifies that the deserialized object is equal to the original.
   * </p>
   * */  
  @Test
  @Order(1)
  public void serialiZerTest() throws IOException {
      Employee employee = EmployeeInstances.getEmployee();

      JacksonOsonConverter conv = new JacksonOsonConverter();
      OracleJsonFactory jsonFactory = new OracleJsonFactory();
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          try (OracleJsonGenerator generator = jsonFactory.createJsonBinaryGenerator(out)) {
              conv.serialize(generator, employee);
          }
          try(ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
              try (OracleJsonParser oParser = jsonFactory.createJsonBinaryParser(in)) {
                  Employee deserEmp = (Employee) conv.deserialize(oParser, Employee.class);
                  Assertions.assertEquals(employee, deserEmp);
              }
          }

      }

  }

    /**
     * Tests the serialization and deserialization of an {@link AnnonationTest} object with
     * Jackson Annotations.
     * <p>
     * This test uses the {@link JacksonOsonConverter} to serialize an {@code Employee} object
     * into binary JSON format using an {@link OracleJsonGenerator}, and then deserializes it
     * back into an {@code Employee} object using an {@link OracleJsonParser}.
     * The test verifies that the deserialized object is equal to the original.
     * </p>
     * */
    @Test
  @Order(2)
  public void serialiZerTest2() throws IOException {
      AnnonationTest annOrig = AnnotationTestInstances.getRandomInstance();

      JacksonOsonConverter conv = new JacksonOsonConverter();
      OracleJsonFactory jsonFactory = new OracleJsonFactory();
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          try (OracleJsonGenerator generator = jsonFactory.createJsonBinaryGenerator(out)) {
              conv.serialize(generator, annOrig);
          }
          try(ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
              try (OracleJsonParser oParser = jsonFactory.createJsonBinaryParser(in)) {
                  AnnonationTest annDeser = (AnnonationTest) conv.deserialize(oParser, AnnonationTest.class);
                  String regex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$";
                  Pattern pattern = Pattern.compile(regex);
                  Assertions.assertTrue(pattern.matcher(annDeser.getLocalDateTime().toString()).matches());
              }
          }

      }

  }

    /**
     * Tests the serialization and deserialization of an {@link Employee} object.
     * <p>
     * This test uses the {@link JacksonOsonConverter} to serialize an {@code Employee} object
     * into binary JSON format using an {@link OracleJsonGenerator}, and then deserializes it
     * back into an {@code Employee} object using an {@link OracleJsonParser}.
     * The test verifies that the deserialized object is equal to the original.
     * </p>
     * */
    @Test
    @Order(3)
    public void serialiZerTest3() throws IOException {
        Employee employee = EmployeeInstances.getEmployee();

        JacksonOsonConverter conv = new JacksonOsonConverter();
        ObjectMapper mapper = conv.getObjectMapper();
        OsonFactory jsonFactory = new OsonFactory();
        OracleJsonFactory oracleJsonFactory = new OracleJsonFactory();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (OracleJsonGenerator generator = oracleJsonFactory.createJsonBinaryGenerator(out)) {
                conv.serialize(generator,employee);
            }
            try(ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray())) {
                DataInput in = new DataInputStream(input);
                try (JsonParser oParser = jsonFactory.createParser(in)) {
                    Employee deserEmp = mapper.readValue(oParser, Employee.class);
                    Assertions.assertEquals(employee, deserEmp);
                }
            }

        }
    }

    /**
     * Tests the serialization and deserialization of an {@link AnnonationTest} object.
     * <p>
     * This test uses the {@link JacksonOsonConverter} to serialize an {@code AnnonationTest} object
     * into binary JSON format using an {@link OracleJsonGenerator}, and then deserializes it
     * back into an {@code Employee} object using an {@link OracleJsonParser}.
     * The test verifies that the deserialized object is equal to the original.
     * </p>
     * */
    @Test
    @Order(4)
    public void serialiZerTest4() throws IOException {
        AnnonationTest employee = AnnotationTestInstances.getRandomInstance();

        JacksonOsonConverter conv = new JacksonOsonConverter();
        ObjectMapper mapper = conv.getObjectMapper();
        OsonFactory jsonFactory = new OsonFactory();
        OracleJsonFactory oracleJsonFactory = new OracleJsonFactory();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (OracleJsonGenerator generator = oracleJsonFactory.createJsonBinaryGenerator(out)) {
                conv.serialize(generator,employee);
            }

            String jsonString = mapper.writeValueAsString(employee);
            Reader reader = new StringReader(jsonString);
            try (JsonParser oParser = jsonFactory.createParser(reader)) {
                AnnonationTest deserEmp = mapper.readValue(oParser, AnnonationTest.class);
                Assertions.assertEquals(employee, deserEmp);
            }

        }
    }

    /**
     * Tests the serialization and deserialization of an {@link Employee} object.
     * <p>
     * This test uses the {@link JacksonOsonConverter} to serialize an {@code Employee} object
     * into binary JSON format using an {@link OracleJsonGenerator}, and then deserializes it
     * back into an {@code Employee} object using an {@link OracleJsonParser}.
     * The test verifies that the deserialized object is equal to the original.
     * </p>
     * */
    @Test
    @Order(5)
    public void serialiZerTest5() throws IOException {
        Employee employee = EmployeeInstances.getEmployee();

        JacksonOsonConverter conv = new JacksonOsonConverter();
        ObjectMapper mapper = conv.getObjectMapper();
        OsonFactory jsonFactory = new OsonFactory();
        OracleJsonFactory oracleJsonFactory = new OracleJsonFactory();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (OracleJsonGenerator generator = oracleJsonFactory.createJsonBinaryGenerator(out)) {
                conv.serialize(generator,employee);
            }
            try {
                File tmpFile = File.createTempFile("oson",null,null);
                FileOutputStream fos = new FileOutputStream(tmpFile);
                fos.write(out.toByteArray());
                fos.close();

                FileInputStream fis = new FileInputStream(tmpFile);
                DataInput dis = new DataInputStream(fis);
                try (JsonParser oParser = jsonFactory.createParser(dis)) {
                    Employee deserEmp = mapper.readValue(oParser, Employee.class);
                    Assertions.assertEquals(employee, deserEmp);
                }
                fis.close();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}
