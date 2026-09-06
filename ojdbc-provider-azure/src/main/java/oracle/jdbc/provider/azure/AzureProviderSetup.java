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

package oracle.jdbc.provider.azure;

import oracle.jdbc.provider.util.cli.ProviderSetupCli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive setup helper for users running this provider jar directly.
 */
public final class AzureProviderSetup extends ProviderSetupCli {

  private static final String README_URL =
    "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-azure/README.md";

  private AzureProviderSetup(Scanner scanner) {
    super(scanner);
  }

  /**
   * Starts the interactive setup helper. Prints a short info banner unless
   * {@code --setup} is passed, in which case it runs the wizard.
   *
   * @param args Pass {@code --setup} to run the wizard.
   */
  public static void main(String[] args) {
    new AzureProviderSetup(new Scanner(System.in)).start(args);
  }

  @Override
  protected String displayName() {
    return "Oracle JDBC Providers for Azure";
  }

  @Override
  protected String description() {
    return "Providers for integration between Oracle JDBC and Azure Services.";
  }

  @Override
  protected String readmeUrl() {
    return README_URL;
  }

  @Override
  protected void setupResourceProvider() {
    switch (promptMenu("Choose a resource provider:",
      "Access Token",
      "Username",
      "Password",
      "Connection String",
      "TCPS Wallet",
      "SEPS Wallet",
      "Back")) {
      case 1:
        setupAccessTokenProvider();
        break;
      case 2:
        setupUsernameProvider();
        break;
      case 3:
        setupPasswordProvider();
        break;
      case 4:
        setupConnectionStringProvider();
        break;
      case 5:
        setupTcpsProvider();
        break;
      case 6:
        setupSepsProvider();
        break;
      case 7:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupAccessTokenProvider() {
    String prefix = "oracle.jdbc.provider.accessToken";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-azure-token");
    properties.put(prefix + ".scope", readRequired(
      "Scope: the Application ID URI of the database registered with "
        + "Active Directory, optionally followed by a scope name "
        + "(e.g. https://example.com/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/.default) "
        + "[required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Access Token Provider from Azure Active Directory",
      "#access-token-provider");
  }

  private void setupUsernameProvider() {
    String prefix = "oracle.jdbc.provider.username";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-azure-key-vault-username");
    properties.put(prefix + ".vaultUrl",
      readRequired("Key Vault URL (e.g. https://myvault.vault.azure.net) [required]: "));
    properties.put(prefix + ".secretName", readRequired("Secret name [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Username Provider from Azure Key Vault",
      "#key-vault-username-provider");
  }

  private void setupPasswordProvider() {
    String prefix = "oracle.jdbc.provider.password";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-azure-key-vault-password");
    properties.put(prefix + ".vaultUrl",
      readRequired("Key Vault URL (e.g. https://myvault.vault.azure.net) [required]: "));
    properties.put(prefix + ".secretName", readRequired("Secret name [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Password Provider from Azure Key Vault",
      "#key-vault-password-provider");
  }

  private void setupConnectionStringProvider() {
    String prefix = "oracle.jdbc.provider.connectionString";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-azure-key-vault-tnsnames");
    properties.put(prefix + ".vaultUrl",
      readRequired("Key Vault URL (e.g. https://myvault.vault.azure.net) [required]: "));
    properties.put(prefix + ".secretName",
      readRequired("Secret name for tnsnames.ora [required]: "));
    properties.put(prefix + ".tnsAlias", readRequired("TNS alias [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Connection String Provider from Azure Key Vault",
      "#key-vault-connection-string-provider");
  }

  private void setupTcpsProvider() {
    String prefix = "oracle.jdbc.provider.tlsConfiguration";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-azure-key-vault-tls");
    properties.put(prefix + ".vaultUrl",
      readRequired("Key Vault URL (e.g. https://myvault.vault.azure.net) [required]: "));
    properties.put(prefix + ".secretName",
      readRequired("Secret name for the TCPS wallet/file [required]: "));
    properties.put(prefix + ".type",
      readRequired("File type (SSO, PKCS12, or PEM) [required]: "));
    addIfPresent(properties, prefix + ".walletPassword",
      readOptional("Wallet password [optional, required only for PKCS12 "
        + "or password-protected PEM files]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "TCPS Wallet Provider from Azure Key Vault",
      "#key-vault-tcps-wallet-provider");
  }

  private void setupSepsProvider() {
    String vaultUrl =
      readRequired("Key Vault URL (e.g. https://myvault.vault.azure.net) [required]: ");
    String secretName = readRequired("Secret name for the SEPS wallet [required]: ");
    String walletPassword = readOptional(
      "Wallet password [optional, required only for PKCS12 wallets]: ");
    String connectionStringIndex =
      readOptional("Connection string index [optional, no default]: ");
    LinkedHashMap<String, String> authentication =
      readResourceAuthenticationParameters();

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    addSepsProviderProperties(properties, "oracle.jdbc.provider.username",
      vaultUrl, secretName, walletPassword, connectionStringIndex,
      authentication);
    addSepsProviderProperties(properties, "oracle.jdbc.provider.password",
      vaultUrl, secretName, walletPassword, connectionStringIndex,
      authentication);

    addResourceProperties(properties,
      "SEPS Wallet Provider from Azure Key Vault",
      "#key-vault-seps-wallet-provider");
  }

  private static void addSepsProviderProperties(
    Map<String, String> properties, String prefix, String vaultUrl,
    String secretName, String walletPassword, String connectionStringIndex,
    Map<String, String> authentication) {

    properties.put(prefix, "ojdbc-provider-azure-key-vault-seps");
    properties.put(prefix + ".vaultUrl", vaultUrl);
    properties.put(prefix + ".secretName", secretName);
    addIfPresent(properties, prefix + ".walletPassword", walletPassword);
    addIfPresent(properties, prefix + ".connectionStringIndex",
      connectionStringIndex);
    addWithPrefix(properties, prefix, authentication);
  }

  private void addResourceAuthentication(
    Map<String, String> properties, String prefix) {

    addWithPrefix(properties, prefix, readResourceAuthenticationParameters());
  }

  private LinkedHashMap<String, String> readResourceAuthenticationParameters() {
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    switch (promptMenu("Choose an authentication method:",
      "Service Principal",
      "Managed Identity",
      "Password",
      "Interactive",
      "Device Code",
      "Auto-detect (default)")) {
      case 1:
        parameters.put("authenticationMethod", "service-principal");
        readServicePrincipalParameters(parameters, false);
        break;
      case 2:
        parameters.put("authenticationMethod", "managed-identity");
        addIfPresent(parameters, "clientId",
          readOptional("Client ID [optional, only required for a "
            + "user-assigned managed identity]: "));
        break;
      case 3:
        parameters.put("authenticationMethod", "password");
        addIfPresent(parameters, "tenantId",
          readOptional("Tenant ID [optional, falls back to "
            + "AZURE_TENANT_ID env var]: "));
        addIfPresent(parameters, "clientId",
          readOptional("Client ID [optional, falls back to "
            + "AZURE_CLIENT_ID env var]: "));
        addIfPresent(parameters, "username",
          readOptional("Azure account username [optional, falls back to "
            + "AZURE_USERNAME env var]: "));
        addIfPresent(parameters, "password",
          readOptional("Azure account password [optional, falls back to "
            + "AZURE_PASSWORD env var, for example ${AZURE_PASSWORD}]: "));
        break;
      case 4:
        parameters.put("authenticationMethod", "interactive");
        addIfPresent(parameters, "tenantId",
          readOptional("Tenant ID [optional]: "));
        addIfPresent(parameters, "clientId",
          readOptional("Client ID [optional]: "));
        addIfPresent(parameters, "redirectUri",
          readOptional("Redirect URL [optional, default: "
            + "http://localhost, redirects to any available port]: "));
        break;
      case 5:
        parameters.put("authenticationMethod", "device-code");
        addIfPresent(parameters, "tenantId",
          readOptional("Tenant ID [optional]: "));
        addIfPresent(parameters, "clientId",
          readOptional("Client ID [optional]: "));
        break;
      case 6:
        break;
      default:
        throw new AssertionError();
    }

    return parameters;
  }

  private void readServicePrincipalParameters(
    LinkedHashMap<String, String> parameters, boolean centralizedConfigKeys) {

    addIfPresent(parameters, centralizedConfigKeys ? "AZURE_TENANT_ID" : "tenantId",
      readOptional("Tenant ID [optional, falls back to "
        + "AZURE_TENANT_ID env var]: "));
    addIfPresent(parameters, centralizedConfigKeys ? "AZURE_CLIENT_ID" : "clientId",
      readOptional("Client ID [optional, falls back to "
        + "AZURE_CLIENT_ID env var]: "));

    switch (promptMenu("Choose a service principal credential type:",
      "Client secret",
      "Client certificate")) {
      case 1:
        addIfPresent(parameters,
          centralizedConfigKeys ? "AZURE_CLIENT_SECRET" : "clientSecret",
          readOptional("Client secret [optional, falls back to "
            + "AZURE_CLIENT_SECRET env var, for example "
            + "${AZURE_CLIENT_SECRET}]: "));
        break;
      case 2:
        addIfPresent(parameters,
          centralizedConfigKeys
            ? "AZURE_CLIENT_CERTIFICATE_PATH" : "clientCertificatePath",
          readOptional("Client certificate path, .pem or .pfx [optional, "
            + "falls back to AZURE_CLIENT_CERTIFICATE_PATH env var]: "));
        addIfPresent(parameters,
          centralizedConfigKeys
            ? "AZURE_CLIENT_CERTIFICATE_PASSWORD"
            : "clientCertificatePassword",
          readOptional("Client certificate password [optional, only for "
            + "PFX certificates, for example "
            + "${AZURE_CLIENT_CERTIFICATE_PASSWORD}]: "));
        break;
      default:
        throw new AssertionError();
    }
  }

  @Override
  protected void setupCentralizedConfigUrl() {
    switch (promptMenu("Choose a centralized configuration provider:",
      "Azure App Configuration",
      "Azure Vault",
      "Back")) {
      case 1:
        buildAzureAppConfigurationUrl();
        break;
      case 2:
        buildAzureVaultConfigUrl();
        break;
      case 3:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void buildAzureAppConfigurationUrl() {
    String name = readRequired("App Configuration store name [required]: ");
    LinkedHashMap<String, String> parameters = centralizedConfigAuth();
    addIfPresent(parameters, "key",
      readOptional("Key prefix [optional, e.g. /sales_app1/]: "));
    addIfPresent(parameters, "label", readOptional("Label [optional]: "));

    addConfigUrl(
      "jdbc:oracle:thin:@config-azure://" + name,
      parameters,
      "Azure App Configuration Provider",
      "#azure-app-configuration-provider");
  }

  private void buildAzureVaultConfigUrl() {
    String secretIdentifier = readRequired(
      "Vault secret identifier "
        + "(e.g. https://myvault.vault.azure.net/secrets/mySecretName) "
        + "[required]: ");
    LinkedHashMap<String, String> parameters = centralizedConfigAuth();
    addIfPresent(parameters, "key",
      readOptional("Key of the datasource, if the JSON payload contains "
        + "multiple datasource configurations [optional]: "));

    addConfigUrl(
      "jdbc:oracle:thin:@config-azurevault://" + secretIdentifier,
      parameters,
      "Azure Vault Config Provider",
      "#azure-vault-config-provider");
  }

  private LinkedHashMap<String, String> centralizedConfigAuth() {
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    switch (promptMenu("Choose an authentication method:",
      "Default",
      "Service Principal",
      "Managed Identity",
      "Interactive")) {
      case 1:
        // Not setting "AUTHENTICATION" here: AZURE_DEFAULT is already the
        // default applied when this key is absent.
        addIfPresent(parameters, "AZURE_TENANT_ID",
          readOptional("Tenant ID [optional, falls back to "
            + "AZURE_TENANT_ID env var]: "));
        addIfPresent(parameters, "AZURE_CLIENT_ID",
          readOptional("Managed identity client ID [optional, only for "
            + "a user-assigned managed identity]: "));
        break;
      case 2:
        parameters.put("AUTHENTICATION", "AZURE_SERVICE_PRINCIPAL");
        readServicePrincipalParameters(parameters, true);
        break;
      case 3:
        parameters.put("AUTHENTICATION", "AZURE_MANAGED_IDENTITY");
        addIfPresent(parameters, "AZURE_CLIENT_ID",
          readOptional("Client ID [optional, only required for a "
            + "user-assigned managed identity]: "));
        break;
      case 4:
        parameters.put("AUTHENTICATION", "AZURE_INTERACTIVE");
        addIfPresent(parameters, "AZURE_TENANT_ID",
          readOptional("Tenant ID [optional]: "));
        addIfPresent(parameters, "AZURE_CLIENT_ID",
          readOptional("Client ID [optional]: "));
        addIfPresent(parameters, "AZURE_REDIRECT_URL",
          readOptional("Redirect URL [optional, default: "
            + "http://localhost, redirects to any available port]: "));
        break;
      default:
        throw new AssertionError();
    }

    return parameters;
  }
}