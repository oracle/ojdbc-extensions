package oracle.jdbc.provider.oson;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleType;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.oson.model.*;
import oracle.jdbc.provider.oson.JacksonOsonProvider;
import oracle.jdbc.provider.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccessJsonColumnUsingPOJOAndJsonProvider {

  private static final String URL = Configuration.getRequired("jackson_oson_url");
  private static final String USER = Configuration.getRequired("jackson_oson_username");
  private static final String PASSWORD = Configuration.getRequired("jackson_oson_password");

  Connection conn = null;

  public static void main(String[] args) {
    try {
      AccessJsonColumnUsingPOJOAndJsonProvider accessJsonColumnUsingPOJOAndJsonProvider = new AccessJsonColumnUsingPOJOAndJsonProvider();
      accessJsonColumnUsingPOJOAndJsonProvider.setup();
      accessJsonColumnUsingPOJOAndJsonProvider.insertIntoDatabase();
      accessJsonColumnUsingPOJOAndJsonProvider.retieveFromDatabase();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void insertIntoDatabase() throws IOException, SQLException {
    List<Phone> phones = new ArrayList<>();
    phones.add(new Phone("333-222-1111", Phone.Type.WORK));
    phones.add(new Phone("666-555-4444", Phone.Type.MOBILE));
    phones.add(new Phone("999-888-7777", Phone.Type.HOME));
    Emp e = new Emp("Bob", "software engineer", new BigDecimal(4000), "bob@bob.org", phones);

    // insert into db
    PreparedStatement pstmt = conn.prepareStatement("insert into emp_json (c1,c2) values(?,?)");
    pstmt.setInt(1, 1);
    // set the column type as JSON and pass in the POJO.
    pstmt.setObject(2, e, OracleType.JSON);
    pstmt.execute();
    pstmt.close();
  }

  public void retieveFromDatabase() throws Exception {

    //retrieve from db
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("select c1, c2 from emp_json order by c1");
    while(rs.next()) {
      try{
        // retrieve the object as POJO
        Emp e = rs.getObject(2, Emp.class);
        System.out.println(e.toString());
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
    rs.close();
    stmt.close();

  }

  public void setup() {
    try {
      // set the system property to the class implementing the "oracle.jdbc.spi.JsonProvider interface"
      System.setProperty(OracleConnection.CONNECTION_PROPERTY_PROVIDER_JSON, JacksonOsonProvider.PROVIDER_NAME);
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

    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }
}
