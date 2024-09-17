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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.provider.Configuration;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.jdbc.provider.oson.OsonModule;
import oracle.jdbc.provider.oson.OsonGenerator;
import oracle.jdbc.provider.oson.sample.model.*;
import oracle.sql.json.OracleJsonDatum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;

 /**
 * <p>
 * A standalone example that inserts and retrieves JSON data to the Oracle database
 * using the {@link OsonFactory}, {@link OsonGenerator} and an {@link ObjectMapper}.
 * </p><p>
 * The {@link OsonFactory} and {@link OsonGenerator} are used to write the OSON representation
 * of the POJO to an OutputStream. The data is inserted by passing the bytes of the OutputStream
 * to {@link PreparedStatement#setBytes(int, byte[])} method, and retrieved by getting the data
 * as a {@link OracleJsonDatum} using the {@link ResultSet#getObject(int, Class)} method and
 * passing the bytes to an {@link ObjectMapper} to get the POJO object.
 * </p>
 */
public class AccessJsonColumnUsingPOJO {
  public static void insertIntoDatabase(Connection conn, JsonFactory osonFactory, ObjectMapper objectMapper) throws IOException, SQLException {
    Emp emp = JacksonOsonSampleUtil.createEmp();

    // An extension of ByteArrayOutputStream that synchronises the read and reset methods
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      try (JsonGenerator jsonGen = osonFactory.createGenerator(out)) {
        objectMapper.writeValue(jsonGen, emp);
      }
      try (PreparedStatement pstmt = conn.prepareStatement("insert into jackson_oson_sample (id, json_value) values(?,?)")) {
        pstmt.setInt(1, 1);
        pstmt.setBytes(2, out.toByteArray());
        pstmt.execute();
      }

    }
  }

  public static void retrieveFromDatabase(Connection conn, ObjectMapper objectMapper) throws SQLException, IOException {

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select id, json_value from jackson_oson_sample order by id")) {
      while (rs.next()) {
        byte[] osonBytes = rs.getObject(2, OracleJsonDatum.class).shareBytes();
        // POJO mapping
        Emp emp = objectMapper.readValue(osonBytes, Emp.class);
        System.out.println(emp.toString());
      }
    }
  }


  public static void main(String[] args) {
    try {
      JsonFactory osonFactory = new OsonFactory();
      ObjectMapper objectMapper = new ObjectMapper(osonFactory);
      Connection conn = JacksonOsonSampleUtil.createConnection();
      JacksonOsonSampleUtil.createTable(conn);
      insertIntoDatabase(conn, osonFactory, objectMapper);
      retrieveFromDatabase(conn, objectMapper);
    } catch (SQLException sqlException) {
      sqlException.printStackTrace();
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }
  }

}
