package oracle.jdbc.provider.oson;

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

import java.io.InputStream;
import java.sql.*;

public class AccessJsonColumnUsingJacksonObjectNode {

  private static final String URL = Configuration.getRequired("jackson_oson_url");
  private static final String USER = Configuration.getRequired("jackson_oson_username");
  private static final String PASSWORD = Configuration.getRequired("jackson_oson_password");

  private Connection conn = null;
  private OsonFactory osonFactory = new OsonFactory();
  private ObjectMapper om = new ObjectMapper(osonFactory);
  private ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream();
  
  public static void main(String[] args) {
    AccessJsonColumnUsingJacksonObjectNode accessJsonColumnUsingJacksonObjectNode = new AccessJsonColumnUsingJacksonObjectNode();
    accessJsonColumnUsingJacksonObjectNode.setup();
    accessJsonColumnUsingJacksonObjectNode.insertIntoDatabase();
    accessJsonColumnUsingJacksonObjectNode.retrieveFromDatabase();
  }

  public void insertIntoDatabase() {
    try{
      om.registerModule(new OsonModule());
        
      ObjectNode node = om.createObjectNode();
      node.put("name", "Alexis Bull");
      node.put("age", 45);
        
      OsonGenerator osonGen = (OsonGenerator)osonFactory.createGenerator(out);
      om.writeValue(osonGen, node);
      osonGen.close();
                
      InputStream in = out.read();
      PreparedStatement pstmt = conn.prepareStatement("insert into JsonTest (id,jsontext) values(?,?)");
      pstmt.setInt(1, 1);
      pstmt.setBinaryStream(2, in);
      pstmt.execute();
      pstmt.close();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void retrieveFromDatabase() {
     
    try{
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("select * from JsonTest");
        
      while(rs.next()) {
        JsonNode node = om.readValue(rs.getObject(2, OracleJsonDatum.class).shareBytes(), JsonNode.class);
        System.out.println(node.toPrettyString());
      }
      rs.close();
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void setup() {
    try{
        OracleDataSource ods = new OracleDataSource();
        ods.setURL(URL);
        ods.setUser(USER);
        ods.setPassword(PASSWORD);
        ods.setConnectionProperty(OracleConnection.CONNECTION_PROPERTY_JSON_DEFAULT_GET_OBJECT_TYPE, "oracle.sql.json.OracleJsonValue");
         
        conn = ods.getConnection();
        createTable(conn);
    }
    catch(Exception e) {
        e.printStackTrace();
    }
  }

  private void createTable(Connection conn) throws SQLException {
    Statement stmt = null;
    try {
      stmt = conn.createStatement();
      stmt.execute("drop table if exists JsonTest");
      stmt.execute("create table JsonTest ("
              + "id number,"
              + "jsontext json"
              + ")");
    }
    finally {
      if(stmt != null)
        stmt.close();
    }
}
}
