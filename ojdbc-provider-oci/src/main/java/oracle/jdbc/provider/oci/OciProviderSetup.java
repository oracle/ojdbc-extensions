/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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
 ** The above copyright notice and either this complete permission notice or
 ** at a minimum a reference to the UPL must be included in all copies or
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

package oracle.jdbc.provider.oci;

import oracle.jdbc.provider.util.cli.ProviderSetupCli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive setup helper for users running this provider jar directly.
 */
public final class OciProviderSetup extends ProviderSetupCli {

  private static final String README_URL =
    "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-oci/README.md";
  private static final String TOKEN_SCOPE_PREFIX = "urn:oracle:db::id::";

  private OciProviderSetup(Scanner scanner) {
    super(scanner);
  }

  /**
   * Starts the interactive setup helper.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    new OciProviderSetup(new Scanner(System.in)).run();
  }

  @Override
  protected String displayName() {
    return "Oracle JDBC Providers for OCI";
  }

  @Override
  protected String readmeUrl() {
    return README_URL;
  }

  @Override
  protected void setupResourceProvider() {
    switch (promptMenu("Choose a resource provider:",
      "Access Token",
      "Database Connection String",
      "Database TLS",
      "Vault Username",
      "Vault Password",
      "Vault Connection String",
      "TCPS Wallet",
      "SEPS Wallet",
      "Back")) {
      case 1:
        setupTokenProvider();
        break;
      case 2:
        setupDatabaseConnectionStringProvider();
        break;
      case 3:
        setupDatabaseTlsProvider();
        break;
      case 4:
        setupVaultUsernameProvider();
        break;
      case 5:
        setupVaultPasswordProvider();
        break;
      case 6:
        setupVaultConnectionStringProvider();
        break;
      case 7:
        setupVaultTlsProvider();
        break;
      case 8:
        setupVaultSepsProvider();
        break;
      case 9:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupTokenProvider() {
    System.out.println();
    System.out.println("Setting up the OCI Token Provider");

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(
      "oracle.jdbc.provider.accessToken",
      "ojdbc-provider-oci-token");

    addResourceAuthentication(properties, "oracle.jdbc.provider.accessToken");
    properties.put("oracle.jdbc.provider.accessToken.scope", readScope());

    addResourceProperties(properties,
      "Access Token Provider from OCI Dataplane",
      "#access-token-provider");
  }

  private void setupDatabaseConnectionStringProvider() {
    String prefix = "oracle.jdbc.provider.connectionString";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-oci-database-connection-string");
    properties.put(prefix + ".ocid",
      readRequired("Autonomous Database OCID [required]: "));
    addIfPresent(properties, prefix + ".consumerGroup",
      readOptional("Consumer group [default: MEDIUM]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Database Connection String Provider from OCI Database Service",
      "#database-connection-string-provider");
  }

  private void setupDatabaseTlsProvider() {
    String prefix = "oracle.jdbc.provider.tlsConfiguration";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-oci-database-tls");
    properties.put(prefix + ".ocid",
      readRequired("Autonomous Database OCID [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Database TLS Provider from OCI Database Service",
      "#database-tls-provider");
  }

  private void setupVaultUsernameProvider() {
    String prefix = "oracle.jdbc.provider.username";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-oci-vault-username");
    properties.put(prefix + ".ocid",
      readRequired("Vault secret OCID for username [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Vault Username Provider from OCI Vault",
      "#vault-username-provider");
  }

  private void setupVaultPasswordProvider() {
    String prefix = "oracle.jdbc.provider.password";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-oci-vault-password");
    properties.put(prefix + ".ocid",
      readRequired("Vault secret OCID for password [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Vault Password Provider from OCI Vault",
      "#vault-password-provider");
  }

  private void setupVaultConnectionStringProvider() {
    String prefix = "oracle.jdbc.provider.connectionString";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-oci-vault-tnsnames");
    properties.put(prefix + ".ocid",
      readRequired("Vault secret OCID for tnsnames.ora [required]: "));
    properties.put(prefix + ".tnsAlias", readRequired("TNS alias [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Vault Connection String Provider from OCI Vault",
      "#vault-connection-string-provider");
  }

  private void setupVaultTlsProvider() {
    String prefix = "oracle.jdbc.provider.tlsConfiguration";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-oci-vault-tls");
    properties.put(prefix + ".ocid",
      readRequired("Vault secret OCID for TLS wallet/file [required]: "));
    properties.put(prefix + ".type",
      readRequired("File type (SSO, PKCS12, or PEM) [required]: "));
    addIfPresent(properties, prefix + ".walletPassword",
      readOptional(
        "Wallet password expression [optional, no default, for example ${TLS_FILE_PASSWORD}]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "TCPS Wallet Provider from OCI Vault",
      "#tcps-wallet-provider");
  }

  private void setupVaultSepsProvider() {
    int useCase = promptMenu("Choose what to configure from the SEPS wallet:",
      "Username and password",
      "Username only",
      "Password only");
    String ocid = readRequired("Vault secret OCID for SEPS wallet [required]: ");
    String walletPassword = readOptional(
      "Wallet password expression [optional, no default, for example ${SEPS_WALLET_PASSWORD}]: ");
    String connectionStringIndex =
      readOptional("Connection string index [optional, no default]: ");
    LinkedHashMap<String, String> authentication =
      readResourceAuthenticationParameters();

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    if (useCase == 1 || useCase == 2) {
      addSepsProviderProperties(properties, "oracle.jdbc.provider.username",
        ocid, walletPassword, connectionStringIndex, authentication);
    }
    if (useCase == 1 || useCase == 3) {
      addSepsProviderProperties(properties, "oracle.jdbc.provider.password",
        ocid, walletPassword, connectionStringIndex, authentication);
    }

    addResourceProperties(properties,
      "SEPS Wallet Provider from OCI Vault",
      "#seps-wallet-provider");
  }

  private void addResourceAuthentication(
    Map<String, String> properties, String prefix) {

    addWithPrefix(properties, prefix, readResourceAuthenticationParameters());
  }

  private LinkedHashMap<String, String> readResourceAuthenticationParameters() {
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    switch (promptMenu("Choose an authentication method:",
      "Config file", "Instance principal", "Resource principal",
      "Interactive", "Use defaults")) {
      case 1:
        parameters.put("authenticationMethod", "config-file");
        addIfPresent(parameters, "configFile",
          readOptional("Config file path [default: ~/.oci/config]: "));
        addIfPresent(parameters, "profile",
          readOptional("Profile [default: DEFAULT]: "));
        addIfPresent(parameters, "region",
          readOptional("Region [optional, inferred from the config file "
            + "when not set]: "));
        break;
      case 2:
        parameters.put("authenticationMethod", "instance-principal");
        addIfPresent(parameters, "instancePrincipalTimeout",
          readOptional("Instance principal timeout in seconds [default: 5]: "));
        break;
      case 3:
        parameters.put("authenticationMethod", "resource-principal");
        break;
      case 4:
        parameters.put("authenticationMethod", "interactive");
        addIfPresent(parameters, "region",
          readOptional("Region [optional, recommended for interactive login realm]: "));
        addIfPresent(parameters, "interactiveTimeout",
          readOptional("Interactive timeout in minutes [default: 5]: "));
        break;
      case 5:
        break;
      default:
        throw new AssertionError();
    }

    return parameters;
  }

  private String readScope() {
    switch (promptMenu("Choose a token scope:",
      "One database",
      "All databases in a compartment",
      "All databases in tenancy",
      "Enter full scope manually")) {
      case 1:
        String compartment = readRequired("Compartment OCID [required]: ");
        String database = readRequired("Database OCID [required]: ");
        return TOKEN_SCOPE_PREFIX + compartment + "::" + database;
      case 2:
        return TOKEN_SCOPE_PREFIX + readRequired("Compartment OCID [required]: ");
      case 3:
        return TOKEN_SCOPE_PREFIX + "*";
      case 4:
        return readScopeValue();
      default:
        throw new AssertionError();
    }
  }

  private String readScopeValue() {
    while (true) {
      String scope = readRequired("Scope [required]: ");
      if (scope.startsWith(TOKEN_SCOPE_PREFIX)
        && scope.length() > TOKEN_SCOPE_PREFIX.length()) {
        return scope;
      }
      System.out.println("Scope must start with " + TOKEN_SCOPE_PREFIX);
    }
  }

  @Override
  protected void setupCentralizedConfigUrl() {
    switch (promptMenu("Choose a centralized configuration provider:",
      "OCI Vault",
      "OCI Object Storage",
      "OCI Database Tools Connection",
      "Back")) {
      case 1:
        buildOciVaultConfigUrl();
        break;
      case 2:
        buildOciObjectStorageConfigUrl();
        break;
      case 3:
        buildOciDatabaseToolsConfigUrl();
        break;
      case 4:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void buildOciVaultConfigUrl() {
    addConfigUrl(
      "jdbc:oracle:thin:@config-ocivault://"
        + readRequired("Vault secret OCID [required]: "),
      "OCI Vault centralized configuration provider",
      "#oci-vault-config-provider");
  }

  private void buildOciObjectStorageConfigUrl() {
    addConfigUrl(
      "jdbc:oracle:thin:@config-ociobject://"
        + readRequired("Object Storage URL path, without https:// [required]: "),
      "OCI Object Storage centralized configuration provider",
      "#oci-object-storage-config-provider");
  }

  private void buildOciDatabaseToolsConfigUrl() {
    addConfigUrl(
      "jdbc:oracle:thin:@config-ocidbtools://"
        + readRequired("Database Tools connection OCID [required]: "),
      "OCI Database Tools Connection centralized configuration provider",
      "#oci-database-tools-connections-config-provider");
  }

  private void addConfigUrl(
    String baseUrl, String comment, String docsAnchor) {
    addConfigUrl(baseUrl, centralizedConfigAuth(), comment, docsAnchor);
  }

  private LinkedHashMap<String, String> centralizedConfigAuth() {
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    switch (promptMenu("Choose an authentication method:",
      "Config file", "Instance principal", "Resource principal",
      "Interactive", "Use defaults")) {
      case 1:
        parameters.put("AUTHENTICATION", "OCI_DEFAULT");
        addIfPresent(parameters, "OCI_CONFIG_FILE",
          readOptional("OCI config file [default: OCI SDK default lookup]: "));
        addIfPresent(parameters, "OCI_PROFILE",
          readOptional("OCI profile [default: DEFAULT]: "));
        addIfPresent(parameters, "OCI_REGION",
          readOptional("OCI region [optional, inferred when possible]: "));
        break;
      case 2:
        parameters.put("AUTHENTICATION", "OCI_INSTANCE_PRINCIPAL");
        addIfPresent(parameters, "OCI_INSTANCE_PRINCIPAL_TIMEOUT",
          readOptional("Instance principal timeout in seconds [default: 5]: "));
        break;
      case 3:
        parameters.put("AUTHENTICATION", "OCI_RESOURCE_PRINCIPAL");
        break;
      case 4:
        parameters.put("AUTHENTICATION", "OCI_INTERACTIVE");
        addIfPresent(parameters, "OCI_REGION",
          readOptional("OCI region [optional, recommended for interactive login realm]: "));
        addIfPresent(parameters, "OCI_INTERACTIVE_TIMEOUT",
          readOptional("Interactive timeout in minutes [default: 5]: "));
        break;
      case 5:
        break;
      default:
        throw new AssertionError();
    }

    return parameters;
  }

  private static void addSepsProviderProperties(
    Map<String, String> properties, String prefix, String ocid,
    String walletPassword, String connectionStringIndex,
    Map<String, String> authentication) {

    properties.put(prefix, "ojdbc-provider-oci-vault-seps");
    properties.put(prefix + ".ocid", ocid);
    addIfPresent(properties, prefix + ".walletPassword", walletPassword);
    addIfPresent(properties, prefix + ".connectionStringIndex",
      connectionStringIndex);
    addWithPrefix(properties, prefix, authentication);
  }

}