/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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
package oracle.jdbc.provider.oci.configuration;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A standalone example that configures Oracle JDBC to be provided with the
 * connection properties retrieved from OCI Vault Secret.
 */
public class SimpleVaultJsonExampleWithUCP {

  private static String url;

  /**
   * <p>
   * Simple example to retrieve connection properties from OCI Vault Secret.
   * </p>
   * <p>
   * For the default authentication, the only required local configuration is
   * to have a valid OCI Config in ~/.oci/config.
   * </p>
   * <p>
   * To run this example, the payload needs to be stored in OCI Vault Secret.
   * The payload examples can be found in
   * {@link oracle.jdbc.spi.OracleConfigurationProvider}.
   * </p>
   * Users need to indicate the OCID of the Secret with the following syntax:
   * <pre>
   * jdbc:oracle:thin:@config-ocivault:{secret-ocid}
   * </pre>
   * @param args the command line arguments
   * @throws SQLException if an error occurs during the database calls
   **/
  public static void main(String[] args) throws SQLException {

    // Sample default URL if non present
    if (args.length == 0) {
      //url = "jdbc:oracle:thin:@config-ocivault:ocid1.vaultsecret.oc1.phx.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
      url = "jdbc:oracle:thin:@config-ocivault:ocid1.vaultsecret.oc1.phx.amaaaaaadxiv6saarznfck3kt7xypco47xb2d4uv6g7p3wfmc27v6vbuzzuq";
    } else {
      url = args[0];
    }

    // No changes required, configuration provider is loaded at runtime
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setURL(url);

    // Standard JDBC code
    try (Connection cn = pds.getConnection()) {
      Statement st = cn.createStatement();
      ResultSet rs = st.executeQuery("SELECT 'Hello, db' FROM sys.dual");
      if (rs.next())
        System.out.println(rs.getString(1));
    }
  }
}
