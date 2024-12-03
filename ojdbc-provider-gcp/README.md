# Oracle JDBC Providers for GCP

This module contains providers for integration between Oracle JDBC and
Google Cloud Platform (GCP).

## Centralized Config Providers

<dl>
<dt><a href="#gcp-cloud-storage-config-provider">GCP Cloud Storage Config 
Provider</a></dt>
<dd>Provides connection properties managed by the Cloud Storage service</dd>
<dt><a href="#gcp-secret_manager-config-provider">GCP Secret Manager Config 
Provider</a></dt>
<dd>Provides connection properties managed by the Secret Manager service</dd>
<dt><a href="#caching-configuration">Caching configuration</a></dt>
<dd>Caching mechanism adopted by Centralized Config Providers</dd>
</dl>

## Resource Providers

<dl>
<dt><a href="#vault-password-provider">Secret Manager Password Provider</a></dt>
<dd>Provides passwords managed by the Secret Manager service</dd>
<dt><a href="#vault-username-provider">Secret Manager Username Provider</a></dt>
<dd>Provides usernames managed by the Secret Manager service</dd>
<dt><a href="#secret-manager-tcps-wallet-provider">Secret Manager TCPS Wallet Provider</a></dt>
<dd>Provides TCPS/TLS wallet for secure connections to an Autonomous Database from the Secret Manager service</dd>
<dt><a href="#secret-manager-seps-wallet-provider">Secret Manager SEPS Wallet Provider</a></dt>
<dd>Provides SEPS (Secure External Password Store) wallets for secure username and password retrieval from the Secret Manager service</dd>
<dt><a href="#secret-manager-connection-string-provider">Secret Manager Connection String Provider</a></dt>
<dd>Provides connection strings for secure database connectivity based on aliases, retrieved from the `tnsnames.ora`
file stored in GCP Secret Manager.</dd> 
</dl>

Visit any of the links above to find information and usage examples for a
particular provider.

## Installation

All providers in this module are distributed as single jar on the Maven Central
Repository. The jar is compiled for JDK 8, and is forward compatible with later
JDK versions. The coordinates for the latest release are:
```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-gcp</artifactId>
  <version>1.0.1</version>
</dependency>
```

## Authentication

Providers use Google Cloud APIs which support 
[Application Default Credentials](https://cloud.google.com/docs/authentication/application-default-credentials); 
the libraries look for credentials in a set of defined locations and use those 
credentials to authenticate requests to the API.

ADC searches for credentials in one of the following locations:

1. GOOGLE_APPLICATION_CREDENTIALS environment variable
2. User credentials set up by using the Google Cloud CLI
3. The attached service account, returned by the metadata server

When your code is running in a local development environment, such as a development workstation, the best option is to use the credentials associated with your user account.

### Configure ADC with your Google Account
To configure ADC with a Google Account, you use the Google Cloud CLI:

1. Install and initialize the gcloud CLI.

When you initialize the gcloud CLI, be sure to specify a Google Cloud project in which you have permission to access the resources your application needs.

2. Configure ADC:
```
gcloud auth application-default login
```
A sign-in screen appears. After you sign in, your credentials are stored in the local credential file used by ADC.

## GCP Cloud Storage Config Provider
The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-gcpstorage:` to be able to identify that the configuration parameters should be loaded using GCP Object Storage. Users only need to indicate the project, bucket and object containing the JSON payload, with the following syntax:

<pre>
jdbc:oracle:thin:@config-gcpstorage://project={project};bucket={bucket};object={object}]
</pre>

### JSON Payload format

There are 3 fixed values that are looked at the root level.

- connect_descriptor (required)
- user (optional)
- password (optional)

The rest are dependent on the driver, in our case `/jdbc`. The key-value pairs that are with sub-prefix `/jdbc` will be applied to a DataSource. The key values are constant keys which are equivalent to the properties defined in the [OracleConnection](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html) interface.

For example, let's suppose an url like:

<pre>
jdbc:oracle:thin:@config-gcpstorage://project=myproject;bucket=mybucket;object=payload_ojdbc_objectstorage.json
</pre>

And the JSON Payload for the file **payload_ojdbc_objectstorage.json** in the **mybucket** which belongs to the project **myproject** is as following:

```json
{
  "connect_descriptor": "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.us-phoenix-1.oraclecloud.com))(connect_data=(service_name=xsxsxs_dbtest_medium.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))",
  "user": "scott",
  "password": { 
    "type": "gcpsecretmanager",
    "value": "projects/138028249883/secrets/test-secret/versions/1"
  },
  "jdbc": {
    "oracle.jdbc.ReadTimeout": 1000,
    "defaultRowPrefetch": 20,
    "autoCommit": "false"
  }
}
```

The sample code below executes as expected with the previous configuration.

```java
    OracleDataSource ds = new OracleDataSource();
    ds.setURL("jdbc:oracle:thin:@config-gcpstorage://project=myproject;bucket=mybucket;object=payload_ojdbc_objectstorage.json");
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("select sysdate from dual");
    if (rs.next())
      System.out.println("select sysdate from dual: " + rs.getString(1));
```

### Password JSON Object

For the JSON type of provider (GCP Object Storage, HTTP/HTTPS, File) the password is an object itself with the following spec:

- type
  - Mandatory
  - Possible values
    - ocivault
    - azurevault
    - base64
    - gcpsecretmanager
- value
  - Mandatory
  - Possible values
    - OCID of the secret (if ocivault)
    - Azure Key Vault URI (if azurevault)
    - Base64 Encoded password (if base64)
    - GCP resource name (if gcpsecretmanager)
    - Text
- authentication
  - Optional
  - Possible Values
    - method
    - optional parameters (depends on the cloud provider).

## GCP Secret Manager Config Provider
Apart from GCP Cloud Storage, users can also store JSON Payload in the content of GCP Secret Manager secret. Users need to indicate the resource name:

<pre>
jdbc:oracle:thin:@config-gcpsecretmanager:{resource-name}
</pre>

The JSON Payload retrieved by GCP Vault Config Provider follows the same format in [GCP Object Storage Config Provider](#json-payload-format).

## Caching configuration

Config providers in this module store the configuration in caches to minimize
the number of RPC requests to remote location. See
[Caching configuration](../ojdbc-provider-azure/README.md#caching-configuration) for more 
details of the caching mechanism.

## Vault Password Provider
The Vault Password Provider provides Oracle JDBC with a password that is managed
by the GCP Secret Manager service. This is a Resource Provider identified by the
name `ojdbc-provider-gcp-secret-password`.

This provider requires the parameters listed below.
<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody><tr>
<td>secretVersionName</td>
<td>Identifies the secret that is provided.</td>
<td>The resource name of the secret.</td>
<td><i>No default value. A value must be configured for this parameter.</i></td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-vault.properties).

## Vault Username Provider
The Vault Username Provider provides Oracle JDBC with a username that is managed by the
GCP Secret Manager service. This is a Resource Provider identified by the name
`ojdbc-provider-gcp-secret-username`.

In addition to the set of [common parameters](#common-parameters-for-resource-providers), this provider
also supports the parameters listed below.
<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody><tr>
<td>secretVersionName</td>
<td>Identifies the secret that is provided.</td>
<td>The resource name of the secret.</td>
<td><i>No default value. A value must be configured for this parameter.</i></td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-vault.properties).

## Secret Manager TCPS Wallet Provider

The TCPS Wallet Provider provides Oracle JDBC with keys and certificates managed by the GCP Secret Manager service to establish secure TLS connections with an Autonomous Database. This is a Resource Provider identified by the name `ojdbc-provider-gcp-secretmanager-tls`.

For example, when connecting to an Autonomous Database Serverless with mutual TLS (mTLS), you need to configure the JDBC-thin driver with its client certificate. If this certificate is stored in a wallet file (e.g., `cwallet.sso`, `ewallet.p12`, `ewallet.pem`), you may store it in a GCP Secret Manager secret for additional security. You can then use this provider to retrieve the wallet content from GCP Secret Manager using the GCP SDK and pass it to the JDBC thin driver.

- The type parameter must be specified to indicate the wallet format: SSO, PKCS12, or PEM.
- The wallet password must be provided for wallets that require a password (e.g., PKCS12 or password-protected PEM files).
- This provider handles both cases where the wallet is stored as a base64-encoded string or directly as an imported file in GCP Secret Manager.

<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody>
<tr>
<td>secretVersionName</td>
<td>The name of the secret version in GCP Secret Manager that contains the TCPS wallet file.</td>
<td> The <a href="https://cloud.google.com/secret-manager/docs/creating-and-accessing-secrets">GCP Secret Manager Secret Version</a>, typically in the form: 
<pre>projects/{project-id}/secrets/{secret-id}/versions/{version-id}</pre> 
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td>walletPassword</td>
<td>
Optional password for PKCS12 or protected PEM files. If omitted, the file is assumed to be SSO or a non-protected PEM file.
</td>
<td>Any valid password for the wallet</td>
<td>
<i>No default value. PKCS12 and password-protected PEM files require a password.</i>
</td>
</tr>
<tr>
<td>type</td>
<td>
Specifies the type of the file being used.
</td>
<td>SSO, PKCS12, PEM</td>
<td>
<i>No default value. The file type must be specified.</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-secret-manager-wallet.properties](example-secret-manager-wallet.properties).

This provider automatically handles cases where the wallet is stored as either a base64-encoded string or an imported file in GCP Secret Manager. It detects the format and processes the wallet accordingly, ensuring flexibility in how users manage and retrieve their wallet data.


## Secret Manager SEPS Wallet Provider

The SEPS Wallet Provider provides Oracle JDBC with a username and password managed by the GCP Secret Manager service, stored in a Secure External Password Store (SEPS) wallet. This is a Resource Provider identified by the name `ojdbc-provider-gcp-secretmanager-seps`.

- The SEPS wallet securely stores encrypted database credentials, including the username, password, and connection strings. These credentials can be stored as default values, such as **oracle.security.client.default_username** and **oracle.security.client.default_password**, or as indexed credentials, for example, **oracle.security.client.username1**, **oracle.security.client.password1**, and **oracle.security.client.connect_string1**.

- The provider retrieves credentials based on the following logic: If `connectionStringIndex` is not specified, it first attempts to retrieve the default credentials (`oracle.security.client.default_username` and `oracle.security.client.default_password`). If default credentials are not found, it checks for a single set of credentials associated with a connection string. If exactly one connection string is found, it uses the associated credentials. However, if multiple connection strings are found, an error is thrown, prompting you to specify a `connectionStringIndex`. If `connectionStringIndex` is specified, the provider attempts to retrieve the credentials associated with the specified connection string index (e.g., **oracle.security.client.username{idx}**, **oracle.security.client.password{idx}**, **oracle.security.client.connect_string{idx}**). If credentials for the specified index are not found, an error is thrown indicating that no connection string was found with that index.

<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody>
<tr>
<td>secretVersionName</td>
<td>The name of the secret version in GCP Secret Manager that contains the SEPS wallet file.</td>
<td> The <a href="https://cloud.google.com/secret-manager/docs/creating-and-accessing-secrets">GCP Secret Manager Secret Version</a>, typically in the form: 
<pre>projects/{project-id}/secrets/{secret-id}/versions/{version-id}</pre> 
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td>walletPassword</td>
<td>
Optional password for wallets stored as PKCS12 keystores. If omitted, the wallet is assumed to be an SSO wallet.
</td>
<td>Any valid password for the SEPS wallet</td>
<td>
<i>No default value. PKCS12 wallets require a password.</i>
</td>
</tr>
<tr>
<td>connectionStringIndex</td>
<td>
Optional parameter to specify the index of the connection string to use when retrieving credentials from the wallet.
</td>
<td>A positive integer representing the index of the desired credential set (e.g., 1, 2, 3, etc.).</td>
<td>
<i>No default value. If not specified, the provider follows the default behavior as described above.</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-secret-manager-wallet.properties](example-secret-manager-wallet.properties).

This provider supports wallets stored in GCP Secret Manager as both base64-encoded strings and imported files. It automatically detects the storage format and processes the wallet accordingly, ensuring flexibility in managing your SEPS credentials.

## Secret Manager Connection String Provider

The Connection String Provider provides Oracle JDBC with a connection string managed by the GCP Secret Manager service.
This is a Resource Provider identified by the name `ojdbc-provider-gcp-secretmanager-tnsnames`.

This provider retrieves a `tnsnames.ora` file stored in GCP Secret Manager, allowing selection of connection strings
based on specified aliases. The `tnsnames.ora` file can be stored as a base64-encoded string or as a raw file,
and the provider automatically detects the format.

This enables flexible configuration for secure database connections using the alias names defined in your `tnsnames.ora` file.

<table>
  <thead>
    <tr>
      <th>Parameter Name</th>
      <th>Description</th>
      <th>Accepted Values</th>
      <th>Default Value</th>
    </tr> 
  </thead> 
  <tbody>
    <tr> 
      <td><code>secretVersionName</code></td>
      <td>The version name of the secret in GCP Secret Manager that contains the <code>tnsnames.ora</code> file.</td>
      <td>The <a href="https://cloud.google.com/secret-manager/docs/creating-and-accessing-secrets">GCP Secret Manager Secret Version</a>, typically in the form:<br><pre>projects/{project-id}/secrets/{secret-id}/versions/{version-id}</pre></td>
      <td><i>No default value. A value must be configured for this parameter.</i></td>
    </tr>
    <tr>
      <td><code>tnsAlias</code></td>
      <td>Specifies the alias to retrieve the appropriate connection string from the <code>tnsnames.ora</code> file.</td>
      <td>Any valid alias present in your <code>tnsnames.ora</code> file.</td>
      <td><i>No default value. A value must be configured for this parameter.</i></td> 
    </tr>
  </tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-vault.properties](example-vault.properties).
