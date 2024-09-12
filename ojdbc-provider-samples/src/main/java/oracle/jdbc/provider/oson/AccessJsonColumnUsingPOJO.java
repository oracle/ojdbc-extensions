package oracle.jdbc.provider.oson;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccessJsonColumnUsingPOJO {

  private static final String URL = Configuration.getRequired("jackson_oson_url");
  private static final String USER = Configuration.getRequired("jackson_oson_username");
  private static final String PASSWORD = Configuration.getRequired("jackson_oson_password");
  Connection conn = null;
  OsonFactory osonFactory;
  ObjectMapper om;
  OsonGenerator osonGen;
  byte[] objectBytes =null;

  public AccessJsonColumnUsingPOJO() {
    osonFactory = new OsonFactory();
    om = new ObjectMapper(osonFactory);
    osonGen = null;
  }

  public static void main(String[] args) {
    try {
      AccessJsonColumnUsingPOJO accessJsonColumnUsingPOJO = new AccessJsonColumnUsingPOJO();
      accessJsonColumnUsingPOJO.setup();
      accessJsonColumnUsingPOJO.insertIntoDatabase();
      accessJsonColumnUsingPOJO.retrieveFromDatabase();
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public void insertIntoDatabase() throws IOException, SQLException {
    List<Phone> phones = new ArrayList<>();
    phones.add(new Phone("333-222-1111", Phone.Type.WORK));
    phones.add(new Phone("666-555-4444", Phone.Type.MOBILE));
    phones.add(new Phone("999-888-7777", Phone.Type.HOME));
    Emp e = new Emp("Bob", "software engineer", new BigDecimal(4000), "bob@bob.org", phones);

    // An extension of ByteArrayOutputStream that synchronises the read and reset methods
    ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream();
    osonGen = (OsonGenerator) osonFactory.createGenerator(out);
    om.writeValue(osonGen, e);
    osonGen.close();

    InputStream in = out.read();

    // insert into db
    PreparedStatement pstmt = conn.prepareStatement("insert into emp_json (c1,c2) values(?,?)");
    pstmt.setInt(1, 1);
    pstmt.setBinaryStream(2, in);
    pstmt.execute();
    pstmt.close();
  }

  public void retrieveFromDatabase() throws Exception {

    //retrieve from db
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("select c1, c2 from emp_json order by c1");
    while(rs.next()) {

      OracleJsonValue oson_value = rs.getObject(2, OracleJsonValue.class);
      byte[] osonBytes = rs.getObject(2, OracleJsonDatum.class).shareBytes();

      if (oson_value != null && oson_value instanceof OracleJsonObject) {
        OracleJsonObject oson_obj = (OracleJsonObject) oson_value;

        //OracleJsonObject value
        System.out.println("c2 = " + oson_obj);
      }
      if(osonBytes!=null && osonBytes.length>0) {
        System.out.println("Oson bytes length: " + osonBytes.length);

        // JsonNode mapping
        JsonNode node = om.readValue(osonBytes, JsonNode.class);
        System.out.println(node.toPrettyString());
      }
      if(osonBytes!=null && osonBytes.length>0) {
        System.out.println("Oson bytes length: " + osonBytes.length);

        // POJO mapping
        Emp emp = om.readValue(osonBytes, Emp.class);
        System.out.println(emp.toString());
      }
    }
    rs.close();
    stmt.close();
  }

  public void setup() {
    try {
      OracleDataSource ods = new OracleDataSource();
      ods.setURL(URL);
      ods.setUser(USER);
      ods.setPassword(PASSWORD);
      conn = ods.getConnection();

      //setup db tables
      Statement stmt = conn.createStatement();
      stmt.execute("drop table if exists emp_json");
      stmt.execute("create table emp_json(c1 number, c2 JSON) tablespace tbs1");
      stmt.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
