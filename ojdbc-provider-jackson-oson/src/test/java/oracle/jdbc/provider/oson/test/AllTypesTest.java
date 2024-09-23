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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import oracle.jdbc.OracleType;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oson.*;
import oracle.jdbc.provider.oson.model.AllOracleTypes;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.*;

/**
 * The {@code AllTypesTest} class is a JUnit test class for testing the conversion of
 * various types to and from Oracle OSON (Oracle Simple Object Notation) format,
 * along with database interactions.
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class AllTypesTest {

  private static final String URL = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_URL);
  private static final String USER_NAME = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_USERNAME);
  private static final String PASSWORD = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_PASSWORD);
   /**
   * The {@code OsonFactory} object to create OSON parsers and generators.
   */
  private static final OsonFactory osonFactory = new OsonFactory();
  /**
   * The {@code ObjectMapper} used for OSON processing.
   */
  private static final ObjectMapper om = new ObjectMapper(osonFactory);

  /**
   * The connection to the Oracle database.
   */
  Connection conn = null;
  PreparedStatement insertStatement = null;

  ObjectNode objectNode;
  
  AllOracleTypes allTypes = new AllOracleTypes(
      1,
      1L,
      BigInteger.valueOf(5L),
      "string sample",
      'c',
      true,
      1.2,
      1.34f,
      BigDecimal.valueOf(1.345),
      Date.valueOf("2024-03-12"),
      Period.of(12, 5, 0),
      Duration.ofDays(30),
      LocalDateTime.of(2024, 3, 12, 1, 30),
      OffsetDateTime.of(2024, 3, 12, 1, 30, 21, 11, ZoneOffset.ofHours(5))
    );

  /**
   * Sets up the test environment by initializing the database connection and creating
   * the necessary tables for testing.
   */
  @BeforeAll
  public static void beforeAll() {
    om.findAndRegisterModules();
    om.registerModule(new OsonModule());
  }

  @BeforeEach
  public void setup() throws Exception {
      OracleDataSource ods = new OracleDataSource();
      ods.setURL(URL);
      ods.setUser(USER_NAME);
      ods.setPassword(PASSWORD);
      conn = ods.getConnection();
      //setup db tables
      try (Statement stmt = conn.createStatement()) {
        stmt.addBatch("drop table if exists all_types_json");
        stmt.addBatch("create table all_types_json(c1 number, c2 JSON) tablespace tbs1");
        stmt.executeBatch();
      }
      objectNode = om.valueToTree(allTypes);

  }

  /**
   * Converts the {@code AllOracleTypes} object to OSON format.
   *
   * @throws StreamWriteException in case of a write failure.
   * @throws DatabindException    in case of a databind failure.
   * @throws IOException          in case of an I/O failure.
   */
  @Order(1)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingStream(String encoding) throws Exception {
    Assumptions.assumeTrue(conn != null);

    JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator(out, jsonEncoding)) {
        om.writeValue(osonGen, allTypes);
      }
      // Insert the converted bytes
      InsertBytes(out.toByteArray());
      // Retrieve the converted bytes
      AllOracleTypes allTypeAfterConvert =  ReadObject();
      verifyEquals(allTypes, allTypeAfterConvert);
    }
  }


  /**
   * Converts the {@code AllOracleTypes} object to OSON format.
   *
   */
  @Order(1)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingStreamAndObjectNode(String encoding) throws Exception {
    Assumptions.assumeTrue(conn != null);

    JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator(out, jsonEncoding)) {
        om.writeValue(osonGen, objectNode);
      }
      // Insert the converted bytes
      InsertBytes(out.toByteArray());
      // Retrieve the converted bytes
      ObjectNode objectNodeAfterConvert =  ReadObjectNode();
      verifyEquals(objectNode, objectNodeAfterConvert);
    }
  }
  @Order(2)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingFile(String encoding) throws Exception {
    Assumptions.assumeTrue(conn != null);

    JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
    File out = new File("file.json");
    try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator(out, jsonEncoding)) {
      om.writeValue(osonGen, allTypes);
    }
    // Insert the converted bytes
    InsertBytes(Files.readAllBytes(Paths.get(out.getPath())));
    // Retrieve the converted bytes
    AllOracleTypes allTypeAfterConvert =  ReadObject();
    verifyEquals(allTypes, allTypeAfterConvert);

  }

  @Order(3)
  @ParameterizedTest()
  @ValueSource(strings = {"UTF8", "UTF16_BE", "UTF16_LE", "UTF32_BE", "UTF32_LE"})
  public void convertToOsonUsingDataOutput(String encoding) throws Exception {
    Assumptions.assumeTrue(conn != null);

    JsonEncoding jsonEncoding = JsonEncoding.valueOf(encoding);
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteArrayOutputStream)) {
      try (OsonGenerator osonGen = (OsonGenerator) osonFactory.createGenerator((DataOutput) out, jsonEncoding)) {
        om.writeValue(osonGen, allTypes);
      }
      // Insert the converted bytes
      InsertBytes(byteArrayOutputStream.toByteArray());

    }
    // Retrieve the converted bytes
    AllOracleTypes allTypeAfterConvert =  ReadObject();
    verifyEquals(allTypes, allTypeAfterConvert);

  }


  private void InsertBytes(byte[] bytes) throws SQLException {
    try (PreparedStatement pstmt = conn.prepareStatement("insert into all_types_json (c1,c2) values(?,?)")) {
      pstmt.setInt(1, 1);
      pstmt.setBytes(2, bytes);
      pstmt.executeUpdate();
    }
  }

  private AllOracleTypes ReadObject() throws SQLException, IOException {
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("select c1, c2 from all_types_json order by c1")) {
      assertTrue(rs.next());
      return om.readValue(rs.getObject(2, OracleJsonDatum.class).shareBytes(), AllOracleTypes.class);
    }
  }

  private ObjectNode ReadObjectNode() throws SQLException, IOException {
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("select c1, c2 from all_types_json order by c1")) {
      assertTrue(rs.next());
      return om.readValue(rs.getObject(2, OracleJsonDatum.class).shareBytes(), ObjectNode.class);
    }
  }


  private void verifyEquals(AllOracleTypes allTypes, AllOracleTypes allTypeAfterConvert) {
    assertEquals(allTypes, allTypeAfterConvert);
  }

  private void verifyEquals(ObjectNode original, ObjectNode converted) {
    original.fieldNames().forEachRemaining(name -> {
      System.out.println("Name:" + name);
      JsonNode originalNode = original.get(name);
      JsonNode convertedNode = converted.get(name);
      System.out.println(originalNode);
      System.out.println(originalNode.getNodeType());
      System.out.println(convertedNode);
      System.out.println(convertedNode.getNodeType());
    });
  }

}
