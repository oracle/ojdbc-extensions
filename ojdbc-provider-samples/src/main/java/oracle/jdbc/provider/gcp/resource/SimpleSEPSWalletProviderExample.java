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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * A standalone example that configures Oracle JDBC to be provided with the
 * connection properties retrieved from a GCP Secret Manager SEPS Wallet.
 * <p>
 * This example demonstrates how to use the SEPS Wallet Provider with Oracle JDBC
 * to connect to an Oracle Autonomous Database (ADB).
 * It retrieves the database credentials from a Secure External Password Store
 * (SEPS) wallet stored in GCP Secret Manager.
 * </p>
 * <p>
 * The SEPS wallet securely stores encrypted database credentials.
 * </p>
 */
public class SimpleSEPSWalletProviderExample {
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

      // TLS Configuration Provider
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration",
        "ojdbc-provider-gcp-secretmanager-tls");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.type","SSO");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.secretVersionName",
        "projects/{your-project-id}/secrets/{your-secret-name}/versions/{version-number}");

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


