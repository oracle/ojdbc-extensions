package oracle.jdbc.provider.oson;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.provider.Configuration;
import oracle.jdbc.provider.oson.model.Emp;
import oracle.jdbc.provider.oson.model.Phone;

public class JacksonOsonSampleUtil {

  public static final String URL = Configuration.getRequired("jackson_oson_url");
  public static final String USER = Configuration.getRequired("jackson_oson_username");
  public static final String PASSWORD = Configuration.getRequired("jackson_oson_password");

  public static Connection createConnection() throws SQLException {
    OracleDataSource ods = new OracleDataSource();
    ods.setURL(URL);
    ods.setUser(USER);
    ods.setPassword(PASSWORD);
    return ods.getConnection();
  }

  public static void createTable(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.addBatch("drop table if exists jackson_oson_sample");
      stmt.addBatch("create table jackson_oson_sample(id number, json_value JSON) tablespace tbs1");
      stmt.executeBatch();
    }
  }

  public static Emp createEmp() {
    List<Phone> phones = new ArrayList<Phone>();
    phones.add(new Phone("333-222-1111", Phone.Type.WORK));
    phones.add(new Phone("666-555-4444", Phone.Type.MOBILE));
    phones.add(new Phone("999-888-7777", Phone.Type.HOME));
    return new Emp("Bob", "software engineer", new BigDecimal(4000), "bob@bob.org", phones);
  }

}
