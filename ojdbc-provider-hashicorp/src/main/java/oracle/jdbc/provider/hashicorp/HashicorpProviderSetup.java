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

package oracle.jdbc.provider.hashicorp;

import oracle.jdbc.provider.util.cli.ProviderSetupCli;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive setup helper for users running this provider jar directly.
 */
public final class HashicorpProviderSetup extends ProviderSetupCli {

  private static final String README_URL =
    "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-hashicorp/README.md";

  private HashicorpProviderSetup(Scanner scanner) {
    super(scanner);
  }

  /**
   * Starts the interactive setup helper. Prints a short info banner unless
   * {@code --setup} is passed, in which case it runs the wizard.
   *
   * @param args Pass {@code --setup} to run the wizard.
   */
  public static void main(String[] args) {
    new HashicorpProviderSetup(new Scanner(System.in)).start(args);
  }

  @Override
  protected String displayName() {
    return "Oracle JDBC Providers for HashiCorp Vault";
  }

  @Override
  protected String description() {
    return "Providers for integration between Oracle JDBC and HashiCorp Vault (HCP).";
  }

  @Override
  protected String readmeUrl() {
    return README_URL;
  }

  @Override
  protected void setupResourceProvider() {
    switch (promptMenu("Choose a resource provider:",
      "Username",
      "Password",
      "Connection String",
      "TCPS Wallet",
      "SEPS Wallet",
      "Back")) {
      case 1:
        setupUsernameProvider();
        break;
      case 2:
        setupPasswordProvider();
        break;
      case 3:
        setupConnectionStringProvider();
        break;
      case 4:
        setupTcpsProvider();
        break;
      case 5:
        setupSepsProvider();
        break;
      case 6:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupUsernameProvider() {
    String prefix = "oracle.jdbc.provider.username";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-hcpvault-dedicated-username");
    properties.put(prefix + ".secretPath", readSecretPath());
    addIfPresent(properties, prefix + ".fieldName", readFieldName());
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Username Provider from HCP Vault Dedicated",
      "#dedicated-vault-username-provider");
  }

  private void setupPasswordProvider() {
    String prefix = "oracle.jdbc.provider.password";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-hcpvault-dedicated-password");
    properties.put(prefix + ".secretPath", readSecretPath());
    addIfPresent(properties, prefix + ".fieldName", readFieldName());
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Password Provider from HCP Vault Dedicated",
      "#dedicated-vault-password-provider");
  }

  private void setupConnectionStringProvider() {
    String prefix = "oracle.jdbc.provider.connectionString";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-hcpvault-dedicated-tnsnames");
    properties.put(prefix + ".secretPath", readSecretPath());
    addIfPresent(properties, prefix + ".fieldName", readFieldName());
    properties.put(prefix + ".tnsAlias", readRequired("TNS alias [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Connection String Provider from HCP Vault Dedicated",
      "#dedicated-vault-connection-string-provider");
  }

  private void setupTcpsProvider() {
    String prefix = "oracle.jdbc.provider.tlsConfiguration";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-hcpvault-dedicated-tls");
    properties.put(prefix + ".secretPath", readSecretPath());
    addIfPresent(properties, prefix + ".fieldName", readFieldName());
    properties.put(prefix + ".type",
      readRequired("File type (SSO, PKCS12, or PEM) [required]: "));
    addIfPresent(properties, prefix + ".walletPassword",
      readOptional("Wallet password [optional, required only for PKCS12 "
        + "or password-protected PEM files, for example ${TLS_FILE_PASSWORD}]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "TCPS Wallet Provider from HCP Vault Dedicated",
      "#dedicated-vault-tcps-wallet-provider");
  }

  private void setupSepsProvider() {
    String secretPath = readSecretPath();
    String fieldName = readFieldName();
    String walletPassword = readOptional(
      "Wallet password [optional, required only for PKCS12 wallets, "
        + "for example ${SEPS_WALLET_PASSWORD}]: ");
    String connectionStringIndex =
      readOptional("Connection string index [optional, no default]: ");
    LinkedHashMap<String, String> authentication =
      readResourceAuthenticationParameters();

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    addSepsProviderProperties(properties, "oracle.jdbc.provider.username",
      secretPath, fieldName, walletPassword, connectionStringIndex,
      authentication);
    addSepsProviderProperties(properties, "oracle.jdbc.provider.password",
      secretPath, fieldName, walletPassword, connectionStringIndex,
      authentication);

    addResourceProperties(properties,
      "SEPS Wallet Provider from HCP Vault Dedicated",
      "#dedicated-vault-seps-wallet-provider");
  }

  private static void addSepsProviderProperties(
    Map<String, String> properties, String prefix, String secretPath,
    String fieldName, String walletPassword, String connectionStringIndex,
    Map<String, String> authentication) {

    properties.put(prefix, "ojdbc-provider-hcpvault-dedicated-seps");
    properties.put(prefix + ".secretPath", secretPath);
    addIfPresent(properties, prefix + ".fieldName", fieldName);
    addIfPresent(properties, prefix + ".walletPassword", walletPassword);
    addIfPresent(properties, prefix + ".connectionStringIndex",
      connectionStringIndex);
    addWithPrefix(properties, prefix, authentication);
  }

  private String readSecretPath() {
    return readRequired(
      "Vault secret path (e.g. secret/data/mydb) [required]: ");
  }

  private String readFieldName() {
    return readOptional(
      "Field name within the secret [optional, only needed when the "
        + "secret holds multiple key-value pairs]: ");
  }

  private void addResourceAuthentication(
    Map<String, String> properties, String prefix) {

    addWithPrefix(properties, prefix, readResourceAuthenticationParameters());
  }

  private LinkedHashMap<String, String> readResourceAuthenticationParameters() {
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    String vaultAddr = readOptional(
      "Vault address [optional, e.g. https://vault.example.com:8200; "
        + "falls back to VAULT_ADDR env var]: ");
    addIfPresent(parameters, "vaultAddr", vaultAddr);
    printInsecureVaultAddrNoticeIfNeeded(vaultAddr);
    addIfPresent(parameters, "vaultNamespace",
      readOptional("Vault namespace [optional, default: admin]: "));

    switch (promptMenu("Choose an authentication method:",
      "Vault token",
      "Userpass",
      "AppRole",
      "GitHub",
      "Auto-detect (default)")) {
      case 1:
        parameters.put("authenticationMethod", "vault-token");
        parameters.put("vaultToken", readRequired("Vault token [required]: "));
        break;
      case 2:
        parameters.put("authenticationMethod", "userpass");
        parameters.put("vaultUsername", readRequired("Vault username [required]: "));
        parameters.put("vaultPassword", readRequired("Vault password [required]: "));
        addIfPresent(parameters, "userPassAuthPath",
          readOptional("Userpass auth path [optional, default: userpass]: "));
        break;
      case 3:
        parameters.put("authenticationMethod", "approle");
        parameters.put("roleId", readRequired("Role ID [required]: "));
        parameters.put("secretId", readRequired("Secret ID [required]: "));
        addIfPresent(parameters, "appRoleAuthPath",
          readOptional("AppRole auth path [optional, default: approle]: "));
        break;
      case 4:
        parameters.put("authenticationMethod", "github");
        parameters.put("githubToken", readRequired("GitHub token [required]: "));
        addIfPresent(parameters, "githubAuthPath",
          readOptional("GitHub auth path [optional, default: github]: "));
        break;
      case 5:
        break;
      default:
        throw new AssertionError();
    }

    return parameters;
  }

  @Override
  protected void setupCentralizedConfigUrl() {
    switch (promptMenu("Choose a centralized configuration provider:",
      "HCP Vault Dedicated",
      "Back")) {
      case 1:
        buildHcpVaultDedicatedConfigUrl();
        break;
      case 2:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void buildHcpVaultDedicatedConfigUrl() {
    String secretPath = readRequired(
      "Secret path (e.g. /v1/namespace/secret/data/secret_name) [required]: ");
    LinkedHashMap<String, String> parameters = centralizedConfigAuth();
    addIfPresent(parameters, "KEY",
      readOptional("Key to extract, if the secret holds multiple named "
        + "configurations [optional]: "));
    addIfPresent(parameters, "FIELD_NAME",
      readOptional("Field name within the secret [optional, only needed "
        + "when the secret holds multiple key-value pairs]: "));

    addConfigUrl(
      "jdbc:oracle:thin:@config-hcpvaultdedicated://" + secretPath,
      parameters,
      "HCP Vault Dedicated Config Provider",
      "#hcp-vault-dedicated-config-provider");
  }

  private LinkedHashMap<String, String> centralizedConfigAuth() {
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    String vaultAddr = readOptional(
      "Vault address [optional, e.g. https://vault.example.com:8200; "
        + "falls back to VAULT_ADDR env var]: ");
    addIfPresent(parameters, "VAULT_ADDR", vaultAddr);
    printInsecureVaultAddrNoticeIfNeeded(vaultAddr);
    addIfPresent(parameters, "VAULT_NAMESPACE",
      readOptional("Vault namespace [optional, default: admin]: "));

    switch (promptMenu("Choose an authentication method:",
      "Vault token",
      "Userpass",
      "AppRole",
      "GitHub",
      "Auto-detect (default)")) {
      case 1:
        parameters.put("authentication", "vault_token");
        addIfPresent(parameters, "VAULT_TOKEN",
          readOptional("Vault token [optional, falls back to VAULT_TOKEN "
            + "system property or env var]: "));
        break;
      case 2:
        parameters.put("authentication", "userpass");
        addIfPresent(parameters, "VAULT_USERNAME",
          readOptional("Vault username [optional, falls back to "
            + "VAULT_USERNAME system property or env var]: "));
        addIfPresent(parameters, "VAULT_PASSWORD",
          readOptional("Vault password [optional, falls back to "
            + "VAULT_PASSWORD system property or env var]: "));
        addIfPresent(parameters, "USERPASS_AUTH_PATH",
          readOptional("Userpass auth path [optional, default: userpass]: "));
        break;
      case 3:
        parameters.put("authentication", "approle");
        addIfPresent(parameters, "ROLE_ID",
          readOptional("Role ID [optional, falls back to ROLE_ID system "
            + "property or env var]: "));
        addIfPresent(parameters, "SECRET_ID",
          readOptional("Secret ID [optional, falls back to SECRET_ID "
            + "system property or env var]: "));
        addIfPresent(parameters, "APPROLE_AUTH_PATH",
          readOptional("AppRole auth path [optional, default: approle]: "));
        break;
      case 4:
        parameters.put("authentication", "github");
        addIfPresent(parameters, "GITHUB_TOKEN",
          readOptional("GitHub token [optional, falls back to GITHUB_TOKEN "
            + "system property or env var]: "));
        addIfPresent(parameters, "GITHUB_AUTH_PATH",
          readOptional("GitHub auth path [optional, default: github]: "));
        break;
      case 5:
        break;
      default:
        throw new AssertionError();
    }

    return parameters;
  }

  private void printInsecureVaultAddrNoticeIfNeeded(String vaultAddr) {
    if (vaultAddr != null
      && vaultAddr.toLowerCase(Locale.ROOT).startsWith("http://")) {
      System.out.println();
      System.out.println(
        "Note: this Vault address uses plain HTTP. At runtime, "
          + "ALLOW_INSECURE_VAULT_ADDR must be set to true, and "
          + "TRUSTED_VAULT_HOSTS must include this host, or requests to "
          + "Vault will be rejected.");
    }
  }
}