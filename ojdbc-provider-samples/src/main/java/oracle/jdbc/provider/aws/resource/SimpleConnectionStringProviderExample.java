/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Example demonstrating how to configure Oracle JDBC with the AWS Secrets Manager
 * Connection String Provider to retrieve connection strings from a tnsnames.ora
 * file stored in AWS Secrets Manager.
 */
public class SimpleConnectionStringProviderExample {
  public static void main(String[] args) throws SQLException {
    try {
      OracleDataSource ds = new OracleDataSource();
      ds.setURL("jdbc:oracle:thin:@");
      ds.setUser("DB_USERNAME");
      ds.setPassword("DB_PASSWORD");

      Properties connectionProps = new Properties();

      // Connection String Provider for retrieving tnsnames.ora content
      // from AWS Secrets Manager
      connectionProps.put("oracle.jdbc.provider.connectionString",
        "ojdbc-provider-aws-secrets-manager-tnsnames");
      connectionProps.put("oracle.jdbc.provider.connectionString.secretName",
        "secret-name");
      connectionProps.put("oracle.jdbc.provider.connectionString.tnsAlias",
        "tns-alias");

      // TLS Configuration for secure connection
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration",
        "ojdbc-provider-aws-secrets-manager-tls");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.secretName",
        "secret-name");
      connectionProps.put("oracle.jdbc.provider.tlsConfiguration.type", "SSO");

      ds.setConnectionProperties(connectionProps);

      try (Connection cn = ds.getConnection()) {
        String query = "SELECT 'Hello, db' FROM sys.dual";
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
          if (rs.next()) {
            System.out.println(rs.getString(1));
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Connection failed: ", e);
    }
  }
}
