package oracle.jdbc.provider.oci.objectstorage;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class ObjectFactoryTest {

  /**
   * Verifies the provider can accept the OCI Object Storage URL in
   * new or old format.
   */
  @Test
  void testConnectionFromOldOrNewURL() throws SQLException {
    String location =
      TestProperties.getOrAbort(OciTestProperty.OCI_OBJECT_STORAGE_URL);
    String url = "jdbc:oracle:thin:@config-ociobject:" + location;
    helper(url);
  }

  private void helper(String url) throws SQLException {
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);

    // Standard JDBC code
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("SELECT 'Hello, db' FROM sys.dual");
    if (rs.next())
      Assertions.assertEquals("Hello, db", rs.getString(1));
    else
      Assertions.fail("Should get 'Hello, db'");
  }
}