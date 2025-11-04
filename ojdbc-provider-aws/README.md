# Oracle JDBC Providers for AWS

This module contains providers for integration between Oracle JDBC and
Amazon Web Services (AWS).

## Centralized Config Providers

<dl>
<dt><a href="#aws-s3-configuration-provider">AWS S3 Configuration
Provider</a></dt>
<dd>Provides connection properties managed by the S3 service</dd>
<dt><a href="#aws-secrets-manager-config-provider">AWS Secrets Manager Configuration 
Provider</a></dt>
<dd>Provides connection properties managed by the Secrets Manager service</dd>
<dt><a href="#aws-parameter-store-config-provider">AWS Parameter Store Configuration Provider</a></dt>
<dd>Provides connection properties managed by the Systems Manager Parameter Store</dd>
<dt><a href="#aws-appconfig-freeform-config-provider">AWS AppConfig Freeform Configuration Provider</a></dt>
<dd>Provides connection properties managed by the AWS AppConfig Freeform Configuration service</dd>
<dt><a href="#common-parameters-for-centralized-config-providers">Common Parameters for Centralized Config Providers</a></dt>
<dd>Common parameters supported by the config providers</dd>
<dt><a href="#caching-configuration">Caching configuration</a></dt>
<dd>Caching mechanism adopted by Centralized Config Providers</dd>
</dl>

Visit any of the links above to find information and usage examples for a
particular provider.

## Resource Providers

<dl>
<dt><a href="#aws-secrets-manager-username-provider">Secrets Manager Username Provider</a></dt>
<dd>Provides a database username from AWS Secrets Manager</dd>
<dt><a href="#aws-parameter-store-username-provider">Parameter Store Username Provider</a></dt>
<dd>Provides a database username from AWS Parameter Store</dd>
<dt><a href="#aws-secrets-manager-password-provider">Secrets Manager Password Provider</a></dt>
<dd>Provides a database password from AWS Secrets Manager</dd>
<dt><a href="#aws-parameter-store-password-provider">Parameter Store Password Provider</a></dt>
<dd>Provides a database password from AWS Parameter Store</dd>
<dt><a href="#aws-secrets-manager-connection-string-provider">Secrets Manager Connection String Provider</a></dt>
<dd>Provides connection strings from a tnsnames.ora file stored in AWS Secrets Manager</dd>
<dt><a href="#aws-parameter-store-connection-string-provider">Parameter Store Connection String Provider</a></dt>
<dd>Provides connection strings from a tnsnames.ora file stored in AWS Parameter Store</dd>
<dt><a href="#aws-secrets-manager-tcps-wallet-provider">Secrets Manager TCPS Wallet Provider</a></dt>
<dd>Provides TCPS/TLS wallet from AWS Secrets Manager</dd>
<dt><a href="#aws-secrets-manager-seps-wallet-provider">Secrets Manager SEPS Wallet Provider</a></dt>
<dd>Provides SEPS (Secure External Password Store) wallet from AWS Secrets Manager</dd>
<dt><a href="#aws-parameter-store-seps-wallet-provider">Parameter Store SEPS Wallet Provider</a></dt>
<dd>Provides SEPS (Secure External Password Store) wallet from AWS Parameter Store</dd>
<dt><a href="#common-parameters-for-resource-providers">Common Parameters for Resource Providers</a></dt>
<dd>Common parameters supported by the resource providers</dd>
</dl>

## Installation

All providers in this module are distributed as single jar on the Maven Central
Repository. The jar is compiled for JDK 8, and is forward compatible with later
JDK versions. The coordinates for the latest release are:
```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-aws</artifactId>
  <version>1.0.7</version>
</dependency>
```

## AWS S3 Configuration Provider
The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-awss3:` to be able to identify that the configuration parameters should be loaded using AWS S3.
Users only need to indicate the S3 URI of the object that contains the JSON payload.

A URL with either of the following formats is valid:
<pre>
jdbc:oracle:thin:@config-awss3://{S3-URI}
</pre>
or
<pre>
jdbc:oracle:thin:@config-aws{S3-URI}
</pre>

The {S3-URI} can be obtained from the Amazon S3 console and follows this naming convention: s3://bucket-name/file-name.

### JSON Payload format

There are 4 fixed values that are looked at the root level.

- connect_descriptor (required)
- user (optional)
- password (optional)
- wallet_location (optional)

The rest are dependent on the driver, in our case `/jdbc`. The key-value pairs that are with sub-prefix `/jdbc` will be applied to a DataSource. The key values are constant keys which are equivalent to the properties defined in the [OracleConnection](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html) interface.

For example, let's suppose an url like:

<pre>
jdbc:oracle:thin:@config-awss3://s3://mybucket/payload_ojdbc_objectstorage.json
</pre>

And the JSON Payload for the file **payload_ojdbc_objectstorage.json** in **mybucket** as following:

```json
{
  "connect_descriptor": "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.us-phoenix-1.oraclecloud.com))(connect_data=(service_name=xsxsxs_dbtest_medium.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))",
  "user": "scott",
  "password": { 
    "type": "awssecretsmanager",
    "value": "test-secret",
    "field_name": "<field-name>"  // Optional: Only needed when the secret is structured and contains multiple key-value pairs.
  },
  "wallet_location": {
    "type": "awssecretsmanager",
    "value": "wallet-secret",
    "field_name": "<field-name>" // Optional: Only needed when the secret is structured and contains multiple key-value pairs.
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
    ds.setURL("jdbc:oracle:thin:@config-awss3://s3://mybucket/payload_ojdbc_objectstorage.json");
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("select sysdate from dual");
    if (rs.next())
      System.out.println("select sysdate from dual: " + rs.getString(1));
```

### Password JSON Object

For the JSON type of provider (AWS S3, AWS Secrets Manager, AWS Parameter Store, HTTP/HTTPS, File) the password is an object itself with the following spec:

- `type`
    - Mandatory
    - Possible values
      - `ocivault` (OCI Vault)
      - `azurevault` (Azure Key Vault)
      - `base64` (Base64)
      - `awssecretsmanager` (AWS Secrets Manager)
      - `awsparameterstore` (AWS Parameter Store)
      - `hcpvaultdedicated` (HCP Vault Dedicated)
      - `gcpsecretmanager` (GCP Secret Manager)
- `value`
    - Mandatory
    - Possible values
        - OCID of the secret (if ocivault)
        - Azure Key Vault URI (if azurevault)
        - Base64 Encoded password (if base64)
        - AWS Secret name (if awssecretsmanager)
        - AWS Parameter name (if awsparameterstore)
        - Secret path (if hcpvaultdedicated)
        - Secret name (if gcpsecretmanager)
- `field_name`
  - Optional
  - Description: Specifies the key within the secret JSON object from which to extract the password value.
    If the secret JSON contains multiple key-value pairs, field_name must be provided to unambiguously select the desired secret value.
    If the secret contains only a single key-value pair and field_name is not provided, that sole value will be used.
    If the secret is provided as plain text (i.e., not structured as a JSON object), no field_name is required.
- `authentication`
    - Optional
    - Possible Values
        - method
        - optional parameters (depends on the cloud provider).

### Wallet_location JSON Object

The `oracle.net.wallet_location` connection property is not allowed in the `jdbc` object due to security reasons. Instead, users should use the `wallet_location` object to specify the wallet in the configuration.

For the JSON type of provider (AWS S3, HTTPS, File) the wallet_location is an object itself with the same spec as the [password JSON object](#password-json-object) mentioned above.

The value stored in the secret should be the Base64 representation of of a supported wallet file. This is equivalent to setting the `oracle.net.wallet_location` connection property in a regular JDBC application using the following format:

```
data:;base64,<Base64 representation of the wallet file>
```

#### Supported formats
- `cwallet.sso` (SSO wallet)
- `ewallet.pem` (PEM wallet)

If the PEM wallet is encrypted, you must also set the wallet password using the `oracle.net.wallet_password` property.
This property should be included inside the jdbc object of the JSON payload:

```
"jdbc": {
  "oracle.net.wallet_password": "<your-password>"
}
```

<i>*Note: When storing a wallet in AWS Secrets Manager, store the raw Base64-encoded wallet bytes directly. The provider will automatically detect and handle the encoding correctly.</i>

## AWS Secrets Manager Config Provider
Apart from AWS S3, users can also store JSON Payload in the content of AWS Secrets Manager secret. Users need to indicate the secret name:

<pre>
jdbc:oracle:thin:@config-awssecretsmanager://{secret-name}
</pre>

The JSON Payload retrieved by AWS Secrets Manager Provider follows the same format in [AWS S3 Configuration Provider](#json-payload-format).

## AWS Parameter Store Config Provider
Apart from AWS S3 and Secrets Manager, users can also store JSON payload in AWS Systems Manager Parameter Store. 
To use it, specify the name of the parameter:

<pre>
jdbc:oracle:thin:@config-awsparameterstore://{parameter-name}
</pre>

The JSON payload stored in the parameter should follow the same format as described in [AWS S3 Configuration Provider](#json-payload-format).

## AWS AppConfig Freeform Config Provider
The Oracle DataSource uses the prefix `jdbc:oracle:thin:@config-awsappconfig` to identify that the freeform
configuration parameters should be loaded using AWS AppConfig. Users need to specify the application identifier or name, along with the environment and configuration profile

A URL with the following format is valid:

<pre>
jdbc:oracle:thin:@config-awsappconfig://{application-identifier}[?appconfig_environment={environment-id-or-name}&appconfig_profile={profile-id-or-name}]
</pre>

All three values are required for the AppConfig Freeform Configuration Provider:

 - `{application-identifier}`: the name or ID of your AppConfig application, as defined in [AWS AppConfig](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/appconfigdata/AppConfigDataClient.html). This must be provided directly in the URL.
 - `{environment-id-or-name}`: the name or ID of the environment within the application (e.g., dev, prod). This can be provided as a URL parameter (`appconfig_environment`), a system property (`aws.appconfig.environment`), or an environment variable (`AWS_APP_CONFIG_ENVIRONMENT`).
 - `{profile-id-or-name}`: the name or ID of the configuration profile that contains your settings. This can be provided as a URL parameter (`appconfig_profile`), a system property (`aws.appconfig.profile`), or an environment variable (`AWS_APP_CONFIG_PROFILE`).

Example using all parameters in the URL:
<pre>
jdbc:oracle:thin:@config-awsappconfig://app-name?appconfig_environment=your-environment&appconfig_profile=your-profile
</pre>

Alternatively, you can set the environment and profile via system properties (`aws.appconfig.environment, aws.appconfig.profile`) or
environment variables (`AWS_APP_CONFIG_ENVIRONMENT, AWS_APP_CONFIG_PROFILE`).

## Common Parameters for Centralized Config Providers
AWS S3 Configuration Provider and AWS Secrets Manager Configuration Provider
share the same sets of parameters for authentication configuration.

### Configuring Authentication

The Centralized Config Providers in this module use the
[Default credentials provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html) to provide authorization and authentication to S3 and Secrets Manager services.
The user can provide an optional parameter `AUTHENTICATION` (case-ignored) which is mapped with the following Credential Class.

<table>
<thead><tr>
<th>'AUTHENTICATION' Param Value</th>
<th>Method</th>
<th>Optional Configuration</th>
<th>Optional Parameters</th>
</tr></thead>
<tbody>
<tr>
  <td><b>AWS_DEFAULT</b> or &lt;Empty&gt;</td>
  <td>Default Credentials Provider Chain</td>
  <td>see below Default Credentials Provider Chain</td>
  <td>AWS_REGION (see AWS Region below)</td>
</tr>
</tbody>
</table>

### Default Credentials Provider Chain

The default credentials provider chain provided by AWS SDK is implemented by the 
[DefaultCredentialsProvider](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html) class,
which searches for credentials in one of the following locations using a predefined sequence:

1. Java system properties
2. Environment variables
3. Web identity token from AWS Security Token Service
4. The shared credentials and config files
5. Amazon ECS container credentials
6. Amazon EC2 instance IAM role-provided credentials

For more details, please refer to [Default credentials provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html).

### AWS Region

In this project, region can be specified from two places: 
1. `AWS_REGION` as an optional parameter in URL
2. [Default region provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/region-selection.html#automatically-determine-the-aws-region-from-the-environment).

If `AWS_REGION` is specified in the URL, the provider uses it as the value of Region for authentication. Otherwise, the value from default region provider chain will be applied.

## AWS Secrets Manager Username Provider
The Secrets Manager Username Provider provides Oracle JDBC with a database username
that is managed by the Secrets Manager service. This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html)
identified by the name `ojdbc-provider-aws-secretsmanager-username`.

In addition to the set of [common parameters](#common-parameters-for-resource-providers),
this provider also supports the parameters listed below.
<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody><tr>
<td><code>secretName</code></td>
<td>
The name of a secret in AWS Secrets Manager.
<td>
Any valid secret name.
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>fieldName</code></td>
<td>
The field inside the secret that contains the username.
Use this parameter only when the secret is stored as a key-value pairs.
If the secret contains multiple keys, this parameter specifies which key to extract.
If the secret contains only one key and this parameter is not provided, that value is automatically used.
If <code>fieldName</code> is provided but does not match any key, or if the secret is not structured as key/value pairs, an error is thrown.
If the secret is plain text and <code>fieldName</code> is provided, an error is also thrown.
<td>
The name of the key to extract from the secret when it is stored as a set of key-value pairs
</td>
<td>
<i>Optional</i>
</td>
</tr>
</tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-aws-secretsmanager.properties).

## AWS Parameter Store Username Provider

The Parameter Store Username Provider provides Oracle JDBC with a database username stored in AWS Systems Manager Parameter Store. 
This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html) identified
by the name `ojdbc-provider-aws-parameter-store-username`.

In addition to the set of [common parameters](#common-parameters-for-resource-providers),
this provider also supports the parameters listed below.

<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody>
<tr>
<td><code>parameterName</code></td>
<td>The name of a parameter in AWS Parameter Store.</td>
<td>Any valid parameter name.</td>
<td><i>No default value. A value must be configured for this parameter.</i></td>
</tr>
</tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-aws-parameterstore.properties).

## AWS Secrets Manager Password Provider
The Secrets Manager Password Provider provides Oracle JDBC with a database password
that is managed by the AWS Secrets Manager service. This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html)
identified by the name `ojdbc-provider-aws-secretsmanager-password`.

In addition to the set of [common parameters](#common-parameters-for-resource-providers),
this provider also supports the parameters listed below.
<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody>
<tr>
<td><code>secretName</code></td>
<td>
The name of a secret in AWS Secrets Manager.
<td>
Any valid secret name.
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>fieldName</code></td>
<td>
The field inside the secret that contains the password.
Use this parameter only when the secret is structured as key-value pairs.
If the secret contains multiple keys, this parameter specifies which key to extract.
If the secret contains only one key and this parameter is not provided, that value is used automatically.
If <code>fieldName</code> is provided but does not match any key, or if the secret is not structured as key-value pairs, an error is thrown.
If the secret is stored as plain text and <code>fieldName</code> is provided, an error is also thrown.
<td>
The name of the key to extract from the secret when it is stored as a set of key-value pairs
</td>
<td>
<i>Optional</i>
</td>
</tr>
</tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-aws-secretsmanager.properties).

## AWS Parameter Store Password Provider

The Parameter Store Password Provider provides Oracle JDBC with a database password stored in AWS Systems Manager Parameter Store.
This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html) identified
by the name `ojdbc-provider-aws-parameter-store-password`.

In addition to the set of [common parameters](#common-parameters-for-resource-providers),
this provider also supports the parameters listed below.
<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody>
<tr>
<td><code>parameterName</code></td>
<td>The name of a parameter in AWS Parameter Store.</td>
<td>Any valid parameter name.</td>
<td><i>No default value. A value must be configured for this parameter.</i></td>
</tr>
</tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-aws-parameterstore.properties).

## AWS Secrets Manager TCPS Wallet Provider

The TCPS Wallet Provider provides Oracle JDBC with keys and certificates managed by the AWS Secrets Manager service
to establish secure TLS connections with an Autonomous Database. This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html) identified by the name
`ojdbc-provider-aws-secretsmanager-tls`.

For example, when connecting to Autonomous Database Serverless with mutual TLS (mTLS), you need to configure the JDBC-thin
driver with its client certificate. If this certificate is stored in a wallet file (e.g., `cwallet.sso`, `ewallet.p12`, `ewallet.pem`),
you may store the base64 encoding of that file as an AWS Secrets Manager secret for additional security.
You can then use this provider to retrieve the wallet content from AWS Secrets Manager using the AWS SDK
and provide it to the JDBC thin driver.

- The type parameter must be specified to indicate the wallet format: SSO, PKCS12, or PEM.
- The walletPassword parameter must be provided for wallets that require a password (e.g., PKCS12 or password-protected PEM files).

In addition to the set of [common parameters](#common-parameters-for-resource-providers), this provider also supports the parameters listed below.

<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>secretName</code></td>
<td>
The name of a secret in AWS Secrets Manager.
<td>
Any valid secret name.
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>walletPassword</code></td>
<td>
Optional password for PKCS12 or protected PEM files. If omitted, the file is assumed to be SSO or an non-protected PEM file.
</td>
<td>Any valid password for the wallet</td>
<td>
<i>No default value. PKCS12 and password-protected PEM files require a password.</i>
</td>
</tr>
<tr>
<td><code>type</code></td>
<td>
Specifies the type of the file being used.
</td>
<td>SSO, PKCS12, PEM</td>
<td>
<i>No default value. The file type must be specified.</i>
</td>
</tr>
<tr>
<td><code>fieldName</code></td>
<td>
The field inside the secret that contains the base64-encoded TCPS wallet.
Use this parameter only when the secret is structured as key-value pairs.
If the secret contains multiple keys, this parameter specifies which key to extract.
If the secret contains only one key and this parameter is not provided, that value is used automatically.
If <code>fieldName</code> is provided but does not match any key, or if the secret is not structured as key-value pairs, an error is thrown.
If the secret is stored as plain text and <code>fieldName</code> is provided, an error is also thrown.
<td>
The name of the key to extract from the secret when it is stored as a set of key-value pairs
</td>
<td>
<i>Optional</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-secrets-manager-wallet.properties](example-aws-secretsmanager-wallet.properties).

## AWS Secrets Manager SEPS Wallet Provider

The SEPS Wallet Provider provides Oracle JDBC with a username and password managed by the AWS Secrets Manager service,
where the base64 encoding of a Secure External Password Store (SEPS) wallet file is stored as a secret. This is a
[Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html) identified by the name `ojdbc-provider-aws-secretsmanager-seps`.

- The SEPS wallet securely stores encrypted database credentials, including the username, password, and connection strings.
  These credentials can be stored as default values, such as **oracle.security.client.default_username** and **oracle.security.client.default_password**,
  or as indexed credentials, for example, **oracle.security.client.username1**, **oracle.security.client.password1**,
  and **oracle.security.client.connect_string1**.

- The provider retrieves credentials based on the following logic: If connectionStringIndex is not specified,
  it first attempts to retrieve the default credentials (`oracle.security.client.default_username` and `oracle.security.client.default_password`).
  If default credentials are not found, it checks for a single set of credentials associated with a connection string.
  If exactly one connection string is found, it uses the associated credentials. However, if multiple connection strings
  are found, an error is thrown, prompting you to specify a `connectionStringIndex`. If `connectionStringIndex` is specified,
  the provider attempts to retrieve the credentials associated with the specified connection string index
  (e.g., **oracle.security.client.username{idx}**, **oracle.security.client.password{idx}**,
  **oracle.security.client.connect_string{idx}**). If credentials for the specified index are not found,
  an error is thrown indicating that no connection string was found with that index.

In addition to the set of [common parameters](#common-parameters-for-resource-providers), this provider also supports the parameters listed below.

<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>secretName</code></td>
<td>
The name of a secret in AWS Secrets Manager.
<td>
Any valid secret name.
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>walletPassword</code></td>
<td>
Optional password for wallets stored as PKCS12 keystores. If omitted, the wallet is assumed to be an SSO wallet.
</td>
<td>Any valid password for the SEPS wallet</td>
<td>
<i>No default value. PKCS12 wallets require a password.</i>
</td>
</tr>
<tr>
<td><code>connectionStringIndex</code></td>
<td>
Optional parameter to specify the index of the connection string to use when retrieving credentials from the wallet
</td>
<td>A positive integer representing the index of the desired credential set (e.g., 1, 2, 3, etc.). </td>
<td>
<i>No default value. If not specified, the provider follows the default behavior as described above</i>
</td>
</tr>
<tr>
<td><code>fieldName</code></td>
<td>
The field inside the secret that contains the base64-encoded SEPS wallet.
Use this parameter only when the secret is structured as key-value pairs.
If the secret contains multiple keys, this parameter specifies which key to extract.
If the secret contains only one key and this parameter is not provided, that value is used automatically.
If <code>fieldName</code> is provided but does not match any key, or if the secret is not structured as key-value pairs, an error is thrown.
If the secret is stored as plain text and <code>fieldName</code> is provided, an error is also thrown.
<td>
The name of the key to extract from the secret when it is stored as a set of key-value pairs
</td>
<td>
<i>Optional</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-secrets-manager-wallet.properties](example-aws-secretsmanager-wallet.properties.properties).

## AWS Parameter Store SEPS Wallet Provider

The SEPS Wallet Provider retrieves a SEPS wallet stored in AWS Parameter Store. 
This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html) identified
by the name `ojdbc-provider-aws-parameter-store-seps`.

This provider works identically to the [AWS Secrets Manager SEPS Wallet Provider](#aws-secrets-manager-seps-wallet-provider)
except that it uses a Parameter Store parameter instead of a Secrets Manager secret.

In addition to the set of [common parameters](#common-parameters-for-resource-providers), this provider also supports the parameters listed below.

<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>parameterName</code></td>
<td>The name of a parameter in AWS Parameter Store.</td>
<td>Any valid parameter name.</td>
<td><i>No default value. A value must be configured for this parameter.</i></td>
</tr>
<tr>
<td><code>walletPassword</code></td>
<td>Optional password for PKCS12 wallets.</td>
<td>Any valid password</td>
<td><i>None. Required if wallet is password-protected.</i></td>
</tr>
<tr>
<td><code>connectionStringIndex</code></td>
<td>Optional index to select specific credentials in SEPS wallet.</td>
<td>Any positive integer (e.g., 1, 2, 3)</td>
<td><i>None</i></td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-parameter-store-wallet.properties](example-aws-parameterstore-wallet.properties).

## AWS Secrets Manager Connection String Provider

The Connection String Provider provides Oracle JDBC with a connection string managed by the AWS Secrets Manager service.
This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html)
identified by the name `ojdbc-provider-aws-secretsmanager-tnsnames`.

This provider retrieves and decodes a `tnsnames.ora` file stored as a secret in AWS Secrets Manager.

You can store the contents of the tnsnames.ora file either as:

- A base64-encoded string, either directly or within a key inside a structured key-value map.

- Plain text, by simply copying and pasting the contents directly into the secret value.

If the secret is a key-value map, the <code>fieldName</code> parameter must be used to specify the key that holds the base64-encoded
tnsnames.ora content.

If the secret is stored as plain text, it must be provided as the raw contents of the tnsnames.ora file,
and <code>fieldName</code> should not be set.

This enables flexible configuration for secure database connections using the alias names defined in your `tnsnames.ora` file.

In addition to the set of [common parameters](#common-parameters-for-resource-providers), this provider also requires the parameters listed below.

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
    <td><code>secretName</code></td> 
    <td>The name of a secret in AWS Secrets Manager.</td> 
    <td>Any valid secret name.</td> 
    <td><i>No default value. A value must be configured for this parameter.</i></td>
  </tr>
  <tr>
    <td><code>tnsAlias</code></td>
    <td>Specifies the alias to retrieve the appropriate connection string from the <code>tnsnames.ora</code> file.</td> 
    <td>Any valid alias present in your <code>tnsnames.ora</code> file.</td>
    <td><i>No default value. A value must be configured for this parameter.</i></td> 
  </tr> 
  <tr>
    <td><code>fieldName</code></td>
    <td>
    The field inside the secret that contains the base64-encoded <code>tnsnames.ora</code> content.
    Use this parameter only when the secret is structured as key-value pairs.
    If the secret contains multiple keys, this parameter specifies which key to extract.
    If the secret contains only one key and this parameter is not provided, that value is used automatically. 
    If <code>fieldName</code> is provided but does not match any key, or if the secret is not structured
    as key-value pairs, an error is thrown. If the secret is stored as plain text and <code>fieldName</code> is provided,
    an error is also thrown.
    </td>
    <td>The name of the key to extract from the secret when it is stored as a set of key-value pairs</td>
    <td><i>Optional</i></td>
  </tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-aws-secretsmanager.properties](example-aws-secretsmanager.properties).

## AWS Parameter Store Connection String Provider

The Connection String Provider provides Oracle JDBC with a connection string managed by the AWS Systems Manager Parameter Store service.
This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html) identified
by the name `ojdbc-provider-aws-parameter-store-tnsnames`.

This provider retrieves and decodes a `tnsnames.ora` file stored as a parameter value in AWS Parameter Store.

You can store the contents of the `tnsnames.ora` file as:

- A base64-encoded string containing the full contents of the `tnsnames.ora` file.

- Plain text, by simply copying and pasting the contents directly into the parameter value.

In addition to the set of [common parameters](#common-parameters-for-resource-providers), this provider also requires the parameters listed below.

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
    <td><code>parameterName</code></td> 
    <td>The name of a parameter in AWS Systems Manager Parameter Store.</td> 
    <td>Any valid parameter name.</td> 
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

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-aws-parameterstore.properties](example-aws-parameterstore.properties).

## Common Parameters for Resource Providers

Providers classified as [Resource Providers](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OracleResourceProvider.html) within this module
all support a common set of parameters.

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
<td><code>authenticationMethod</code></td>
<td> Configures a method of <a href="#configuring-authentication">authentication with AWS</a>. <br>This setting shares the same behavior and supported values as described in the <a href="#configuring-authentication">Configuring Authentication</a> section for Centralized Config Providers. </td><td><code>aws-default</code></td>
<td><i>aws-default</i></td>
</tr>
<tr>
<td><code>awsRegion</code></td> 
<td> Configures the AWS region used to access Secrets Manager. <br>This parameter is handled identically to <a href="#aws-region">AWS Region</a> configuration described above. </td> <td> Any valid <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html">AWS region</a>, such as <code>us-west-2</code> </td>
<td><i>If not provided, will be determined from AWS SDK default region provider chain.</i></td>
</tr>
</tbody>
</table>

These parameters may be configured as connection properties recognized by the Oracle JDBC Driver.
Parameter names are recognized when appended to the name of a connection property that identifies a provider.

For example, when the connection property `oracle.jdbc.provider.password` identifies a provider,
any of the parameter names listed above may be appended to it:
```properties
oracle.jdbc.provider.password=ojdbc-provider-aws-secretsmanager-password
oracle.jdbc.provider.password.authenticationMethod=aws-default
oracle.jdbc.provider.password.awsRegion=us-west-2
oracle.jdbc.provider.password.fieldName=password
```
In the example above, the parameter names `authenticationMethod`, `awsRegion`, and `fieldName`
are appended to the property `oracle.jdbc.provider.password`, effectively configuring the Secrets Manager Password Provider.

These same parameter names can be appended to the name of any other property that identifies a provider.
For instance, a provider identified by the connection property `oracle.jdbc.provider.username` can be configured with the same parameters:
```properties
oracle.jdbc.provider.username=ojdbc-provider-aws-secretsmanager-username
oracle.jdbc.provider.username.authenticationMethod=aws-default
oracle.jdbc.provider.username.awsRegion=eu-central-1
oracle.jdbc.provider.username.fieldName=username
```
Connection properties which identify and configure a provider may appear in a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
or be configured programmatically. Configuration with JVM system properties is
not supported.

## Caching configuration

Config providers in this module store the configuration in caches to minimize
the number of RPC requests to remote location. See
[Caching configuration](../ojdbc-provider-azure/README.md#caching-configuration) for more
details of the caching mechanism.


