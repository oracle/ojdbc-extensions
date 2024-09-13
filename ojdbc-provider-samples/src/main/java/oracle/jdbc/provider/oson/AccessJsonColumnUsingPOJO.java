package oracle.jdbc.provider.oson;

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
import oracle.jdbc.provider.oson.model.*;
import oracle.sql.json.OracleJsonDatum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;

/**
 * <p>
 * A standalone example that inserts and retieves JSON data to the Oracle database
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
