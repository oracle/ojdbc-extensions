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
import java.util.LinkedHashSet;
import java.util.Set;

import oracle.jdbc.provider.Configuration;
import oracle.jdbc.pool.OracleDataSource;

/**
 * A standalone example that configures Oracle JDBC to be provided with the
 * connection properties retrieved from Azure App Configuration.
 */
public class AzureAppConfigExample {
  /**
   * An AZURE App Configuration name as a JVM system property,
   * environment variable, or configuration.properties file entry named
   * "app_configuration_name".
   */
  private static final String APPCONFIG_NAME = Configuration.getRequired("app_configuration_name");

  /**
   * An AZURE App Configuration prefix configured as a JVM system property,
   * environment variable, or configuration.properties file entry named
   * "app_configuration_prefix".
   */
  private static final String PREFIX = Configuration.getOptional("app_configuration_prefix");

  /**
   * An AZURE App Configuration label configured as a JVM system property,
   * environment variable, or configuration.properties file entry named
   * "app_configuration_label".
   */
  private static final String LABEL = Configuration.getOptional("app_configuration_label");

  /**
   * An Azure Authentication method configured as an JVM system property,
   * environment variable, or configuration.properties file entry named
   * "oci_authentication". {@code null} if not present.
   */
  private static String AZURE_AUTHENTICATION =
    Configuration.getOptional("azure_authentication");

  /**
   * <p>
   * Connects to a database using connection properties retrieved from Azure
   * App Configuration.
   * </p><p>
   * By default, the provider authenticates with Azure using
   * {@code DefaultAzureCredential} class. It tries to create a valid credential
   * with a certain flow, which can be found here:
   * https://learn.microsoft.com/en-us/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable
   * </p><p>
   * To authenticate using different authentication methods, set the
   * "azure_authentication" property either in JVM system property, environment
   * variable, or configuration.properties file entry.
   * </p>
   * @param args the command line arguments
   * @throws SQLException if an error occurs during the database calls
   */
  public static void main(String[] args) throws SQLException {
    // Construct a jdbc: URL
    String url = "jdbc:oracle:thin:@config-azure:" + APPCONFIG_NAME;

    String optionalString = constructOptionalString();
    if (optionalString.length() != 0)
      url += "?" + optionalString;

    // Standard JDBC code
    OracleDataSource ds = new OracleDataSource();
    ds.setURL(url);

    System.out.println("Connection URL: " + url);

    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("SELECT 'Hello, db' FROM sys.dual");
    if (rs.next())
      System.out.println(rs.getString(1));
  }

  private static String constructOptionalString() {
    Set<String> parameters = new LinkedHashSet<>();

    if (PREFIX != null)
      parameters.add("key=" + PREFIX);
    if (LABEL != null)
      parameters.add("label=" + LABEL);

    if (AZURE_AUTHENTICATION == null)
      System.out.println("azure_authentication property is empty. Default " +
        "authentication is used.");
    else {
      switch (AZURE_AUTHENTICATION) {
      case "AZURE_DEFAULT":
        parameters.add(useDefault());
        break;
      case "AZURE_SERVICE_PRINCIPAL":
        parameters.add(useServicePrincipal());
        break;
      case "AZURE_MANAGED_IDENTITY":
        parameters.add(useManagedIdentity());
        break;
      case "AZURE_INTERACTIVE":
        parameters.add(useInteractive());
        break;
      default:
        throw new IllegalArgumentException("Unknown azure_authentication: " +
          AZURE_AUTHENTICATION);
      }
    }

    return String.join("&", parameters);
  }

  private static String useDefault() {
    return "AUTHENTICATION=AZURE_DEFAULT";
  }

  private static String useServicePrincipal() {
    String client_id = Configuration.getRequired("azure_client_id");
    String tenant_id = Configuration.getRequired("azure_tenant_id");

    String client_secret =
      Configuration.getOptional("azure_client_secret");
    String client_certificate_path =
      Configuration.getOptional("azure_client_certificate_path");

    if (client_secret != null) {
      return useServicePrincipalSecret(client_id, client_secret, tenant_id);
    }

    if (client_certificate_path != null) {
      return useServicePrincipalCertificate(
        client_id, client_certificate_path, tenant_id);
    }

    throw new IllegalStateException("azure_client_secret and " +
      "azure_client_certificate_path cannot be both empty at the same time");
  }

  private static String useServicePrincipalSecret(
    String client_id, String client_secret, String tenant_id) {
    return "AUTHENTICATION=AZURE_SERVICE_PRINCIPAL" +
      "&AZURE_CLIENT_ID=" + client_id +
      "&AZURE_CLIENT_SECRET=" + client_secret +
      "&AZURE_TENANT_ID=" + tenant_id;
  }

  private static String useServicePrincipalCertificate(
    String client_id, String client_certificate_path, String tenant_id) {

    String authString = "AUTHENTICATION=AZURE_SERVICE_PRINCIPAL" +
      "&AZURE_CLIENT_ID=" + client_id +
      "&AZURE_CLIENT_CERTIFICATE_PATH=" + client_certificate_path +
      "&AZURE_TENANT_ID=" + tenant_id;

    String client_certificate_password =
      Configuration.getOptional("client_certificate_password");
    if (client_certificate_password != null)
      authString += "&AZURE_CLIENT_CERTIFICATE_PASSWORD=" + client_certificate_password;

    return authString;
  }

  private static String useManagedIdentity() {
    String authString = "AUTHENTICATION=AZURE_MANAGED_IDENTITY";

    String client_id = Configuration.getOptional("azure_client_id");
    if (client_id != null)
      authString += "&AZURE_CLIENT_ID=" + client_id;

    return authString;
  }

  private static String useInteractive() {
    String client_id = Configuration.getRequired("azure_client_id");
    String redirect_url = Configuration.getRequired("azure_redirect_url");

    return "AUTHENTICATION=AZURE_INTERACTIVE" +
      "&AZURE_CLIENT_ID=" + client_id +
      "&AZURE_REDIRECT_URL=" + redirect_url;
  }
}

