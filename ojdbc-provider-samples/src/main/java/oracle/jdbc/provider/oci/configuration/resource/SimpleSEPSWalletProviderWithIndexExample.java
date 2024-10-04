package oracle.jdbc.provider.oci.configuration.resource;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * A standalone example that configures Oracle JDBC to be provided with a
 * specified connection string index to retrieve credentials from an OCI
 * Vault SEPS wallet.
 * <p>
 * This example demonstrates how to use the SEPS Wallet Provider with Oracle JDBC
 * to connect to an Oracle Autonomous Database (ADB) in Oracle Cloud Infrastructure (OCI).
 * It retrieves the database credentials from a Secure External Password Store
 * (SEPS) wallet stored in OCI Vault and specifying a connection string index
 * to select a specific credential set.
 * </p>
 * <p>
 * The SEPS wallet securely stores encrypted database credentials.
 * </p>
 *
 */
public class SimpleSEPSWalletProviderWithIndexExample {
  private static final String DB_URL = "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=your_db_host))(connect_data=(service_name=your_service_name))(security=(ssl_server_dn_match=yes)))";
  private static final String JDBC_URL = "jdbc:oracle:thin:@" + DB_URL;


  public static void main(String[] args) {
    try {
      OracleDataSource ds = new OracleDataSource();
      ds.setURL(JDBC_URL);

      Properties connectionProps = new Properties();
      connectionProps.put("oracle.jdbc.provider.username", "ojdbc-provider-oci-vault-seps");
      connectionProps.put("oracle.jdbc.provider.password", "ojdbc-provider-oci-vault-seps");

      // Set the OCID of your SEPS wallet stored in OCI Vault
      connectionProps.put("oracle.jdbc.provider.username.ocid",
        "ocid1.vaultsecret.oc1.phx.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
      connectionProps.put("oracle.jdbc.provider.password.ocid",
        "ocid1.vaultsecret.oc1.phx.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

      // TLS Configuration Provider
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration",
        "ojdbc-provider-oci-database-tls");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.ocid",
        "ocid1.autonomousdatabase.oc1.phx.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

      // Specify the connection string index
      connectionProps.put("oracle.jdbc.provider.username.connectionStringIndex","1");
      connectionProps.put("oracle.jdbc.provider.password.connectionStringIndex","1");

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


