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

import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.OracleType;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.jdbc.provider.oson.OsonTestProperty;
import oracle.jdbc.provider.oson.model.EmployeeInstances;
import oracle.jdbc.provider.oson.model.Employee;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.IOException;
import java.sql.*;

/**
 * The {@code DBTest} class is a JUnit test class that tests insertion and retrieval
 * of JSON data into and from an Oracle database using OSON (Oracle Simple Object Notation).
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class DBTest {

  /**
   * The connection to the Oracle database.
   */
  Connection conn = null;

  /**
   * The OSON factory for creating OSON parsers and generators.
   */
  OsonFactory osonFactory = new OsonFactory();

  /**
   * The {@code ObjectMapper} used for OSON processing.
   */
  ObjectMapper om = new ObjectMapper(osonFactory);

  /**
   * The byte array that holds the serialized object data.
   */
  byte[] objectBytes = null;

  /**
   * The implyee instance inserted into DB.
   */
  Employee insertedEmployee = null;

  /**
   * Sets up the database connection and the required tables before all tests are run.
   */
  @BeforeAll
  public void setup() {
    try {
      String url = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_URL);
      String userName = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_USERNAME);
      String password = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_PASSWORD);
      OracleDataSource ods = new OracleDataSource();
      ods.setURL(url);
      ods.setUser(userName);
      ods.setPassword(password);
      conn = ods.getConnection();

      //setup db tables
      try(Statement stmt = conn.createStatement()) {
        stmt.execute("drop table if exists emp_json");
        stmt.execute("create table emp_json(c1 number, c2 JSON) tablespace tbs1");
      }


    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Inserts an {@code Employee} object into the database in JSON format.
   *
   * @throws IOException   if there is an error during object serialization.
   * @throws SQLException  if there is a database access error.
   */
  @Test
  @Order(1)
  public void insertIntoDatabase() throws IOException, SQLException {
    Assumptions.assumeTrue(conn != null);
    insertedEmployee = EmployeeInstances.getEmployee();
    try(PreparedStatement pstmt = conn.prepareStatement("insert into emp_json (c1,c2) values(?,?)")) {
      pstmt.setInt(1, 1);
      pstmt.setObject(2, insertedEmployee, OracleType.JSON);
      pstmt.execute();
    }

  }

  /**
   * Retrieves the {@code Employee} object from the database and prints its details.
   *
   * @throws Exception if there is an error during retrieval or object conversion.
   */
  @Test
  @Order(2)
  public void retrieveFromDatabase() throws Exception {
    Assumptions.assumeTrue(conn != null);
    try(Statement stmt = conn.createStatement()) {
      try(ResultSet rs = stmt.executeQuery("select c1, c2 from emp_json order by c1")) {
        while(rs.next()) {
          Employee retrievedEmployee = rs.getObject(2, Employee.class);
          Assertions.assertEquals(insertedEmployee, retrievedEmployee);
        }
      }
    }
  }

  @AfterAll
  public void tearDown() throws SQLException {
    Assumptions.assumeTrue(conn != null);
    conn.close();
  }
}
