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
package oracle.jdbc.provider.oson.sample;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleType;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.oson.sample.model.*;
import oracle.jdbc.provider.oson.JacksonOsonProvider;
import oracle.jdbc.provider.Configuration;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A standalone example that inserts and retrieves JSON data to the Oracle database
 * using an {@link OsonFactory} and {@link ObjectNode}.
 * </p><p>
 * The JSON object is created using Jackson API's {@link ObjectNode}. The {@link OsonFactory}
 * and {@link OsonGenerator} are used to write the JSON Object to an OutputStream. The data
 * is inserted by passing the bytes of the OutputStream to
 * {@link PreparedStatement#setBytes(int, byte[])} method, and retrieved getting the data
 * as an {@link OracleJsonDatum} using the {@link ResultSet#getObject(int, Class)} method and
 * passing the bytes to an {@link ObjectMapper} to get the {@link ObjectNode} representation
 * of the JSON object.
 * </p>
 */
public class AccessJsonColumnUsingPOJOAndJsonProvider {
  public static void insertIntoDatabase(Connection conn) throws SQLException {
    Emp emp = JacksonOsonSampleUtil.createEmp();

    try (PreparedStatement pstmt = conn.prepareStatement("insert into jackson_oson_sample (id,json_value) values(?,?)")) {
      pstmt.setInt(1, 1);
      pstmt.setObject(2, emp, OracleType.JSON);
      pstmt.execute();
    }
  }

  public static void retieveFromDatabase(Connection conn) throws SQLException {

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select id, json_value from jackson_oson_sample order by id")) {
      while (rs.next()) {
        // retrieve the object as POJO
        Emp emp = rs.getObject(2, Emp.class);
        System.out.println(emp.toString());
      }
    }

  }

  public static void main(String[] args) {
    try {
      // set the system property to the class implementing the "oracle.jdbc.spi.JsonProvider interface"
      System.setProperty(OracleConnection.CONNECTION_PROPERTY_PROVIDER_JSON, JacksonOsonProvider.PROVIDER_NAME);

      Connection conn = JacksonOsonSampleUtil.createConnection();
      JacksonOsonSampleUtil.createTable(conn);
      insertIntoDatabase(conn);
      retieveFromDatabase(conn);
      JacksonOsonSampleUtil.dropTable(conn);
      conn.close();
    } catch (SQLException sqlException) {
      sqlException.printStackTrace();
    }
  }

}
