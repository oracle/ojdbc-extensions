package oracle.jdbc.provider.gcp.resource;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * A standalone example that configures Oracle JDBC to be provided with a
 * specified connection string index to retrieve credentials from a GCP
 * Secret Manager SEPS wallet.
 * <p>
 * This example demonstrates how to use the SEPS Wallet Provider with Oracle JDBC
 * to connect to an Oracle Autonomous Database (ADB) using credentials stored
 * in a Secure External Password Store (SEPS) wallet managed by GCP Secret Manager.
 * It retrieves the database credentials from the SEPS wallet stored in GCP Secret Manager,
 * specifying a connection string index to select a specific credential set.
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
      connectionProps.put("oracle.jdbc.provider.username",
              "ojdbc-provider-gcp-secretmanager-seps");
      connectionProps.put("oracle.jdbc.provider.password",
              "ojdbc-provider-gcp-secretmanager-seps");

      // Set the secret version name of your SEPS wallet stored in GCP Secret Manager
      connectionProps.put("oracle.jdbc.provider.username.secretVersionName",
              "projects/{your-project-id}/secrets/{your-secret-name}/versions/{version-number}");
      connectionProps.put("oracle.jdbc.provider.password.secretVersionName",
              "projects/{your-project-id}/secrets/{your-secret-name}/versions/{version-number}");

      // Specify the connection string index
      connectionProps.put("oracle.jdbc.provider.username.connectionStringIndex","1");
      connectionProps.put("oracle.jdbc.provider.password.connectionStringIndex","1");

      connectionProps.put("oracle.jdbc.provider.tlsConfiguration",
              "ojdbc-provider-gcp-secretmanager-tls");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.secretVersionName",
              "projects/{your-project-id}/secrets/{your-secret-name}/versions/{version-number}");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.type","SSO");

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


