package oracle.jdbc.provider.oci.configuration.resource;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.*;
import java.util.Properties;

/**
 * Example demonstrating how to configure Oracle JDBC with the TCPS Wallet
 * Provider to establish a secure TLS connection to an Oracle Autonomous
 * Database in OCI.
 * <p>
 * The wallet is retrieved from OCI Vault to enable secure TLS communication.
 * </p>
 */
public class SimpleTCPSWalletProviderExample {
  private static final String DB_URL = "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=your_db_host))(connect_data=(service_name=your_service_name))(security=(ssl_server_dn_match=yes)))";
  private static final String JDBC_URL = "jdbc:oracle:thin:@" + DB_URL;
  private static final String USERNAME = "DB_USER";
  private static final String PASSWORD = "DB_PASSWORD";


  public static void main(String[] args) throws SQLException {
    try {
      OracleDataSource ds = new OracleDataSource();
      ds.setURL(JDBC_URL);
      ds.setUser(USERNAME);
      ds.setPassword(PASSWORD);

      Properties connectionProps = new Properties();
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration", "ojdbc-provider-oci-vault-tls");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.ocid",
        "ocid1.vaultsecret.oc1.phx.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.type", "SSO");
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