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

package oracle.jdbc.provider.hashicorp.dedicated.configuration;

import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.Configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A standalone example demonstrating how to connect to an Oracle database using
 * connection properties retrieved from a dedicated HashiCorp Vault.
 */
public class SimpleVaultDedicatedJsonExample {

  /**
   * The configuration parameter for the Vault secret path.
   * <p>
   * This parameter should be set as a JVM system property, environment variable,
   * or an entry in the configuration.properties file under the key
   * "DEDICATED_VAULT_SECRET_PATH".
   * </p>
   */
  private static final String VAULT_SECRET_PATH = Configuration.getRequired(
          "DEDICATED_VAULT_SECRET_PATH");

  /**
   * <p>
   * Connects to a database using connection properties retrieved from the
   * configured Vault secret path.
   * </p>
   *
   * <p>
   * Ensure that the dedicated provider is properly configured and the secret
   * path is accessible for authentication and configuration retrieval.
   * </p>
   *
   * @param args the command line arguments
   * @throws SQLException if an error occurs during the database calls
   */
  public static void main(String[] args) throws SQLException {

    // Construct a JDBC URL for the dedicated type provider
    String url = "jdbc:oracle:thin:@config-hcpdedicatedvault://" + VAULT_SECRET_PATH;

    // Sample default URL if not provided in arguments
    if (args.length > 0) {
      url = args[0];
    }

    // Configure the data source
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);
    ds.setConnectionProperty("oracle.net.wallet_location","/Users/elmajdoubymouhsin/Downloads/wallet_2");


    // Standard JDBC code
    try (Connection cn = ds.getConnection()) {
      System.out.println("Connected to: " + cn.getMetaData().getURL());

      Statement st = cn.createStatement();
      ResultSet rs = st.executeQuery("SELECT 'Hello, Dedicated Vault' FROM sys.dual");
      if (rs.next()) {
        System.out.println(rs.getString(1));
      }
    }
  }
}
