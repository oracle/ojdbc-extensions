package oracle.jdbc.provider.oci.configuration.resource;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.*;
import java.util.Properties;

/**
 * Example demonstrating how to use the Database TLS Provider
 * with Oracle JDBC to establish a secure TLS connection to an
 * Oracle Autonomous Database.
 */
public class SimpleDatabaseTlsProviderExample {
  private static final String DB_URL = "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=your_db_host))(connect_data=(service_name=your_service_name))(security=(ssl_server_dn_match=yes)))";
  private static final String JDBC_URL = "jdbc:oracle:thin:@" + DB_URL;
  private static final String USERNAME = "DB_USERNAME";
  private static final String PASSWORD = "DB_PASSWORD";


  public static void main(String[] args) {
    try {
      OracleDataSource ds = new OracleDataSource();
      ds.setURL(JDBC_URL);
      ds.setUser(USERNAME);
      ds.setPassword(PASSWORD);

      Properties connectionProps = new Properties();
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration",
        "ojdbc-provider-oci-database-tls");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.ocid",
        "ocid1.autonomousdatabase.oc1.phx.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");


      ds.setConnectionProperties(connectionProps);
      try (Connection cn = ds.getConnection()) {
        String connectionString = cn.getMetaData().getURL();
        System.out.println("Connected to: " + connectionString);

        Statement st = cn.createStatement();
        ResultSet rs = st.executeQuery("SELECT 'Hello, db' FROM sys.dual");
        if (rs.next())
          System.out.println(rs.getString(1));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
