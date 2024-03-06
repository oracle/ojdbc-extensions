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

import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.Configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A standalone example that configures Oracle JDBC to be provided with the
 * connection properties retrieved from OCI Object Storage.
 */
public class ObjectStorageExample {
  /**
   * An OCI Object Storage URI configured as a JVM system property,
   * environment variable, or configuration.properties file entry named
   * "object_uri".
   */
  private static final String OBJECT_URI = Configuration
    .getRequired("object_uri");

  /**
   * An OCI Authentication method configured as an JVM system property,
   * environment variable, or configuration.properties file entry named
   * "oci_authentication". {@code null} if not present.
   */
  private static final String OCI_AUTHENTICATION = Configuration
    .getOptional("oci_authentication");

  /**
   * <p>
   * Connects to a database using connection properties retrieved from OCI
   * Object Storage.
   * </p><p>
   * By default, the provider authenticates with OCI using the API key of the
   * DEFAULT profile in $HOME/.oci/config. To authenticate using different
   * authentication methods, set the "oci_authentication" property either in JVM
   * system property, environment variable, or configuration.properties file
   * entry.
   * </p>
   * @param args the command line arguments
   * @throws SQLException if an error occurs during the database calls
   */
  public static void main(String[] args) throws SQLException {
    String url = "jdbc:oracle:thin:@config-ociobject:" + OBJECT_URI;

    if (OCI_AUTHENTICATION == null)
      System.out.println("oci_authentication  property is empty. Default " +
        "authentication is used.");
    else {
      switch (OCI_AUTHENTICATION) {
      case "OCI_DEFAULT":
        url += "?" + useDefault();
        break;
      case "OCI_INSTANCE_PRINCIPAL":
        url += "?" + useInstancePrincipal();
        break;
      case "OCI_RESOURCE_PRINCIPAL":
        url += "?" + useResourcePrincipal();
        break;
      case "OCI_INTERACTIVE":
        url += "?" + useInteractive();
        break;
      default:
        throw new IllegalArgumentException("Unknown oci_authentication: " +
          OCI_AUTHENTICATION);
      }
    }

    // Standard JDBC code
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);

    System.out.println("Connection URL: " + url);

    try (Connection cn = ds.getConnection()) {
      String connectionString = cn.getMetaData().getURL();
      System.out.println("Connected to: " + connectionString);

      Statement st = cn.createStatement();
      ResultSet rs = st.executeQuery("SELECT 'Hello, db' FROM sys.dual");
      if (rs.next())
        System.out.println(rs.getString(1));
    }
  }

  private static String useDefault() {
    final String tenantId = Configuration.getOptional("oci_tenancy");
    final String userId = Configuration.getOptional("oci_user");
    final String fingerprint = Configuration.getOptional("oci_fingerprint");
    final String privateKey = Configuration.getOptional("oci_key_file");
    final String passPhrase = Configuration.getOptional("oci_pass_phrase");

    if (Stream.of(tenantId, userId, fingerprint, privateKey, passPhrase)
      .allMatch(Objects::isNull)) {
      return "AUTHENTICATION=OCI_DEFAULT";
    }

    return "AUTHENTICATION=OCI_DEFAULT" +
      "&OCI_TENANCY=" + tenantId +
      "&OCI_USER=" + userId +
      "&OCI_FINGERPRINT=" + fingerprint +
      "&OCI_KEY_FILE=" + privateKey +
      "&OCI_PASS_PHRASE=" + passPhrase;
  }

  private static String useInstancePrincipal() {
    return "AUTHENTICATION=OCI_INSTANCE_PRINCIPAL";
  }

  private static String useResourcePrincipal() {
    return "AUTHENTICATION=OCI_RESOURCE_PRINCIPAL";
  }

  private static String useInteractive() {
    // NOTE:
    //   Interactive authentication will not work if JVM is in "headless" mode.
    return "AUTHENTICATION=OCI_INTERACTIVE";
  }
}
