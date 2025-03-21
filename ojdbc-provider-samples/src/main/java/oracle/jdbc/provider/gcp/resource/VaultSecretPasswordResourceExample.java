/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */
package oracle.jdbc.provider.gcp.resource;

import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.Configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * A standalone example that configures Oracle JDBC to be provided with the
 * connection properties retrieved from OCI Object Storage.
 */
public class VaultSecretPasswordResourceExample {
  /**
   * An GCP SecretManager resource name configured as a JVM system property,
   * environment variable, or configuration.properties file entry named
   * "gcp_secret_version_name".
   */
  private static final String RESOURCE_NAME = Configuration
      .getRequired("gcp_secret_version_name");

  private static final String DB_URL = "(description=(retry_count=2)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.us-phoenix-1.oraclecloud.com))(connect_data=(service_name=gebqqvpozhjbqbs_awyonurbg0gp0smq_tp.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))";

  /**
   * <p>
   * Connects to a database using connection properties retrieved from GCP
   * Object Storage.
   * </p>
   * <p>
   * Providers use Google Cloud APIs which support Application Default
   * Credentials; the libraries look for credentials in a set of defined
   * locations and use those credentials to authenticate requests to the API.
   * </p>
   * 
   * @see <a href=
   *      "https://cloud.google.com/docs/authentication/application-default-credentials">
   *      Application Default Credentials</a>
   * 
   * @param args the command line arguments
   * @throws SQLException if an error occurs during the database calls
   */
  public static void main(String[] args) throws SQLException {
    String url = "jdbc:oracle:thin:@" + DB_URL;

    // Standard JDBC code
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);
    ds.setUser("DB_USER");
    Properties properties = new Properties();
    properties.put("oracle.jdbc.provider.password", "ojdbc-provider-gcp-secretmanager-password");
    properties.put("oracle.jdbc.provider.password.secretVersionName", RESOURCE_NAME);
    ds.setConnectionProperties(properties);

    try (Connection cn = ds.getConnection()) {
      String connectionString = cn.getMetaData().getURL();
      System.out.println("Connected to: " + connectionString);

      Statement st = cn.createStatement();
      ResultSet rs = st.executeQuery("SELECT 'Hello, db' FROM sys.dual");
      if (rs.next())
        System.out.println(rs.getString(1));
    }
  }

}
