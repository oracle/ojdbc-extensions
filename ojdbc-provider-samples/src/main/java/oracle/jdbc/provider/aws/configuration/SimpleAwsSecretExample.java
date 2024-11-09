package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SimpleAwsSecretExample {
  private static String url;
  public static void main(String[] args) throws SQLException {

    // Sample default URL if non present
    if (args.length == 0) {
      url = "jdbc:oracle:thin:@config-awssecret://payload_ojdbc_base64_adb";
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
