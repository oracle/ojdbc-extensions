package oracle.jdbc.provider.oson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.provider.Configuration;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.jdbc.provider.oson.OsonModule;
import oracle.jdbc.provider.oson.OsonGenerator;
import oracle.sql.json.OracleJsonDatum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;

/**
 * <p>
 * A standalone example that inserts and retieves JSON data to the Oracle database
 * using an {@link OsonFactory} and {@link ObjectNode}.
 * </p><p>
 * The JSON object is created using Jackson API's {@link ObjectNode}. The {@link OsonFactory}
 * and {@link OsonGenerator} are used to write the JSON Object to an OutputStream. The data
 * is inserted by passing the bytes of the OutputStream to
 * {@link PreparedStatement#setBytes(int, byte[])} method, and retrieved getting the data
 * as a {@link OracleJsonDatum} using the {@link ResultSet#getObject(int, Class)} method and
 * passing the bytes to an {@link ObjectMapper} to get the {@link ObjectNode} representation
 * of the JSON object.
 * </p>
 */
public class AccessJsonColumnUsingJacksonObjectNode {
  public static void insertIntoDatabase(Connection conn, JsonFactory osonFactory, ObjectMapper objectMapper) throws SQLException, IOException {

    objectMapper.registerModule(new OsonModule());
    ObjectNode node = objectMapper.createObjectNode();
    node.put("name", "Alexis Bull");
    node.put("age", 45);

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      try (JsonGenerator osonGen = osonFactory.createGenerator(out)) {
        objectMapper.writeValue(osonGen, node);
      }

      try (PreparedStatement pstmt = conn.prepareStatement("insert into jackson_oson_sample (id, json_value) values(?,?)")) {
        pstmt.setInt(1, 1);
        pstmt.setBytes(2, out.toByteArray());
        pstmt.execute();
      }
    }
  }

  public static void retrieveFromDatabase(Connection conn, ObjectMapper objectMapper) throws SQLException, IOException{
     
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("select * from jackson_oson_sample")){
      while(rs.next()) {
        JsonNode node = objectMapper.readValue(rs.getObject(2, OracleJsonDatum.class).shareBytes(), JsonNode.class);
        System.out.println(node.toPrettyString());
      }
    }
  }

  public static void main(String[] args) {
    try {
      System.setProperty(OracleConnection.CONNECTION_PROPERTY_JSON_DEFAULT_GET_OBJECT_TYPE, "oracle.sql.json.OracleJsonValue");
      JsonFactory osonFactory = new OsonFactory();
      ObjectMapper objectMapper = new ObjectMapper(osonFactory);
      Connection conn = JacksonOsonSampleUtil.createConnection();
      JacksonOsonSampleUtil.createTable(conn);
      insertIntoDatabase(conn, osonFactory, objectMapper);
      retrieveFromDatabase(conn, objectMapper);
      conn.close();
    } catch (SQLException exception) {
      exception.printStackTrace();
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }
  }

}
