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
package oracle.jdbc.provider.azure.configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import oracle.jdbc.datasource.impl.OracleDataSource;


/**
 * <p>
 * Simple example to retrieve connection properties from Azure App Config.
 * For the default authentication, the following environment variables must be
 * set:
 * </p>
 * <ul>
 * <li>AZURE_TENANT_ID The Azure Active Directory tenant(directory) ID.</li>
 * <li>AZURE_CLIENT_ID The client(application) ID of an App Registration in the
 * tenant.</li>
 * <li>AZURE_CLIENT_SECRET A client secret that was generated for the App
 * Registration.</li>
 * </ul>
 * <p>The Oracle DataSource uses a new prefix jdbc:oracle:thin:@config-azure://
 * to be able to identify that the configuration parameters should be loaded
 * using Azure App Configuration. Users only need to indicate the App Config's
 * name, a prefix for the key-names and the Label (both optionally) with the
 * following syntax:
 * </p>
 * <pre>
 * jdbc:oracle:thin:@config-azure://{appconfig-name}[?key=prefix&amp;label=value
 * &amp;option1=value1&amp;option2=value2...]
 * </pre>
 */
public class SimpleAzureAppConfigExample {
  private static String url;

  /**
   * @param args the command line arguments
   * @throws SQLException if an error occurs during the database calls
   */
  public static void main(String[] args) throws SQLException {
    // Sample default URL if non present
    if (args.length == 0) {
      url = "jdbc:oracle:thin:@config-azure://myappconfig?key=/sales_app1/&label=dev";
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
