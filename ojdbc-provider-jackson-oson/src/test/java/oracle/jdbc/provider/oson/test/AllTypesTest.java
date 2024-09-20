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

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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

  /**
   * The connection to the Oracle database.
   */
  Connection conn = null;

  /**
   * The {@code OsonFactory} object to create OSON parsers and generators.
   */
  OsonFactory osonFactory = new OsonFactory();

  /**
   * The {@code ObjectMapper} used for OSON processing.
   */
  ObjectMapper om = new ObjectMapper(osonFactory);

  /**
   * The {@code OsonGenerator} for generating OSON-encoded data.
   */
  OsonGenerator osonGen = null;

  /**
   * The byte array containing the OSON-encoded data to be checked.
   */
  byte[] osonTocheck = null;

  /**
   * A {@code ByteArrayOutputStream} to hold the OSON-encoded data.
   */
  ByteArrayOutputStream out = new ByteArrayOutputStream();

  /**
   * A test object of {@code AllOracleTypes} containing various types of data.
   */
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
  public void setup() {
    try {
      String url = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_URL);
      String userName = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_USERNAME);
      String password = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_PASSWORD);

      om.findAndRegisterModules();
      om.registerModule(new OsonModule());
      osonGen = (OsonGenerator) osonFactory.createGenerator(out);
      
      OracleDataSource ods = new OracleDataSource();
      ods.setURL(url);
      ods.setUser(userName);
      ods.setPassword(password);
      conn = ods.getConnection();
      
      //setup db tables
      Statement stmt = conn.createStatement();
      stmt.execute("drop table if exists all_types_json");
      stmt.execute("create table all_types_json(c1 number, c2 JSON) tablespace tbs1");
      stmt.close();
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }

  /**
   * Converts the {@code AllOracleTypes} object to OSON format.
   *
   * @throws StreamWriteException in case of a write failure.
   * @throws DatabindException    in case of a databind failure.
   * @throws IOException          in case of an I/O failure.
   */
  @Test
  @Order(1)
  public void convertToOson() throws StreamWriteException, DatabindException, IOException {
    Assumptions.assumeTrue(conn != null);
    om.writeValue(osonGen, allTypes);
    osonGen.close();
    osonTocheck = out.toByteArray();

    System.out.println("Binary encoded OSON: " + osonTocheck.length + " bytes");

  }

  /**
   * Converts the OSON-encoded binary data back to a {@code AllOracleTypes} object.
   *
   * @throws Exception in case of a failure during parsing or conversion.
   */
  @Test
  @Order(2)
  public void convertFromOson() throws Exception {
    Assumptions.assumeTrue(conn != null);
    try (OsonParser parser = (OsonParser) osonFactory.createParser(osonTocheck) ) {
      
      AllOracleTypes ne = om.readValue(parser, AllOracleTypes.class);
      System.out.println("POJO after conversion from OSON: " + ne.toString());
    }
    
  }

  /**
   * Inserts the {@code AllOracleTypes} object into the database in OSON format.
   *
   * @throws Exception in case of a failure during database insertion.
   */
  @Test
  @Order(3)
  public void insertIntoDB() throws Exception {
    Assumptions.assumeTrue(conn != null);
    try (OsonParser parser = (OsonParser) osonFactory.createParser(osonTocheck) ) {
      
      // insert into db
      PreparedStatement pstmt = conn.prepareStatement("insert into all_types_json (c1,c2) values(?,?)");
      pstmt.setInt(1, 1);
      pstmt.setObject(2,  allTypes, OracleType.JSON);
      pstmt.execute();
      pstmt.close();
    }
    
  }

  /**
   * Retrieves the OSON-encoded data from the database, converts it to a {@code JsonNode},
   * and then back to a {@code AllOracleTypes} object.
   *
   * @throws Exception in case of a failure during retrieval or conversion.
   */
  @Test
  @Order(4)
  public void retieveFromDatabase() throws Exception {
    Assumptions.assumeTrue(conn != null);
        //retrieve from db
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("select c1, c2 from all_types_json order by c1");
    while(rs.next()) {
      OracleJsonValue oson_value = rs.getObject(2, OracleJsonValue.class);
      byte[] osonBytes = rs.getObject(2, OracleJsonDatum.class).shareBytes();

      if (oson_value != null && oson_value instanceof OracleJsonObject) {
              OracleJsonObject oson_obj = (OracleJsonObject) oson_value;
        
              //OracleJsonObject value
              System.out.println("OracleJsonObject: " + oson_obj);
            }
            if(osonBytes!=null && osonBytes.length>0) {
              System.out.println("Oson bytes length: " + osonBytes.length);
        
              // JsonNode mapping
              JsonNode node = om.readValue(osonBytes, JsonNode.class);
              System.out.println("JsonNode: " + node.toPrettyString());
            }
            if(osonBytes!=null && osonBytes.length>0) {
              System.out.println("Oson bytes length: " + osonBytes.length);
        
              // POJO mapping
              AllOracleTypes types = om.readValue(osonBytes, AllOracleTypes.class);
              System.out.println("POJO after retrieval from DB and conversion: " + types.toString());
            }
    }
    rs.close();
    stmt.close();
    
  }
}
