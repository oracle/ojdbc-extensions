# Oracle JDBC Providers for HashiCorp Vault

This module contains providers for integration between Oracle JDBC 
and HashiCorp Vault (HCP).

## Centralized Config Providers
<dl>
<dt><a href="#hcp-vault-dedicated-config-provider">HashiCorp Vault Dedicated Config Provider</a></dt>
<dd>Provides connection properties managed by the Dedicated Vault service</dd>
<dt><a href="#caching-configuration">Caching configuration</a></dt>
<dd>Caching mechanism adopted by Centralized Config Providers</dd>
</dl>

## Resource Providers
<dl>
<dt><a href="#dedicated-vault-username-provider">Dedicated Vault Username Provider</a></dt> 
<dd>Provides a username stored in a HashiCorp Vault Dedicated.</dd>
<dt><a href="#dedicated-vault-password-provider">Dedicated Vault Password Provider</a></dt>
<dd>Provides a password stored in a HashiCorp Vault Dedicated.</dd>
<dt><a href="#dedicated-vault-tcps-wallet-provider">Dedicated Vault TCPS Wallet Provider</a></dt> 
<dd>Provides TLS wallets from HashiCorp Vault Dedicated for secure connections.</dd>
<dt><a href="#dedicated-vault-seps-wallet-provider">Dedicated Vault SEPS Wallet Provider</a></dt>
<dd>Provides SEPS (Secure External Password Store) wallets for secure username and password retrieval from HashiCorp Vault Dedicated.</dd>
<dt><a href="#dedicated-vault-connection-string-provider">Dedicated Vault Connection String Provider</a></dt>
<dd>Provides connection strings based on aliases stored in a `tnsnames.ora` file within HashiCorp Vault Dedicated.</dd>
<dt><a href="#common-parameters-for-hcp-vault-dedicated-resource-providers">Common Parameters for HCP Vault Dedicated Resource Providers</a></dt>
<dd>Defines common configuration parameters for providers using HCP Vault Dedicated.</dd>
<dt><a href="#configuring-authentication-for-resource-providers">Configuring Authentication for Resource Providers</a></dt>
<dd>Details supported authentication methods and usage instructions.</dd>
</dl>


Visit any of the links above to find information and usage examples for a particular provider.

## Installation

All providers in this module are distributed as a single jar on the Maven Central Repository. 
The jar is compiled for JDK 8, and is forward compatible with later JDK versions. 
The coordinates for the latest release are:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-hashicorp</artifactId>
  <version>1.0.7</version>
</dependency>
```

## Authentication

Providers use the HashiCorp Vault API to retrieve secrets and configurations.
The HashiCorp Vault Providers support **HCP Vault Dedicated** with multiple authentication methods,
each requiring specific parameters.

The provider searches for these parameters in the following locations in a predefined sequence:

1. Explicitly provided in the URL
2. System properties
3. Environment variables

### HCP Vault Dedicated

Authentication for the **HCP Vault Dedicated** offers various methods to suit different use cases,
including token-based, userpass, AppRole, and GitHub authentication.
Below is an overview of these methods:

#### Token-based Authentication
Token-based authentication is the simplest way to authenticate with HashiCorp Vault.
It uses a static token provided by the Vault administrator, offering direct and straightforward access to the Vault's API.

The provider searches for the following parameters:

<table>
<thead>
<tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>VAULT_ADDR</code></td>
<td>The URL of the HashiCorp Vault instance (e.g., <code>https://vault-dedicated.example.com:8200</code>)</td>
<td>Yes</td>
</tr>
<tr>
<td><code>VAULT_TOKEN</code></td>
<td>Token provided by the Vault administrator</td>
<td>Yes</td>
</tr>
</tbody>
</table>

#### Userpass Authentication
Userpass authentication is a method that uses a combination of a username and a password to authenticate against the Vault.
It relies on the userpass authentication backend and can optionally include additional configuration parameters
such as namespaces or custom authentication paths.

The provider searches for the following parameters:

<table>
<thead>
<tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>VAULT_ADDR</code></td>
<td>The URL of the HashiCorp Vault instance (e.g., <code>https://vault-dedicated.example.com:8200</code>)</td>
<td>Yes</td>
</tr>
<tr>
<td><code>USERPASS_AUTH_PATH</code></td>
<td>The authentication path in the Vault (default: <code>userpass</code>)</td>
<td>No</td>
</tr>
<tr>
<td><code>VAULT_NAMESPACE</code></td>
<td>The namespace in the Vault (default: <code>admin</code>)</td>
<td>No</td>
</tr>
<tr>
<td><code>VAULT_USERNAME</code></td>
<td>The username for the Userpass method</td>
<td>Yes</td>
</tr>
<tr>
<td><code>VAULT_PASSWORD</code></td>
<td>The password for the Userpass method</td>
<td>Yes</td>
</tr>
</tbody>
</table>

Once authenticated, the `client_token` generated from the `Userpass` method is cached and reused until it expires.
This minimizes API calls to the Vault and enhances performance.

For more information, visit the official documentation: [Userpass Authentication](https://developer.hashicorp.com/vault/api-docs/auth/userpass).

#### AppRole Authentication
AppRole authentication is a method that relies on a `role_id` and a `secret_id` for secure authentication. It is based on the AppRole authentication backend in HashiCorp Vault,
which allows entities to authenticate and obtain a `client_token` by providing these identifiers.

The provider searches for the following parameters:

<table>
<thead>
<tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>VAULT_ADDR</code></td>
<td>The URL of the HashiCorp Vault instance (e.g., <code>https://vault-dedicated.example.com:8200</code>)</td>
<td>Yes</td>
</tr>
<tr>
<td><code>APPROLE_AUTH_PATH</code></td>
<td>The authentication path in the Vault for AppRole (default: <code>approle</code>)</td>
<td>No</td>
</tr>
<tr>
<td><code>VAULT_NAMESPACE</code></td>
<td>The namespace in the Vault (default: <code>admin</code>)</td>
<td>No</td>
</tr>
<tr>
<td><code>ROLE_ID</code></td>
<td>The role ID for the AppRole method</td>
<td>Yes</td>
</tr>
<tr>
<td><code>SECRET_ID</code></td>
<td>The secret ID for the AppRole method</td>
<td>Yes</td>
</tr>
</tbody>
</table>

Once authenticated, the <code>client_token</code> generated from the <strong>AppRole</strong> method is cached and reused until it expires. This minimizes API calls to the Vault and enhances performance.

For more information, visit the official documentation: <a href="https://developer.hashicorp.com/vault/api-docs/auth/approle">AppRole Authentication</a>.

#### GitHub Authentication
GitHub authentication is a method that uses a personal access token to authenticate against the Vault.
It relies on the github authentication backend and supports configuring a custom authentication path.

The provider searches for the following parameters:

<table>
<thead>
<tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>VAULT_ADDR</code></td>
<td>The URL of the HashiCorp Vault instance (e.g., <code>https://vault-dedicated.example.com:8200</code>)</td>
<td>Yes</td>
</tr>
<tr>
<td><code>GITHUB_TOKEN</code></td>
<td>The GitHub personal access token for authentication</td>
<td>Yes</td>
</tr>
<tr>
<td><code>GITHUB_AUTH_PATH</code></td> 
<td>The authentication path for GitHub in the Vault (default: <code>github</code>)</td> 
<td>No</td>
</tr>
<tr>
<td><code>VAULT_NAMESPACE</code></td> 
<td>The namespace in the Vault (default: <code>admin</code>)</td> 
<td>No</td>
</tr> 
</tbody>
</table>

The `GITHUB` method allows authentication using a GitHub personal access token and supports
configuration of a custom path for GitHub authentication.

For more information, visit the official documentation: <a href="https://developer.hashicorp.com/vault/api-docs/auth/github">Github Authentication</a>.

#### AUTO_DETECT Authentication

The **AUTO_DETECT** authentication method dynamically selects the most suitable authentication mechanism based on the provided parameters. This eliminates the need for users to manually specify an authentication method, ensuring a seamless and efficient authentication process.

#### Selection Order:
1. **VAULT_TOKEN** → If a pre-existing Vault token is available, it is used.
2. **USERPASS** → If `VAULT_USERNAME` and `VAULT_PASSWORD` are provided, Userpass authentication is used.
3. **APPROLE** → If `ROLE_ID` and `SECRET_ID` are available, AppRole authentication is used as a fallback.
4. **GITHUB** → If no other method is available, GitHub authentication is attempted using `GITHUB_TOKEN`.

The provider automatically detects the available parameters and chooses the best authentication method accordingly.

**Note:** If no authentication method is explicitly specified, **AUTO_DETECT is used by default.**

## Config Providers

### HCP Vault Dedicated Config Provider

The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-hcpvaultdedicated://` to be able to identify that the configuration parameters should be loaded using HCP Vault Dedicated. Users need to indicate the secret path with the following syntax:

<pre>
jdbc:oracle:thin:@config-hcpvaultdedicated://{secret-path}[?option1=value1&option2=value2...]
</pre>

The `secret-path` refers to the exact location of the secret within the HCP Vault Dedicated.

The query parameters (`option1=value1`, `option2=value2`, etc.) are optional key-value pairs that can be used to:

- Specify authentication parameters (e.g., `VAULT_ADDR`, `VAULT_TOKEN`, `VAULT_USERNAME`)
- Pass additional context to the provider (e.g., `KEY` for specifying which set of credentials to use)

 All parameters that can be specified as environment variables or system properties can also be provided directly in the URL.
 For example:
```
jdbc:oracle:thin:@config-hcpvaultdedicated:///v1/namespace/secret/data/secret_name?KEY=sales_app1&authentication=approle
```

### JSON Payload format

There are 4 fixed values that are looked at the root level:

- `connect_descriptor` (required)
- `user` (optional)
- `password` (optional)
- `wallet_location` (optional)

The rest are dependent on the driver, in our case `/jdbc`. The key-value pairs that are under the `/jdbc` prefix will be applied to a `DataSource`. These keys correspond to the properties defined in the [OracleConnection](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html) interface.

For example, let's suppose a URL like:

<pre>
jdbc:oracle:thin:@config-hcpvaultdedicated:///v1/namespace/secret/data/secret_name?KEY=sales_app1
</pre>

And the JSON Payload for the secret **test_config** stored in the HCP Vault Dedicated would look like the following:

```json
{
  "connect_descriptor": "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.us-phoenix-1.oraclecloud.com))(connect_data=(service_name=xsxsxs_dbtest_medium.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))",
  "user": "scott",
  "password": {
    "type": "hcpvaultdedicated",
    "value": "/v1/namespace/secret/data/password",
    "field_name": "db-password" // Optional: Only needed when the secret is structured and contains multiple key-value pairs.
  },
  "wallet_location": {
    "type": "hcpvaultdedicated",
    "value": "/v1/namespace/secret/data/wallet",
    "field_name": "wallet_field" // Optional: Only needed when the secret is structured and contains multiple key-value pairs.
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
    ds.setURL("jdbc:oracle:thin:@config-hcpvaultdedicated:///v1/namespace/secret/data/test_config");
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("select sysdate from dual");
    if (rs.next())
      System.out.println("select sysdate from dual: " + rs.getString(1));
```

### Password JSON Object

For the JSON type of provider (HCP Vault Dedicated, HTTP/HTTPS, File), the password is an object itself with the following spec:

- `type`
    - Mandatory
    - Possible values
      - `hcpvaultdedicated` (HCP Vault Dedicated)
      - `ocivault` (OCI Vault)
      - `azurevault` (Azure Key Vault)
      - `base64` (Base64)
      - `awssecretsmanager` (AWS Secrets Manager)
      - `gcpsecretmanager` (GCP Secret Manager)
- `value`
    - Mandatory
    - Possible values
      - Secret path (if hcpvaultdedicated)
      - OCID of the secret (if ocivault)
      - Azure Key Vault URI (if azurevault)
      - Base64 Encoded password (if base64)
      - AWS Secret name (if awssecretsmanager)
      - Secret name (if gcpsecretmanager)
- `field_name` (HCP Vault Dedicated)
    - Optional
    - Description: Specifies the key within the secret JSON object to retrieve the password value.
      For example, if the secret contains `{ "db-password": "mypassword" }`,
      setting `field_name: "db-password"` will extract `"mypassword"`. 
    - **Logic behind the `field_name` attribute:**
      - If `field_name` is **specified**, its corresponding value is extracted.
      - If the **secret contains only one key-value pair**, that value is **automatically used**.
      - If `field_name` is **missing** and **multiple keys exist**, an **error is thrown**.
- `authentication`
    - Optional
    - Possible Values
        - method
        - optional parameters (depends on the cloud provider).

### Wallet_location JSON Object

The `oracle.net.wallet_location` connection property is not allowed in the `jdbc` object due to security reasons. Instead, users should use the `wallet_location` object to specify the wallet in the configuration.

For the JSON type of provider (HCP Vault Dedicated, HTTPS, File) the `wallet_location` is an object itself with the same spec as the [password JSON object](#password-json-object) mentioned above.

The value stored in the secret should be the Base64 representation of a supported wallet file.  This is equivalent to setting the `oracle.net.wallet_location` connection property in a regular JDBC application using the following format:

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

<i>*Note: When storing a wallet in HCP Vault Dedicated, store the raw Base64-encoded wallet bytes directly. The provider will automatically detect and handle the encoding correctly.</i>

## Resource Providers

### Dedicated Vault Username Provider

The **Dedicated Vault Username Provider** provides Oracle JDBC with a **database username** that is managed by **HashiCorp Vault Dedicated**.
This is a **Resource Provider** identified by the name `ojdbc-provider-hcpvault-dedicated-username`.

In addition to the set of [common parameters](#common-parameters-for-hcp-vault-dedicated-resource-providers), this provider also supports the parameters listed below.

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
<td><code>vaultAddr</code></td>
<td>The URL of the HashiCorp Vault Dedicated instance.</td>
<td>
<i>The URL will typically have the following form:</i>
<pre>https://{vault-name}.hashicorp.cloud:8200/</pre>
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>secretPath</code></td>
<td>The path to the secret containing the username.</td>
<td>Any valid secret path.</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>fieldName</code> (Optional)</td>
<td>
The field inside the JSON secret that contains the required value.  
If the secret contains multiple keys, this parameter specifies which key to extract.  
If the secret contains only one key and this parameter is not provided, the value is automatically used.
if <code>fieldName</code> is provided but not found, an error is thrown.
If omitted and multiple keys exist, an error is thrown.
</td>
<td>Any valid field name.</td>
<td>
<i>No default value. If not specified, the provider selects the only key if a single key exists.</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-vault-dedicated.properties](example-vault-dedicated.properties).

---

### Dedicated Vault Password Provider

The **Dedicated Vault Password Provider** provides Oracle JDBC with a **database password** that is managed by **HashiCorp Vault Dedicated**.  
This is a **Resource Provider** identified by the name `ojdbc-provider-hcpvault-dedicated-password`.

In addition to the set of [common parameters](#common-parameters-for-hcp-vault-dedicated-resource-providers), this provider also supports the parameters listed below.

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
<td><code>vaultAddr</code></td>
<td>The URL of the HashiCorp Vault Dedicated instance.</td>
<td>
<i>The URL will typically have the following form:</i>
<pre>https://{vault-name}.hashicorp.cloud:8200/</pre>
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>secretPath</code></td>
<td>The path to the secret containing the password.</td>
<td>Any valid secret path.</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>fieldName</code> (Optional)</td>
<td>
The field inside the JSON secret that contains the required value.  
If the secret contains multiple keys, this parameter specifies which key to extract.  
If the secret contains only one key and this parameter is not provided, the value is automatically used.
if <code>fieldName</code> is provided but not found, an error is thrown.
If omitted and multiple keys exist, an error is thrown.
</td>
<td>Any valid field name.</td>
<td>
<i>No default value. If not specified, the provider selects the only key if a single key exists.</i>
</td>
</tr>

</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-vault-dedicated.properties](example-vault-dedicated.properties).

---

### Dedicated Vault TCPS Wallet Provider

The **Dedicated Vault TCPS Wallet Provider** provides Oracle JDBC with **keys and certificates** managed by **HashiCorp Vault Dedicated** to establish secure **TLS connections** with an Autonomous Database.  
This is a **Resource Provider** identified by the name `ojdbc-provider-hcpvault-dedicated-tls`.

For example, when connecting to an Autonomous Database Serverless with **mutual TLS (mTLS)**,  
you need to configure the JDBC-thin driver with its client certificate.  
If this certificate is stored in a wallet file (`cwallet.sso`, `ewallet.p12`, `ewallet.pem`),  
you may store it in **HCP Vault Dedicated** for additional security.  
This provider retrieves the wallet content from **HCP Vault Dedicated** and passes it to the JDBC thin driver.

- The **type** parameter must be specified to indicate the wallet format: **SSO, PKCS12, or PEM**.
- The **walletPassword** must be provided for wallets that require a password (**PKCS12** or password-protected **PEM** files).

In addition to the set of [common parameters](#common-parameters-for-hcp-vault-dedicated-resource-providers), this provider also supports the parameters listed below.

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
<td><code>vaultAddr</code></td>
<td>The URL of the HashiCorp Vault Dedicated instance.</td>
<td>
<pre>https://{vault-name}.hashicorp.cloud:8200/</pre>
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>secretPath</code></td>
<td>The path to the wallet file.</td>
<td>Any valid secret path.</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>type</code></td>
<td>The wallet format.</td>
<td><code>SSO</code>, <code>PKCS12</code>, <code>PEM</code></td>
<td>
<i>No default value. The file type must be specified.</i>
</td>
</tr>
<tr>
<td><code>walletPassword</code></td>
<td>
Optional password for **PKCS12** or protected **PEM** files.  
If omitted, the file is assumed to be **SSO** or an **unprotected PEM** file.
</td>
<td>Any valid password.</td>
<td>
<i>No default value. PKCS12 and password-protected PEM files require a password.</i>
</td>
</tr>
<tr>
<td><code>fieldName</code> (Optional)</td>
<td>
The field inside the JSON secret that contains the base64-encoded TCPS wallet.  
<br>
HashiCorp Vault Dedicated stores secrets as JSON objects.  
For TCPS wallets, this means a secret may contain multiple base64-encoded entries (e.g., <code>sso</code>, <code>pkcs12</code>, <code>pem</code>, etc.). 
<br>
If the secret contains multiple keys, this parameter specifies which key to extract.  
If the secret contains only one key and this parameter is not provided, the value is automatically used.
if <code>fieldName</code> is provided but not found, an error is thrown.
If omitted and multiple keys exist, an error is thrown.
</td>
<td>Any valid field name.</td>
<td>
<i>No default value. If not specified, the provider selects the only key if a single key exists.</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-vault-dedicated-wallet.properties](example-vault-dedicated-wallet.properties).

---

### Dedicated Vault SEPS Wallet Provider

The **Dedicated Vault SEPS Wallet Provider** provides Oracle JDBC with **username and password credentials** stored in a **Secure External Password Store (SEPS) wallet** within **HCP Vault Dedicated**.

This is a **Resource Provider** identified by the name `ojdbc-provider-hcpvault-dedicated-seps`.

- The SEPS wallet securely stores **encrypted database credentials**, including the **username, password, and connection strings**.
  These credentials can be stored as **default values**, such as:
    - `oracle.security.client.default_username`
    - `oracle.security.client.default_password`

  or as indexed credentials, for example:
    - `oracle.security.client.username1`
    - `oracle.security.client.password1`
    - `oracle.security.client.connect_string1`.

- The provider retrieves credentials using the following logic:
    1. If `connectionStringIndex` is **not specified**, the provider attempts to retrieve the **default credentials** (`oracle.security.client.default_username` and `oracle.security.client.default_password`).
    2. If **default credentials are missing**, the provider checks for a single **set of credentials** associated with a **connection string**.
    3. If **exactly one connection string** is found, the associated credentials are used.
    4. If **multiple connection strings** exist, an **error is thrown**, prompting you to specify a `connectionStringIndex`.
    5. If `connectionStringIndex` is specified, the provider attempts to retrieve the credentials associated with the **specified connection string index** (e.g., `oracle.security.client.username{idx}`, `oracle.security.client.password{idx}`, `oracle.security.client.connect_string{idx}`).
    6. If credentials for the **specified index** are not found, an **error is thrown**, indicating that no connection string exists with that index.

In addition to the set of [common parameters](#common-parameters-for-hcp-vault-dedicated-resource-providers), this provider also supports the parameters listed below.

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
<td><code>vaultAddr</code></td>
<td>The URL of the HashiCorp Vault Dedicated instance.</td>
<td>
<i>The URL will typically have the following form:</i>
<pre>https://{vault-name}.hashicorp.cloud:8200/</pre>
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>secretPath</code></td>
<td>The path to the SEPS wallet file in HashiCorp Vault Dedicated.</td>
<td>Any valid secret path.</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>walletPassword</code></td>
<td>
Optional password for **PKCS12 SEPS wallets**. If omitted, the wallet is assumed to be **SSO**.
</td>
<td>Any valid password for the SEPS wallet.</td>
<td>
<i>No default value. PKCS12 wallets require a password.</i>
</td>
</tr>
<tr>
<td><code>connectionStringIndex</code> (Optional)</td>
<td>
Specifies the **index** of the connection string to use when retrieving credentials from the wallet.
</td>
<td>A positive integer representing the index of the desired credential set (e.g., 1, 2, 3, etc.).</td>
<td>
<i>No default value. If not specified, the provider follows the default behavior as described above.</i>
</td>
</tr>
<tr>
<td><code>fieldName</code> (Optional)</td>
<td>
The field inside the JSON secret that contains the base64-encoded SEPS wallet.  
<br>
HashiCorp Vault Dedicated stores secrets as JSON objects.  
For SEPS wallets, this means a secret may contain multiple base64-encoded entries (e.g., <code>sso</code>, <code>pkcs12</code>, etc.). 
<br>
If the secret contains multiple keys, this parameter specifies which key to extract.  
If the secret contains only one key and this parameter is not provided, the value is automatically used.
if <code>fieldName</code> is provided but not found, an error is thrown.
If omitted and multiple keys exist, an error is thrown.
</td>
<td>Any valid field name.</td>
<td>
<i>No default value. If not specified, the provider selects the only key if a single key exists.</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-vault-dedicated-wallet.properties](example-vault-dedicated-wallet.properties).

---

### Dedicated Vault Connection String Provider

The **Dedicated Vault Connection String Provider** provides Oracle JDBC with a **connection string**  
retrieved from a `tnsnames.ora` file stored in **HCP Vault Dedicated**.

This is a **Resource Provider** identified by the name `ojdbc-provider-hcpvault-dedicated-tnsnames`.

This provider retrieves and decodes a `tnsnames.ora` file stored as a **base64-encoded secret** or **plain text** in **HCP Vault Dedicated**, allowing selection of connection strings based on specified aliases.

This enables flexible configuration for **secure database connections** using the alias names defined in your `tnsnames.ora` file.

In addition to the set of [common parameters](#common-parameters-for-hcp-vault-dedicated-resource-providers), this provider also requires the parameters listed below.

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
<td><code>vaultAddr</code></td>
<td>The URL of the HashiCorp Vault Dedicated instance.</td>
<td>
<pre>https://{vault-name}.hashicorp.cloud:8200/</pre>
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>secretPath</code></td>
<td>The path to the `tnsnames.ora` file.</td>
<td>Any valid secret path.</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>tnsAlias</code></td>
<td>The alias to retrieve the connection string.</td>
<td>Any valid alias present in the `tnsnames.ora` file.</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td><code>fieldName</code> (Optional)</td>
<td>
The field inside the JSON secret that contains the <code>tnsnames.ora</code> content, either as plain text or base64-encoded.  
If the secret contains multiple keys, this parameter specifies which key to extract.  
If the secret contains only one key and this parameter is not provided, the value is automatically used.
if <code>fieldName</code> is provided but not found, an error is thrown.
If omitted and multiple keys exist, an error is thrown.
</td>
<td>Any valid field name.</td>
<td>
<i>No default value. If not specified, the provider selects the only key if a single key exists.</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-vault-dedicated.properties](example-vault-dedicated.properties).

### Common Parameters for HCP Vault Dedicated Resource Providers

Providers classified as Resource Providers in this module all support a common set of parameters.

<table>
<thead>
<tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>System Property / Environment Variable</th>
<th>Default Value</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>authenticationMethod</code></td>
<td>Configures a method of <a href="#hcp-vault-dedicated">authentication for HCP Vault Dedicated</a>.</td>
<td>Accepted values are defined in: <a href="#hcp-vault-dedicated">Configuring Authentication for HCP Vault Dedicated</a>.</td>
<td>Not supported</td>
<td><i>auto-detect</i></td>
</tr>
<tr>
<td><code>vaultAddr</code></td> 
<td>The URL of the HashiCorp Vault Dedicated instance.</td>
<td>A valid Vault endpoint, typically in the form: <pre>https://vault.example.com:8200</pre></td> 
<td><code>VAULT_ADDR</code></td> 
<td><i>No default value.</i></td> 
</tr>
<tr>
<td><code>vaultNamespace</code></td> 
<td>The namespace in the Vault (only required if using namespaces).</td>
<td>Any valid Vault namespace.</td> 
<td><code>VAULT_NAMESPACE</code></td>
<td><i>admin</i> (if not explicitly set)</td>
</tr> 
<tr>
<td><code>vaultToken</code></td>
<td>Configures authentication using a Vault token.</td>
<td>A valid Vault authentication token.</td>
<td><code>VAULT_TOKEN</code></td>
<td><i>No default value.</i></td>
</tr>
<tr>
<td><code>vaultUsername</code></td>
<td>Configures authentication using the Userpass method.</td>
<td>A valid Vault username.</td> <td><code>VAULT_USERNAME</code></td>
<td><i>No default value.</i></td> 
</tr> 
<tr>
<td><code>vaultPassword</code></td>
<td>The password for Userpass authentication.</td>
<td>A valid Vault password.</td>
<td><code>VAULT_PASSWORD</code></td>
<td><i>No default value.</i></td> 
</tr>
<tr>
<td><code>roleId</code></td>
<td>Configures authentication using the AppRole method.</td>
<td>A valid Role ID for the AppRole authentication method.</td>
<td><code>ROLE_ID</code></td>
<td><i>No default value.</i></td>
</tr>
<tr>
<td><code>secretId</code></td>
<td>The secret ID for AppRole authentication.</td> 
<td>A valid Secret ID for the AppRole authentication method.</td>
<td><code>SECRET_ID</code></td>
<td><i>No default value.</i></td>
</tr>
<tr>
<td><code>githubToken</code></td>
<td>Configures authentication using a GitHub token.</td>
<td>A valid GitHub personal access token.</td> 
<td><code>GITHUB_TOKEN</code></td> 
<td><i>No default value.</i></td>
</tr> 
<tr> 
<td><code>userPassAuthPath</code></td>
<td>The authentication path in the Vault for Userpass authentication.</td>
<td>Any valid Vault authentication path.</td>
<td><code>USERPASS_AUTH_PATH</code></td>
<td><i>userpass</i> (if not explicitly set)</td>
</tr>
<tr>
<td><code>appRoleAuthPath</code></td>
<td>The authentication path in the Vault for AppRole authentication.</td>
<td>Any valid Vault authentication path.</td>
<td><code>APPROLE_AUTH_PATH</code></td> 
<td><i>approle</i> (if not explicitly set)</td>
</tr>
<tr>
<td><code>githubAuthPath</code></td>
<td>The authentication path in the Vault for GitHub authentication.</td>
<td>Any valid Vault authentication path.</td> <td><code>GITHUB_AUTH_PATH</code></td>
<td><i>github</i> (if not explicitly set)</td> 
</tr>
</tbody>
</table>

---

### Configuring Authentication for Resource Providers

#### HCP Vault Dedicated

Resource Providers in this module must authenticate with **HashiCorp Vault Dedicated**.
By default, the provider will automatically detect any available credentials.
A specific authentication method may be configured using the "authenticationMethod" parameter.

Supported values for `authenticationMethod`:

- **`vault-token`**  
  Authenticate using a pre-existing Vault token.

- **`userpass`**  
  Authenticate using a username and password.

- **`approle`**  
  Authenticate using AppRole credentials (requires `roleId` and `secretId`).

- **`github`**  
  Authenticate using a GitHub personal access token.

- **`auto-detect`** (default)  
  Automatically selects the method based on the following priority:
    1. `vault-token`
    2. `userpass`
    3. `approle`
    4. `github`

## Caching configuration

Config providers in this module store the configuration in caches to minimize
the number of RPC requests to remote location. See
[Caching configuration](../ojdbc-provider-azure/README.md#caching-configuration) for more
details of the caching mechanism.

