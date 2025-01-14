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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oson.*;
import oracle.jdbc.provider.oson.model.Employee;
import oracle.jdbc.provider.oson.model.EmployeeInstances;
import oracle.sql.json.OracleJsonDatum;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code AllTypesTest} class is a JUnit test class for testing the conversion of
 * various types to and from Oracle OSON (Oracle Simple Object Notation) format,
 * along with database interactions.
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class EncondingTest {

   /**
   * The {@code OsonFactory} object to create OSON parsers and generators.
   */
  private static final OsonFactory osonFactory = new OsonFactory();
  /**
   * The {@code ObjectMapper} used for OSON processing.
   */
  private static final ObjectMapper om = new ObjectMapper(osonFactory);

  private static final Employee employee = EmployeeInstances.getEmployee();

  private static final ObjectNode objectNode;

  static {
    om.findAndRegisterModules();
    om.registerModule(new OsonModule());
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectNode = om.valueToTree(employee);
  }

  @BeforeAll
  public void setup() throws Exception {
    try (Connection conn = getConnection()){
      //setup db tables
      try (Statement stmt = conn.createStatement()) {
        stmt.addBatch("drop table if exists all_types_json");
        stmt.addBatch("create table all_types_json(c1 number, c2 JSON)");
        stmt.executeBatch();
      }
    }

  }

  /**
   * Converts the {@code AllOracleTypes} object to OSON format.
   *
   * @throws StreamWriteException in case of a write failure.
   * @throws DatabindException  in case of a databind failure.
   * @throws IOException      in case of an I/O failure.
   */
  @Order(1)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingStream(String encoding) throws Exception {
    try(Connection conn = getConnection()) {
      Assumptions.assumeTrue(conn != null);

      try(Statement stmt = conn.createStatement()) {
        stmt.execute("truncate table all_types_json");
      }
      JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator(out, jsonEncoding)) {
          om.writeValue(osonGen, employee);
        }
        // Insert the converted bytes
        InsertBytes(out.toByteArray(),conn);
        // Retrieve the converted bytes
        Employee employeeAfterConvert =  ReadObject(conn);
        verifyEquals(employee, employeeAfterConvert);
      }
    }
  }


  @Order(2)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingFile(String encoding) throws Exception {
    try (Connection conn = getConnection()) {
      Assumptions.assumeTrue(conn != null);
      try(Statement stmt = conn.createStatement()) {
        stmt.execute("truncate table all_types_json");
      }
      JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
      File out = new File("file.json");
      try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator(out, jsonEncoding)) {
        om.writeValue(osonGen, employee);
      }
      // Insert the converted bytes
      InsertBytes(Files.readAllBytes(Paths.get(out.getPath())),conn);
      // Retrieve the converted bytes
      Employee employeeAfterConvert =  ReadObject(conn);
      verifyEquals(employee, employeeAfterConvert);
    }
  }

  @Order(3)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingDataOutput(String encoding) throws Exception {

    try (Connection conn = getConnection()) {
      Assumptions.assumeTrue(conn != null);
      try(Statement stmt = conn.createStatement()) {
        stmt.execute("truncate table all_types_json");
      }
      JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
      try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
           DataOutputStream out = new DataOutputStream(byteArrayOutputStream)) {
        try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator((DataOutput) out, jsonEncoding)) {
          om.writeValue(osonGen, employee);
        }
        // Insert the converted bytes
        InsertBytes(byteArrayOutputStream.toByteArray(),conn);
      }
      // Retrieve the converted bytes
      Employee employeeAfterConvert =  ReadObject(conn);
      verifyEquals(employee, employeeAfterConvert);
    }
  }


  /**
   * Converts the {@code AllOracleTypes} object to OSON format.
   *
   */
  @Order(5)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingStreamAndObjectNode(String encoding) throws Exception {

    try(Connection conn = getConnection()) {
      Assumptions.assumeTrue(conn != null);
      try(Statement stmt = conn.createStatement()) {
        stmt.execute("truncate table all_types_json");
      }
      JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator(out, jsonEncoding)) {
          om.writeValue(osonGen, objectNode);
        }
        // Insert the converted bytes
        InsertBytes(out.toByteArray(),conn);
        ObjectNode intermediateNode = om.readValue(out.toByteArray(), ObjectNode.class);
        verifyEquals(objectNode, intermediateNode);
        // Retrieve the converted bytes
        ObjectNode objectNodeAfterConvert =  ReadObjectNode(conn);
        verifyEquals(objectNode, objectNodeAfterConvert);
     }
    }
  }

  @Order(6)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingFileAndObjectNode(String encoding) throws Exception {

    try(Connection conn = getConnection()) {
      Assumptions.assumeTrue(conn != null);
      try(Statement stmt = conn.createStatement()) {
        stmt.execute("truncate table all_types_json");
      }
      JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
      File out = new File("file.json");
      try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator(out, jsonEncoding)) {
        om.writeValue(osonGen, objectNode);
      }
      // Insert the converted bytes
      InsertBytes(Files.readAllBytes(Paths.get(out.getPath())),conn);
      // Retrieve the converted bytes
      ObjectNode objectNodeAfterConvert =  ReadObjectNode(conn);
      verifyEquals(objectNode, objectNodeAfterConvert);
    }
  }

  @Order(7)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingDataOutputAndObjectNode(String encoding) throws Exception {
    try (Connection conn = getConnection()) {
      Assumptions.assumeTrue(conn != null);
      try(Statement stmt = conn.createStatement()) {
        stmt.execute("truncate table all_types_json");
      }
      JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
      try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
           DataOutputStream out = new DataOutputStream(byteArrayOutputStream)) {
        try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator((DataOutput) out, jsonEncoding)) {
          om.writeValue(osonGen, objectNode);
        }
        // Insert the converted bytes
        InsertBytes(byteArrayOutputStream.toByteArray(),conn);

      }
      // Retrieve the converted bytes
      ObjectNode objectNodeAfterConvert =  ReadObjectNode(conn);
      verifyEquals(objectNode, objectNodeAfterConvert);
    }
  }

  /**
   * Create an Oracle Connection and return the instance.
   * @return
   * @throws SQLException
   */
  private Connection getConnection() throws SQLException {
    final String URL = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_URL);
    final String USER_NAME = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_USERNAME);
    final String PASSWORD = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_PASSWORD);
    OracleDataSource ods = new OracleDataSource();
    ods.setURL(URL);
    ods.setUser(USER_NAME);
    ods.setPassword(PASSWORD);
    return ods.getConnection();
  }

  private void InsertBytes(byte[] bytes, Connection conn) throws SQLException {
    try (PreparedStatement pstmt = conn.prepareStatement("insert into all_types_json (c1,c2) values(?,?)")) {
      pstmt.setInt(1, 1);
      pstmt.setBytes(2, bytes);
      pstmt.executeUpdate();
    }
  }

  private Employee ReadObject(Connection conn) throws SQLException, IOException {
    try (Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("select c1, c2 from all_types_json order by c1")) {
      assertTrue(rs.next());
      return om.readValue(rs.getObject(2, OracleJsonDatum.class).shareBytes(), Employee.class);
    }
  }

  private ObjectNode ReadObjectNode(Connection conn) throws SQLException, IOException {
    try (Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("select c1, c2 from all_types_json order by c1")) {
      assertTrue(rs.next());
      return om.readValue(rs.getObject(2, OracleJsonDatum.class).shareBytes(), ObjectNode.class);
    }
  }

  private void verifyEquals(Employee employee, Employee employeeAfterConvert) {
    assertEquals(employee, employeeAfterConvert);
  }

  private void verifyEquals(ObjectNode original, ObjectNode converted) throws Exception {
    assertEquals(original.get("employeeId").intValue(), converted.get("employeeId").intValue());
    assertEquals(original.get("salary").decimalValue(), converted.get("salary").decimalValue());
    assertEquals(original.get("largeNumber").bigIntegerValue(), converted.get("largeNumber").bigIntegerValue());
    assertEquals(original.get("firstName").toString(), converted.get("firstName").toString());
    assertEquals(original.get("middleInitial").toString(), converted.get("middleInitial").toString());
    assertEquals(original.get("lastName").toString(), converted.get("lastName").toString());
    assertEquals(om.convertValue(original.get("hireDate"), Date.class), om.convertValue(converted.get("hireDate"), Date.class));
    assertEquals(om.convertValue(original.get("localHireDate"), LocalDate.class), om.convertValue(converted.get("localHireDate"), LocalDate.class));
    assertEquals(om.convertValue(original.get("loginInstant"), Instant.class), om.convertValue(converted.get("loginInstant"), Instant.class));
    assertEquals(om.convertValue(original.get("loginLocalTime"), LocalTime.class), om.convertValue(converted.get("loginLocalTime"), LocalTime.class));
    assertEquals(om.convertValue(original.get("salaryMonthDay"), MonthDay.class), om.convertValue(converted.get("salaryMonthDay"), MonthDay.class));
    assertEquals(om.convertValue(original.get("incrementYearmonth"), YearMonth.class), om.convertValue(converted.get("incrementYearmonth"), YearMonth.class));
    assertEquals(original.get("logoutTime").longValue(), converted.get("logoutTime").longValue());
    assertEquals(om.convertValue(original.get("startTime"), Time.class), om.convertValue(converted.get("startTime"), Time.class));
    assertEquals(om.convertValue(original.get("lastUpdated"), Timestamp.class), om.convertValue(converted.get("lastUpdated"), Timestamp.class));
    assertEquals(om.convertValue(original.get("localDateTime"), LocalDateTime.class), om.convertValue(converted.get("localDateTime"), LocalDateTime.class));
    assertEquals(om.convertValue(original.get("offsetDateTime"), OffsetDateTime.class), om.convertValue(converted.get("offsetDateTime"), OffsetDateTime.class));
    assertEquals(om.convertValue(original.get("yearJoined"), Year.class), om.convertValue(converted.get("yearJoined"), Year.class));
    assertEquals(om.convertValue(original.get("durationOfEmployment"), Duration.class), om.convertValue(converted.get("durationOfEmployment"), Duration.class));
    assertEquals(original.get("resume").toString(), converted.get("resume").toString());
    assertEquals(original.get("active").booleanValue(), converted.get("active").booleanValue());
    assertEquals(om.convertValue(original.get("vacationZonedDateTime"), ZonedDateTime.class), om.convertValue(converted.get("vacationZonedDateTime"), ZonedDateTime.class));
    assertArrayEquals(original.get("rawData").binaryValue(), converted.get("rawData").binaryValue());
    assertArrayEquals(original.get("picture").binaryValue(), converted.get("picture").binaryValue());
  }

}
