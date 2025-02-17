# Oracle JDBC Providers for HashiCorp Vault

This module contains providers for integration between Oracle JDBC 
and HashiCorp Vault (HCP).

## Centralized Config Providers
<dl>
<dt><a href="#hcp-vault-secrets-config-provider">HashiCorp Vault Dedicated Config Provider</a></dt>
<dd>Provides connection properties managed by the Vault Secrets service</dd>
<dt><a href="#hcp-vault-dedicated-config-provider">HashiCorp Vault Secret Config Provider</a></dt>
<dd>Provides connection properties managed by the Dedicated Vault service</dd>
<dt><a href="#caching-configuration">Caching configuration</a></dt>
<dd>Caching mechanism adopted by Centralized Config Providers</dd>
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
  <version>1.0.3</version>
</dependency>
```

## Authentication

Providers use the HashiCorp Vault API to retrieve secrets and configurations.
The HashiCorp Vault Providers support two types of Vaults: **HCP Vault Dedicated** and **HCP Vault Secrets**.
Each type has its own authentication method uses specific parameters. Choose the method that matches the type of Vault you are using.
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
<td>The namespace in the Vault (default: <code>(default: `admin`)</code>)</td>
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
AppRole authentication is a method that relies on a role_id and a secret_id for secure authentication. It is based on the AppRole authentication backend in HashiCorp Vault,
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
<td>The namespace in the Vault (default: <code>(default: `admin`)</code>)</td>
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
<td>The namespace in the Vault (default: <code>(default: `admin`)</code>)</td> 
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


### HCP Vault Secrets

Authentication for **HCP Vault Secrets** supports multiple methods:

Below is an overview of the supported authentication methods:

1. **OAuth 2.0 Client Credentials Flow**
    - Uses `HCP_CLIENT_ID` and `HCP_CLIENT_SECRET` to obtain a Bearer token for authentication.
    - The token is then used to retrieve secrets from HCP Vault Secrets API.

2. **Credentials File Authentication**
    - Uses a JSON file (`creds-cache.json`) containing authentication credentials (`access_token`, `refresh_token`, and `access_token_expiry`).    - If the access token is expired, it is automatically refreshed using the stored refresh token.
    - If the access token is expired, it is **automatically refreshed** using the stored refresh token.
    - This method allows authentication **without requiring direct API credentials**.

The generated token is cached and reused until it expires, minimizing API calls to HCP Vault Secrets.

Secrets can be retrieved from the following API endpoint:  
`https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/$HCP_ORG_ID/projects/$HCP_PROJECT_ID/apps/$APP_NAME/secrets`

For more information, visit the official HashiCorp Vault documentation: [HCP Vault Secrets](https://developer.hashicorp.com/hcp/tutorials/get-started-hcp-vault-secrets/hcp-vault-secrets-retrieve-secret).

#### OAuth 2.0 Client Credentials Flow

This method uses OAuth 2.0 **client credentials** to obtain a **Bearer token**, which is required for authentication.
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
<td><code>HCP_CLIENT_ID</code></td>
<td>The client ID for OAuth 2.0 authentication</td>
<td>Yes</td>
</tr>
<tr>
<td><code>HCP_CLIENT_SECRET</code></td>
<td>The client secret for OAuth 2.0 authentication</td>
<td>Yes</td>
</tr>
</tbody>
</table>

#### CLI CREDENTIALS FILE
This method **retrieves authentication details** from a **JSON file (`creds-cache.json`)** that contains access tokens.

- If **HCP CLI is installed**, a **creds-cache.json** file is **automatically created** in: <code>~/.config/hcp/creds-cache.json</code>
- This file contains **access_token, refresh_token, and access_token_expiry**.
- If **the token is expired**, it is **automatically refreshed** using the **refresh_token**.
- The credentials file should be a JSON file containing the following structure:

```json
{
  "login": {
    "access_token": "YOUR_ACCESS_TOKEN",
    "refresh_token": "YOUR_REFRESH_TOKEN",
    "access_token_expiry": "2025-01-01T12:34:56.789Z"
  }
}
```
- access_token: The current access token for API authentication.
- refresh_token: The refresh token used to obtain a new access token when expired.
- access_token_expiry: The expiration timestamp of the access_token.

When using this method, the provider will:
   * Read the file and validate the access_token.
   * Refresh the token if it's expired, using the refresh_token.
   * Update the file with the new token details.

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
<td><code>HCP_CREDENTIALS_FILE</code></td>
<td>The path of the credentials file ( by default
<code>~/.config/hcp/creds-cache.json</code></td>
<td>No</td>
</tr>
</tbody>
</table>

#### AUTO_DETECT Authentication

The **AUTO_DETECT** authentication method dynamically selects the most suitable authentication mechanism based on the provided parameters.
This eliminates the need for users to manually specify an authentication method, ensuring a seamless and efficient authentication process.

#### Selection Order:
1. **CLI_CREDENTIALS_FILE** → If `HCP_CREDENTIALS_FILE` is provided or the default credentials file (`~/.config/hcp/creds-cache.json`) exists, it is used.
2. **CLIENT_CREDENTIALS** → If `HCP_CLIENT_ID` and `HCP_CLIENT_SECRET` are available, Client Credentials authentication is used as a fallback.

The provider automatically detects the available parameters and chooses the best authentication method accordingly.

**Note:** If no authentication method is explicitly specified, **AUTO_DETECT is used by default.**

#### Common Parameters for HCP Vault Secrets authentication methods

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
<td><code>HCP_ORG_ID</code></td>
<td>The organization ID associated with the Vault</td>
<td>Yes</td>
</tr>
<tr>
<td><code>HCP_PROJECT_ID</code></td>
<td>The project ID associated with the Vault</td>
<td>Yes</td>
</tr>
<tr>
<td><code>HCP_APP_NAME</code></td>
<td>The application name in HCP Vault Secrets</td>
<td>Yes</td>
</tr>
</tbody>
</table>


## Config Providers

### HCP Vault Dedicated Config Provider

The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-hcpdedicatedvault://` to be able to identify that the configuration parameters should be loaded using HCP Vault Dedicated. Users need to indicate the secret path with the following syntax:

<pre>
jdbc:oracle:thin:@config-hcpvaultdedicated://{secret-path}[?option1=value1&option2=value2...]
</pre>

The `secret-path` refers to the exact location of the secret within the HCP Vault Dedicated. 

### HCP Vault Secrets Config Provider

The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-hcpvaultsecret://` to identify that the configuration parameters should be loaded using HCP Vault Secrets. Users need to indicate the secret name (`SECRET_NAME`) with the following syntax:

<pre>
jdbc:oracle:thin:@config-hcpvaultsecret://{secret-name}[?option1=value1&option2=value2...]
</pre>

The `secret-name` refers to the name of the secret to retrieve from HCP Vault Secrets

### JSON Payload format

There are 3 fixed values that are looked at the root level:

- `connect_descriptor` (required)
- `user` (optional)
- `password` (optional)

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
    "field_name": "db-password"
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

For **HCP Vault Secrets**
For example, let's suppose a URL like:

<pre> jdbc:oracle:thin:@config-hcpvaultsecret://secret-name </pre>
And the JSON Payload for a secret stored within the application app_name in the HCP Vault Secrets would look like the following:

```json
{
  "connect_descriptor": "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.us-phoenix-1.oraclecloud.com))(connect_data=(service_name=xsxsxs_dbtest_medium.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))",
  "user": "scott",
  "password": {
    "type": "hcpvaultsecret",
    "value": "secret-name"
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
    ds.setURL("jdbc:oracle:thin:@config-hcpvaultsecret://secret-name");
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("select sysdate from dual");
    if (rs.next())
      System.out.println("select sysdate from dual: " + rs.getString(1));
```

### Password JSON Object

For the JSON type of provider (HCP Vault Dedicated, HCP Vault Secrets, HTTP/HTTPS, File), the password is an object itself with the following spec:

- type
    - Mandatory
    - Possible values
        - ocivault
        - azurevault
        - base64
        - gcpsecretmanager
        - hcpvaultdedicated
        - hcpvaultsecret
- value
    - Mandatory
    - Possible values
        - OCID of the secret (if ocivault)
        - Azure Key Vault URI (if azurevault)
        - Base64 Encoded password (if base64)
        - GCP resource name (if gcpsecretmanager)
        - Secret path (if hcpvaultdedicated)
        - Secret name (if hcpvaultsecret)
        - Text
- field_name (HCP Vault Dedicated only)
    - Optional
    - Description: Specifies the key within the secret JSON object to retrieve the password value.
      For example, if the secret contains `{ "db-password": "mypassword" }`,
      setting `field_name: "db-password"` will extract `"mypassword"`. 
    - **Logic behind the `field_name` attribute:**
      - If `field_name` is **specified**, its corresponding value is extracted.
      - If the **secret contains only one key-value pair**, that value is **automatically used**.
      - If `field_name` is **missing** and **multiple keys exist**, an **error is thrown**.
- authentication
    - Optional
    - Possible Values
        - method
        - optional parameters (depends on the cloud provider).

## Caching configuration

Config providers in this module store the configuration in caches to minimize
the number of RPC requests to remote location. See
[Caching configuration](../ojdbc-provider-azure/README.md#caching-configuration) for more
details of the caching mechanism.

