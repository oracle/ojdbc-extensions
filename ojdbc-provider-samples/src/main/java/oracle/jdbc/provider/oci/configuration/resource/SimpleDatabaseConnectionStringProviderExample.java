package oracle.jdbc.provider.oci.configuration.resource;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Example demonstrating how to configure Oracle JDBC with the Database
 * Connection String Provider to retrieve connection strings and TLS
 * configuration for a secure connection to an Oracle Autonomous Database
 * <p>
 * The connection string and TLS credentials are securely retrieved based on the
 * Autonomous Database OCID.
 * </p>
 */
public class SimpleDatabaseConnectionStringProviderExample {
  private static final String JDBC_URL_PREFIX = "jdbc:oracle:thin:@";
  private static final String DB_PASSWORD = "password";
  private static final String DB_USER = "username";


  public static void main(String[] args) throws SQLException {
    try {
      OracleDataSource ds = new OracleDataSource();
      ds.setURL(JDBC_URL_PREFIX);
      ds.setUser(DB_USER);
      ds.setPassword(DB_PASSWORD);

      Properties connectionProps = new Properties();
      connectionProps.put("oracle.jdbc.provider.connectionString",
        "ojdbc-provider-oci-database-connection-string");
      connectionProps.put("oracle.jdbc.provider.connectionString.ocid",
        "ocid1.autonomousdatabase.oc1.phx.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

      ds.setConnectionProperties(connectionProps);

      try (Connection cn = ds.getConnection()) {
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
