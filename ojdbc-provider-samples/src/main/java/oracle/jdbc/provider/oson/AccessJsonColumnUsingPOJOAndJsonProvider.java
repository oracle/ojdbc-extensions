package oracle.jdbc.provider.oson;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleType;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.oson.model.*;
import oracle.jdbc.provider.oson.JacksonOsonProvider;
import oracle.jdbc.provider.Configuration;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A standalone example that inserts and retieves JSON data to the Oracle database
 * using the JacksonOsonProvider.
 * </p><p>
 * The data is inserted by passing a POJO to the {@link PreparedStatement#setObject(int, Object, SQLType)}
 * method, and retrieved by passing the POJO's class to {@link ResultSet#getObject(int, Class)} method. The
 * conversion from OSON to POJO done by the provider.
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
    } catch (SQLException sqlException) {
      sqlException.printStackTrace();
    }
  }

}
