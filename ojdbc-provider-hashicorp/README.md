# Oracle JDBC Providers for HashiCorp Vault

This module contains providers for integration between Oracle JDBC 
and HashiCorp Vault (HCP).

## Centralized Config Providers
<dl>
<dt><a href="#hcp-vault-secrets-config-provider">HashiCorp Vault Secrets Config Provider</a></dt>
<dd>Provides connection properties managed by the Vault Secrets service</dd>
<dt><a href="#hcp-vault-dedicated-config-provider">HashiCorp Vault Dedicated Config Provider</a></dt>
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
<td>The namespace in the Vault (if applicable)</td>
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
<td>The namespace in the Vault (if applicable)</td>
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
<td>The namespace in the Vault (if applicable)</td> 
<td>No</td>
</tr> 
</tbody>
</table>

The `GITHUB` method allows authentication using a GitHub personal access token and supports
configuration of a custom path for GitHub authentication.

For more information, visit the official documentation: <a href="https://developer.hashicorp.com/vault/api-docs/auth/github">Github Authentication</a>.

### HCP Vault Secrets

Authentication for the **HCP Vault Secrets** uses the OAuth 2.0 Client Credentials flow.

The `CLIENT_ID` and `CLIENT_SECRET` are used to obtain a Bearer token for authentication,
the Bearer token is then used for making API calls to retrieve secrets from HCP Vault Secrets.
Once authenticated, the secrets can be retrieved using the HashiCorp Vault API.

The generated token is cached and reused until it expires, minimizing API calls to the HCP Vault Secrets.

Secrets can be retrieved from the following API endpoint:  
`https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/$HCP_ORG_ID/projects/$HCP_PROJECT_ID/apps/$APP_NAME/secrets`

For more information, visit the official HashiCorp Vault documentation: [HCP Vault Secrets](https://developer.hashicorp.com/hcp/tutorials/get-started-hcp-vault-secrets/hcp-vault-secrets-retrieve-secret).

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
    - Mandatory
    - Description: Specifies the key within the secret JSON object to retrieve the password value.
      For example, if the secret contains `{ "db-password": "mypassword" }`,
      setting `field_name: "db-password"` will extract `"mypassword"`.
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

