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

package oracle.jdbc.provider.aws;

import oracle.jdbc.provider.util.cli.ProviderSetupCli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive setup helper for users running this provider jar directly.
 */
public final class AwsProviderSetup extends ProviderSetupCli {

  private static final String README_URL =
    "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-aws/README.md";

  private AwsProviderSetup(Scanner scanner) {
    super(scanner);
  }

  /**
   * Starts the interactive setup helper. Prints a short info banner unless
   * {@code --setup} is passed, in which case it runs the wizard.
   *
   * @param args Pass {@code --setup} to run the wizard.
   */
  public static void main(String[] args) {
    new AwsProviderSetup(new Scanner(System.in)).start(args);
  }

  @Override
  protected String displayName() {
    return "Oracle JDBC Providers for AWS";
  }

  @Override
  protected String description() {
    return "Providers for integration between Oracle JDBC and Amazon Web "
      + "Services (AWS).";
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
    switch (promptMenu("Choose a backing store:",
      "Secrets Manager", "Parameter Store", "Back")) {
      case 1:
        setupSecretsManagerCredentialProvider(
          "oracle.jdbc.provider.username",
          "ojdbc-provider-aws-secrets-manager-username",
          "username",
          "Username Provider from AWS Secrets Manager",
          "#aws-secrets-manager-username-provider");
        break;
      case 2:
        setupParameterStoreCredentialProvider(
          "oracle.jdbc.provider.username",
          "ojdbc-provider-aws-parameter-store-username",
          "username",
          "Username Provider from AWS Parameter Store",
          "#aws-parameter-store-username-provider");
        break;
      case 3:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupPasswordProvider() {
    switch (promptMenu("Choose a backing store:",
      "Secrets Manager", "Parameter Store", "Back")) {
      case 1:
        setupSecretsManagerCredentialProvider(
          "oracle.jdbc.provider.password",
          "ojdbc-provider-aws-secrets-manager-password",
          "password",
          "Password Provider from AWS Secrets Manager",
          "#aws-secrets-manager-password-provider");
        break;
      case 2:
        setupParameterStoreCredentialProvider(
          "oracle.jdbc.provider.password",
          "ojdbc-provider-aws-parameter-store-password",
          "password",
          "Password Provider from AWS Parameter Store",
          "#aws-parameter-store-password-provider");
        break;
      case 3:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupSecretsManagerCredentialProvider(
    String prefix, String providerName, String label, String comment,
    String anchor) {

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, providerName);
    properties.put(prefix + ".secretName",
      readRequired("Secrets Manager secret name for the " + label + " [required]: "));
    addIfPresent(properties, prefix + ".fieldName",
      readOptional("Field name within the secret [optional, only needed "
        + "when the secret holds multiple key-value pairs]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties, comment, anchor);
  }

  private void setupParameterStoreCredentialProvider(
    String prefix, String providerName, String label, String comment,
    String anchor) {

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, providerName);
    properties.put(prefix + ".parameterName",
      readRequired("Parameter Store parameter name for the " + label + " [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties, comment, anchor);
  }

  private void setupConnectionStringProvider() {
    switch (promptMenu("Choose a backing store:",
      "Secrets Manager", "Parameter Store", "Back")) {
      case 1:
        setupSecretsManagerConnectionStringProvider();
        break;
      case 2:
        setupParameterStoreConnectionStringProvider();
        break;
      case 3:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupSecretsManagerConnectionStringProvider() {
    String prefix = "oracle.jdbc.provider.connectionString";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-aws-secrets-manager-tnsnames");
    properties.put(prefix + ".secretName",
      readRequired("Secrets Manager secret name for tnsnames.ora [required]: "));
    properties.put(prefix + ".tnsAlias", readRequired("TNS alias [required]: "));
    addIfPresent(properties, prefix + ".fieldName",
      readOptional("Field name within the secret [optional, only needed "
        + "when the secret holds multiple key-value pairs]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Connection String Provider from AWS Secrets Manager",
      "#aws-secrets-manager-connection-string-provider");
  }

  private void setupParameterStoreConnectionStringProvider() {
    String prefix = "oracle.jdbc.provider.connectionString";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-aws-parameter-store-tnsnames");
    properties.put(prefix + ".parameterName",
      readRequired("Parameter Store parameter name for tnsnames.ora [required]: "));
    properties.put(prefix + ".tnsAlias", readRequired("TNS alias [required]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "Connection String Provider from AWS Parameter Store",
      "#aws-parameter-store-connection-string-provider");
  }

  private void setupTcpsProvider() {
    String prefix = "oracle.jdbc.provider.tlsConfiguration";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-aws-secrets-manager-tls");
    properties.put(prefix + ".secretName",
      readRequired("Secrets Manager secret name for the TCPS wallet/file [required]: "));
    properties.put(prefix + ".type",
      readRequired("File type (SSO, PKCS12, or PEM) [required]: "));
    addIfPresent(properties, prefix + ".walletPassword",
      readOptional("Wallet password [optional, required only for PKCS12 "
        + "or password-protected PEM files]: "));
    addIfPresent(properties, prefix + ".fieldName",
      readOptional("Field name within the secret [optional, only needed "
        + "when the secret holds multiple key-value pairs]: "));
    addResourceAuthentication(properties, prefix);

    addResourceProperties(properties,
      "TCPS Wallet Provider from AWS Secrets Manager",
      "#aws-secrets-manager-tcps-wallet-provider");
  }

  private void setupSepsProvider() {
    switch (promptMenu("Choose a backing store:",
      "Secrets Manager", "Parameter Store", "Back")) {
      case 1:
        setupSecretsManagerSepsProvider();
        break;
      case 2:
        setupParameterStoreSepsProvider();
        break;
      case 3:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupSecretsManagerSepsProvider() {
    int useCase = promptMenu("Choose what to configure from the SEPS wallet:",
      "Username and password", "Username only", "Password only");
    String secretName =
      readRequired("Secrets Manager secret name for the SEPS wallet [required]: ");
    String walletPassword = readOptional(
      "Wallet password [optional, required only for PKCS12 wallets]: ");
    String connectionStringIndex =
      readOptional("Connection string index [optional, no default]: ");
    String fieldName = readOptional(
      "Field name within the secret [optional, only needed when the "
        + "secret holds multiple key-value pairs]: ");
    LinkedHashMap<String, String> authentication =
      resourceAuthenticationParameters();

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    if (useCase == 1 || useCase == 2) {
      addSepsProviderProperties(properties, "oracle.jdbc.provider.username",
        "ojdbc-provider-aws-secrets-manager-seps", "secretName", secretName,
        walletPassword, connectionStringIndex, fieldName, authentication);
    }
    if (useCase == 1 || useCase == 3) {
      addSepsProviderProperties(properties, "oracle.jdbc.provider.password",
        "ojdbc-provider-aws-secrets-manager-seps", "secretName", secretName,
        walletPassword, connectionStringIndex, fieldName, authentication);
    }

    addResourceProperties(properties,
      "SEPS Wallet Provider from AWS Secrets Manager",
      "#aws-secrets-manager-seps-wallet-provider");
  }

  private void setupParameterStoreSepsProvider() {
    int useCase = promptMenu("Choose what to configure from the SEPS wallet:",
      "Username and password", "Username only", "Password only");
    String parameterName =
      readRequired("Parameter Store parameter name for the SEPS wallet [required]: ");
    String walletPassword = readOptional(
      "Wallet password [optional, required only for PKCS12 wallets]: ");
    String connectionStringIndex =
      readOptional("Connection string index [optional, no default]: ");
    LinkedHashMap<String, String> authentication =
      resourceAuthenticationParameters();

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    if (useCase == 1 || useCase == 2) {
      addSepsProviderProperties(properties, "oracle.jdbc.provider.username",
        "ojdbc-provider-aws-parameter-store-seps", "parameterName",
        parameterName, walletPassword, connectionStringIndex, null,
        authentication);
    }
    if (useCase == 1 || useCase == 3) {
      addSepsProviderProperties(properties, "oracle.jdbc.provider.password",
        "ojdbc-provider-aws-parameter-store-seps", "parameterName",
        parameterName, walletPassword, connectionStringIndex, null,
        authentication);
    }

    addResourceProperties(properties,
      "SEPS Wallet Provider from AWS Parameter Store",
      "#aws-parameter-store-seps-wallet-provider");
  }

  private static void addSepsProviderProperties(
    Map<String, String> properties, String prefix, String providerName,
    String storeKey, String storeValue, String walletPassword,
    String connectionStringIndex, String fieldName,
    Map<String, String> authentication) {

    properties.put(prefix, providerName);
    properties.put(prefix + "." + storeKey, storeValue);
    addIfPresent(properties, prefix + ".walletPassword", walletPassword);
    addIfPresent(properties, prefix + ".connectionStringIndex",
      connectionStringIndex);
    addIfPresent(properties, prefix + ".fieldName", fieldName);
    addWithPrefix(properties, prefix, authentication);
  }

  private void addResourceAuthentication(
    Map<String, String> properties, String prefix) {

    addWithPrefix(properties, prefix, resourceAuthenticationParameters());
  }

  private LinkedHashMap<String, String> resourceAuthenticationParameters() {
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
    parameters.put("authenticationMethod", "aws-default");
    addIfPresent(parameters, "awsRegion",
      readOptional("AWS region [optional, e.g. us-west-2; falls back to "
        + "the default AWS region provider chain]: "));
    return parameters;
  }

  @Override
  protected void setupCentralizedConfigUrl() {
    switch (promptMenu("Choose a centralized configuration provider:",
      "AWS S3",
      "AWS Secrets Manager",
      "AWS Parameter Store",
      "AWS AppConfig",
      "Back")) {
      case 1:
        buildAwsS3ConfigUrl();
        break;
      case 2:
        buildAwsSecretsManagerConfigUrl();
        break;
      case 3:
        buildAwsParameterStoreConfigUrl();
        break;
      case 4:
        buildAwsAppConfigConfigUrl();
        break;
      case 5:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void buildAwsS3ConfigUrl() {
    String location = readRequired(
      "S3 location, with or without the s3:// prefix "
        + "(e.g. mybucket/payload.json) [required]: ");
    LinkedHashMap<String, String> parameters = centralizedConfigAuth();
    addIfPresent(parameters, "key",
      readOptional("Key of the datasource, if the JSON payload contains "
        + "multiple datasource configurations [optional]: "));

    addConfigUrl(
      "jdbc:oracle:thin:@config-awss3://" + location,
      parameters,
      "AWS S3 Configuration Provider",
      "#aws-s3-configuration-provider");
  }

  private void buildAwsSecretsManagerConfigUrl() {
    String secretName = readRequired("Secrets Manager secret name [required]: ");
    LinkedHashMap<String, String> parameters = centralizedConfigAuth();
    addIfPresent(parameters, "key",
      readOptional("Key of the datasource, if the JSON payload contains "
        + "multiple datasource configurations [optional]: "));
    addIfPresent(parameters, "field_name",
      readOptional("Field name within the secret [optional, only needed "
        + "when the secret holds multiple key-value pairs]: "));

    addConfigUrl(
      "jdbc:oracle:thin:@config-awssecretsmanager://" + secretName,
      parameters,
      "AWS Secrets Manager Config Provider",
      "#aws-secrets-manager-config-provider");
  }

  private void buildAwsParameterStoreConfigUrl() {
    String parameterName = readRequired("Parameter Store parameter name [required]: ");
    LinkedHashMap<String, String> parameters = centralizedConfigAuth();
    addIfPresent(parameters, "key",
      readOptional("Key of the datasource, if the JSON payload contains "
        + "multiple datasource configurations [optional]: "));

    addConfigUrl(
      "jdbc:oracle:thin:@config-awsparameterstore://" + parameterName,
      parameters,
      "AWS Parameter Store Config Provider",
      "#aws-parameter-store-config-provider");
  }

  private void buildAwsAppConfigConfigUrl() {
    String applicationIdentifier =
      readRequired("AppConfig application identifier [required]: ");
    LinkedHashMap<String, String> parameters = centralizedConfigAuth();
    addIfPresent(parameters, "appconfig_environment",
      readOptional("AppConfig environment id or name [optional]: "));
    addIfPresent(parameters, "appconfig_profile",
      readOptional("AppConfig profile id or name [optional]: "));
    addIfPresent(parameters, "key",
      readOptional("Key of the datasource, if the JSON payload contains "
        + "multiple datasource configurations [optional]: "));

    addConfigUrl(
      "jdbc:oracle:thin:@config-awsappconfig://" + applicationIdentifier,
      parameters,
      "AWS AppConfig Freeform Config Provider",
      "#aws-appconfig-freeform-config-provider");
  }

  private LinkedHashMap<String, String> centralizedConfigAuth() {
    LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
    parameters.put("AUTHENTICATION", "AWS_DEFAULT");
    addIfPresent(parameters, "AWS_REGION",
      readOptional("AWS region [optional, e.g. us-west-2; falls back to "
        + "the default AWS region provider chain]: "));
    return parameters;
  }
}