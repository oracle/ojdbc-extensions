package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.oci.configuration.ObjectStorageExample;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A standalone example that configures Oracle JDBC to be provided with the
 * connection properties retrieved from AWS Secrets Manager.
 */
public class AwsSecretsManagerConfigurationExample {
  private static String url;

  /**
   * <p>
   * A simple example to retrieve connection properties from AWS Secrets Manager.
   * </p><p>
   * For the default authentication, the only required local configuration is
   * to have a valid AWS Config in ~/.aws/config and ~/.aws/credentials.
   * </p>
   * @param args the command line arguments
   * @throws SQLException if an error occurs during the database calls
   */
  public static void main(String[] args) throws SQLException {

    // Sample default URL if non present
    if (args.length == 0) {
      url = "jdbc:oracle:thin:@config-awssecretsmanager://{secret-name}";
    } else {
      url = args[0];
    }

    // No changes required, configuration provider is loaded at runtime
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);

    // Standard JDBC code
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("SELECT 'Hello, db' FROM sys.dual");
    if (rs.next())
      System.out.println(rs.getString(1));
  }
}
