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

package oracle.jdbc.provider.gcp;

import oracle.jdbc.provider.util.cli.ProviderSetupCli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive setup helper for users running this provider jar directly.
 */
public final class GcpProviderSetup extends ProviderSetupCli {

  private static final String README_URL =
    "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-gcp/README.md";

  private GcpProviderSetup(Scanner scanner) {
    super(scanner);
  }

  /**
   * Starts the interactive setup helper. Prints a short info banner unless
   * {@code --setup} is passed, in which case it runs the wizard.
   *
   * @param args Pass {@code --setup} to run the wizard.
   */
  public static void main(String[] args) {
    new GcpProviderSetup(new Scanner(System.in)).start(args);
  }

  @Override
  protected String displayName() {
    return "Oracle JDBC Providers for GCP";
  }

  @Override
  protected String description() {
    return "Providers for integration between Oracle JDBC and Google "
      + "Cloud Platform (GCP).";
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
    properties.put(prefix, "ojdbc-provider-gcp-secretmanager-username");
    properties.put(prefix + ".secretVersionName", readSecretVersionName());

    addResourceProperties(properties,
      "Username Provider from GCP Secret Manager",
      "#secret-manager-username-provider");
  }

  private void setupPasswordProvider() {
    String prefix = "oracle.jdbc.provider.password";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-gcp-secretmanager-password");
    properties.put(prefix + ".secretVersionName", readSecretVersionName());

    addResourceProperties(properties,
      "Password Provider from GCP Secret Manager",
      "#secret-manager-password-provider");
  }

  private void setupConnectionStringProvider() {
    String prefix = "oracle.jdbc.provider.connectionString";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-gcp-secretmanager-tnsnames");
    properties.put(prefix + ".secretVersionName", readSecretVersionName());
    properties.put(prefix + ".tnsAlias", readRequired("TNS alias [required]: "));

    addResourceProperties(properties,
      "Connection String Provider from GCP Secret Manager",
      "#secret-manager-connection-string-provider");
  }

  private void setupTcpsProvider() {
    String prefix = "oracle.jdbc.provider.tlsConfiguration";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-gcp-secretmanager-tls");
    properties.put(prefix + ".secretVersionName", readSecretVersionName());
    properties.put(prefix + ".type",
      readRequired("File type (SSO, PKCS12, or PEM) [required]: "));
    addIfPresent(properties, prefix + ".walletPassword",
      readOptional("Wallet password [optional, required only for PKCS12 "
        + "or password-protected PEM files]: "));

    addResourceProperties(properties,
      "TCPS Wallet Provider from GCP Secret Manager",
      "#secret-manager-tcps-wallet-provider");
  }

  private void setupSepsProvider() {
    String secretVersionName = readSecretVersionName();
    String walletPassword = readOptional(
      "Wallet password [optional, required only for PKCS12 wallets]: ");
    String connectionStringIndex =
      readOptional("Connection string index [optional, no default]: ");

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    addSepsProviderProperties(properties, "oracle.jdbc.provider.username",
      secretVersionName, walletPassword, connectionStringIndex);
    addSepsProviderProperties(properties, "oracle.jdbc.provider.password",
      secretVersionName, walletPassword, connectionStringIndex);

    addResourceProperties(properties,
      "SEPS Wallet Provider from GCP Secret Manager",
      "#secret-manager-seps-wallet-provider");
  }

  private static void addSepsProviderProperties(
    Map<String, String> properties, String prefix, String secretVersionName,
    String walletPassword, String connectionStringIndex) {

    properties.put(prefix, "ojdbc-provider-gcp-secretmanager-seps");
    properties.put(prefix + ".secretVersionName", secretVersionName);
    addIfPresent(properties, prefix + ".walletPassword", walletPassword);
    addIfPresent(properties, prefix + ".connectionStringIndex",
      connectionStringIndex);
  }

  private String readSecretVersionName() {
    return readRequired(
      "Secret Manager secret version resource name "
        + "(e.g. projects/{project}/secrets/{secret}/versions/{version}) "
        + "[required]: ");
  }

  @Override
  protected void setupCentralizedConfigUrl() {
    switch (promptMenu("Choose a centralized configuration provider:",
      "GCP Cloud Storage",
      "GCP Secret Manager",
      "Back")) {
      case 1:
        buildGcpCloudStorageConfigUrl();
        break;
      case 2:
        buildGcpSecretManagerConfigUrl();
        break;
      case 3:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void buildGcpCloudStorageConfigUrl() {
    switch (promptMenu("Choose a Cloud Storage location format:",
      "Bucket and object (optionally with project)",
      "gs:// URI",
      "storage.googleapis.com URL",
      "Back")) {
      case 1:
        buildGcpCloudStorageNamedValuesUrl();
        break;
      case 2:
        addGcpCloudStorageConfigUrl(
          readRequired("gs:// URI (e.g. gs://mybucket/config.json) [required]: "));
        break;
      case 3:
        addGcpCloudStorageConfigUrl(
          readRequired("Public storage.googleapis.com URL "
            + "(e.g. https://storage.googleapis.com/mybucket/config.json) "
            + "[required]: "));
        break;
      case 4:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void buildGcpCloudStorageNamedValuesUrl() {
    String bucket = readRequired("Bucket name [required]: ");
    String object = readRequired("Object name [required]: ");
    String project = readOptional("Project id [optional]: ");

    StringBuilder location = new StringBuilder();
    if (project != null) {
      location.append("project=").append(project).append(";");
    }
    location.append("bucket=").append(bucket)
      .append(";object=").append(object);

    addGcpCloudStorageConfigUrl(location.toString());
  }

  private void addGcpCloudStorageConfigUrl(String location) {
    addConfigUrl(
      "jdbc:oracle:thin:@config-gcpstorage://" + location,
      new LinkedHashMap<>(),
      "GCP Cloud Storage Config Provider",
      "#gcp-cloud-storage-config-provider");
  }

  private void buildGcpSecretManagerConfigUrl() {
    addConfigUrl(
      "jdbc:oracle:thin:@config-gcpsecretmanager:"
        + readSecretVersionName(),
      new LinkedHashMap<>(),
      "GCP Secret Manager Config Provider",
      "#gcp-secret-manager-config-provider");
  }
}