package oracle.jdbc.provider.oci.configuration.resource;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Example demonstrating how to use the Vault Username Provider
 * with Oracle JDBC to securely retrieve a database username from OCI Vault.
 * <p>
 * This example shows how to configure Oracle JDBC to retrieve the username.
 * </p>
 */
public class SimpleVaultSecretUsernameProviderExample {
  private static final String DB_URL = "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=your_db_host))(connect_data=(service_name=your_service_name))(security=(ssl_server_dn_match=yes)))";
  private static final String JDBC_URL = "jdbc:oracle:thin:@" + DB_URL;
  

  public static void main(String[] args) throws SQLException {
    try {
      OracleDataSource ds = new OracleDataSource();
      ds.setURL(JDBC_URL);
      ds.setPassword("DB_PASSWORD");

      Properties connectionProps = new Properties();
      connectionProps.put("oracle.jdbc.provider.username","ojdbc-provider-oci-vault-username");
      connectionProps.put("oracle.jdbc.provider.username.ocid",
        "ocid1.vaultsecret.oc1.phx.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration", "ojdbc-provider-oci-database-tls");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.ocid",
        "ocid1.vaultsecret.oc1.phx.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
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
