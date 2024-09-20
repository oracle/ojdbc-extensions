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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import oracle.jdbc.OracleType;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oson.JacksonOsonConverter;
import oracle.jdbc.provider.oson.OsonTestProperty;
import oracle.jdbc.provider.oson.model.Phone;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ListTypeTest} class is a JUnit test class that tests insertion and retrieval
 * of a list of {@link Phone} objects into and from an Oracle database using JSON data types.
 * <p>
 * The class is annotated with {@link TestInstance} with {@code PER_CLASS} lifecycle,
 * meaning the test class will be instantiated only once. Tests are executed in a specific order
 * using {@link OrderAnnotation}.
 * </p>
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class ListTypeTest {
  static Connection conn = null;

  /**
   * Sets up the database connection and the required tables before all tests are run.
   */
  @BeforeAll
  public void setup() {
    try {
        String url = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_URL);
        String userName = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_USERNAME);
        String password = TestProperties.getOrAbort(OsonTestProperty.JACKSON_OSON_PASSWORD);
        oracle.jdbc.datasource.impl.OracleDataSource ods = new OracleDataSource();
        ods.setURL(url);
        ods.setUser(userName);
        ods.setPassword(password);
       conn = ods.getConnection();
      //setup db tables
      Statement stmt = conn.createStatement();
      stmt.execute("drop table if exists emp_json");
      stmt.execute("create table emp_json(c1 number, c2 JSON) tablespace tbs1");
      stmt.close();

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Inserts a list of {@link Phone} objects into the database in JSON format.
   *
   * @throws IOException   if there is an error during object serialization.
   * @throws SQLException  if there is a database access error.
   */
  @Test
  @Order(1)
  public void insertIntoDatabase() throws IOException, SQLException {
    Assumptions.assumeTrue(conn != null);
    List<Phone> phones = new ArrayList<>();
    phones.add(new Phone("333-222-1111", Phone.Type.WORK));
    phones.add(new Phone("666-555-4444", Phone.Type.MOBILE));
    phones.add(new Phone("999-888-7777", Phone.Type.HOME));

    PreparedStatement
      pstmt = conn.prepareStatement("insert into emp_json (c1,c2) values(?,?)");
    pstmt.setInt(1, 1);

    pstmt.setObject(2, phones, OracleType.JSON);
    pstmt.execute();
    pstmt.close();
  }

  /**
   * Retrieves the list of {@link Phone} objects from the database and prints their details.
   *
   * @throws SQLException  if there is a database access error.
   * @throws IOException   if there is an error during object conversion.
   */
  @Test
  @Order(2)
  public void retieveFromDatabase() throws SQLException, IOException {
    Assumptions.assumeTrue(conn != null);
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("select c1, c2 from emp_json order by c1");
    while(rs.next()) {
      
      JavaType type = TypeFactory.defaultInstance().constructParametricType(List.class, Phone.class); 
      List<Phone> phones = (List<Phone>) JacksonOsonConverter.convertValue(
          rs.getObject(2, JsonNode.class), type);

        for (Phone phone : phones) {
            System.out.println(phone.toString());
        }
    }
    rs.close();
    stmt.close();

  }

}
