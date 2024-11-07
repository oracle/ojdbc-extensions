# Oracle JDBC Providers for Azure

This module contains providers for integration between Oracle JDBC and Azure.

## Centralized Config Providers

<dl>
<dt><a href="#azure-app-configuration-provider">Azure App Configuration Provider</a></dt>
<dd>Provides connection properties managed by the App Configuration service</dd>
<dt><a href="#azure-vault-config-provider">Azure Vault Config Provider</a></dt>
<dd>Provides connection properties managed by the Key Vault service</dd>
<dt><a href="#common-parameters-for-centralized-config-providers">Common Parameters for Centralized Config Providers</a></dt>
<dd>Common parameters supported by the config providers</dd>
<dt><a href="#caching-configuration">Caching configuration</a></dt>
<dd>Caching mechanism adopted by Centralized Config Providers</dd>
</dl>

## Resource Providers

<dl>
<dt><a href="#access-token-provider">Access Token Provider</a></dt>
<dd>Provides access tokens issued by the Active Directory service</dd>
<dt><a href="#key-vault-username-provider">Key Vault Username Provider</a></dt>
<dd>Provides a username from the Key Vault service</dd>
<dt><a href="#key-vault-password-provider">Key Vault Password Provider</a></dt>
<dd>Provides a password from the Key Vault service</dd>
<dt><a href="#key-vault-tcps-wallet-provider">Key Vault TCPS Wallet Provider</a></dt>
<dd>Provides TCPS/TLS wallet for secure connections to an Autonomous Database from the Key Vault service</dd>
<dt><a href="#key-vault-seps-wallet-provider">Key Vault SEPS Wallet Provider</a></dt>
<dd>Provides SEPS (Secure External Password Store) wallets for secure username and password retrieval from the Key Vault service</dd> 
<dt><a href="#key-vault-connection-string-provider">Key Vault Connection String Provider</a></dt>
<dd>Provides connection strings for secure database connectivity based on consumer groups, retrieved from the tnsnames.ora file stored in Azure Key Vault.</dd> 
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
  <artifactId>ojdbc-provider-azure</artifactId>
  <version>1.0.1</version>
</dependency>
```

## Azure App Configuration Provider

The Config Provider for Azure is a Centralized Config Provider that provides Oracle JDBC with
connection properties from the App Configuration service and the Key Vault service.

A new prefix of the JDBC URL `jdbc:oracle:thin:@config-azure://` is used by the Oracle DataSource to be able to identify that the configuration parameters should be loaded using Azure App Configuration. Users only need to indicate the App Config's name, a prefix for the key-names and a label (both optional) with the following syntax:

<pre>
jdbc:oracle:thin:@config-azure://{appconfig-name}[?key=prefix&label=value&option1=value1&option2=value2...]
</pre>

If prefix and label are not informed, the provider will retrieve all the values that are not labeled or prefixed.

There are 3 fixed values that are looked at by the provider in the retrieved configuration:

- connect_descriptor (required)
- user (optional)
- password (optional)

The rest are dependent on the driver, in our case `/jdbc`. The key-value pairs that are with sub-prefix `/jdbc` will be applied to a DataSource. The key values are constant keys which are equivalent to the properties defined in the [OracleConnection](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html) interface.

For example, let's suppose an url like:

<pre>
jdbc:oracle:thin:@config-azure://myappconfig?key=/sales_app1/&label=dev
</pre>

And the configuration in App Configuration '**myappconfig**' as follows (note that some values such as password can be a reference to a Key Vault secret):

| Key                                       | Value                                                                                                                                                                    | Label |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----- |
| /sales_app1/user                          | scott                                                                                                                                                                    | dev   |
| /sales_app1/connect_descriptor            | (description=(address=(protocol=tcps)(port=1521)(host=adb.us-phoenix-1.oraclecloud.com))(connect_data=(service_name=gebqqvpozhjbqbs_dbtest_medium.adb.oraclecloud.com))) | dev   |
| /sales_app1/password                      | {"uri":"myvault.vault.azure.net/secrets/mysecret"}                                                                                                                       | dev   |
| /sales_app1/jdbc/autoCommit               | false                                                                                                                                                                    | dev   |
| /sales_app1/jdbc/oracle.jdbc.fanEnabled   | true                                                                                                                                                                     | dev   |
| /sales_app1/jdbc/oracle.jdbc.loginTimeout | 20                                                                                                                                                                       | dev   |

In this case the OracleDataSource that gets generated uses the above values as its properties.

The sample code below executes as expected with the previous configuration (and the Azure Credentials set as explained below).

```java
    OracleDataSource ds = new OracleDataSource();
    ds.setURL("jdbc:oracle:thin:@config-azure://myappconfig?key=/sales_app1/&label=dev");
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("select sysdate from dual");
    if (rs.next())
      System.out.println("select sysdate from dual: " + rs.getString(1));
```
## Azure Vault Config Provider
Similar to [OCI Vault Config Provider](../ojdbc-provider-oci/README.md#oci-vault-config-provider), JSON Payload can also be stored in the content of Azure Key Vault Secret.
The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-azurevault://`. Users only need to indicate the Vault Secret’s secret identifier, with the following syntax:

<pre>
jdbc:oracle:thin:@config-azurevault://{secret-identifier}
</pre>

To view an example format of JSON Payload, please refer to [JSON Payload format](../ojdbc-provider-oci/README.md#json-payload-format). 



## Common Parameters for Centralized Config Providers
Provider that are classified as Centralized Config Providers in this module share the same sets of parameters for authentication configuration.

### Configuring Authentication

This provider relies upon the [Azure SDK Credential Classes](https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/identity/azure-identity#credential-classes) to provide authorization and authentication to the App Configuration and Key Vault services. The parameters that the SDK retrieves through environment variables are retrieved the same way and the attributes that are exposed through the API are exposed in the DataSource URL as option parameters.

The user can provide an optional parameter `AUTHENTICATION` (case-ignored) which is mapped with the following Credential Class.

<table>
<thead><tr>
<th>'AUTHENTICATION' Param Value</th>
<th>Credential Class</th>
<th>Required Parameters<br>(if not already set as an environment variable)</th>
<th>Optional Parameters</th>
</tr></thead>
<tbody>
<tr>
  <td rowspan="2"><b>AZURE_DEFAULT</b> or &lt;Empty&gt;</td>
  <td rowspan="2">DefaultAzureCredential</td>
  <td rowspan="2">see below DefaultAzureCredential</td>
  <td>AZURE_TENANT_ID</td></tr>
  <tr><td>AZURE_MANAGED_IDENTITY_CLIENT_ID</td>
</tr>
<tr>
  <td rowspan="3"><b>AZURE_SERVICE_PRINCIPAL</b> (with secret: will be picked if <b>AZURE_CLIENT_SECRET</b> is set). <br>
  It has priority over service principal with certificate.</td>
  <td rowspan="3">ClientSecretCredential</td>
  <td><b>AZURE_CLIENT_ID<b/></td>
  <td rowspan="3">&nbsp;</td></tr>
  <tr><td><b>AZURE_CLIENT_SECRET<b/></td></tr>
  <tr><td><b>AZURE_TENANT_ID<b/></td>
</tr>
<tr>
  <td rowspan="3"><b>AZURE_SERVICE_PRINCIPAL</b> (with certificate: will be picked if <b>AZURE_CLIENT_CERTIFICATE_PATH</b> is set).</td>
  <td rowspan="3">ClientCertificateCredential</td>
  <td><b>AZURE_CLIENT_ID<b/></td>
  <td rowspan="3"><b>AZURE_CLIENT_CERTIFICATE_PASSWORD</b></td></tr>
  <tr><td><b>AZURE_CLIENT_CERTIFICATE_PATH<b/></td></tr>
  <tr><td><b>AZURE_TENANT_ID<b/></td>
</tr>
<tr>
  <td><b>AZURE_MANAGED_IDENTITY</b></td>
  <td>ManagedIdentityCredential</td>
  <td>&nbsp;</td>
  <td><b>AZURE_CLIENT_ID</b> (only required for user assigned)</td>
</tr>
<tr>
  <td rowspan="2"><b>AZURE_INTERACTIVE</b></td>
  <td rowspan="2">InteractiveBrowserCredential</td>
  <td><b>AZURE_CLIENT_ID</b></td>
  <td><b>AZURE_REDIRECT_URL</b></td>
</tr>
</tbody>
</table>

### DefaultAzureCredential

The Azure SDK `DefaultAzureCredential` class tries the following flow in order to try to Authenticate an application:

- [EnvironmentCredential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.environmentcredential?view=azure-dotnet)
- [ManagedIdentityCredential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.managedidentitycredential?view=azure-dotnet)
- [SharedTokenCacheCredential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.sharedtokencachecredential?view=azure-dotnet)
- [VisualStudioCredential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.visualstudiocredential?view=azure-dotnet)
- [VisualStudioCodeCredential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.visualstudiocodecredential?view=azure-dotnet)
- [AzureCliCredential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.azureclicredential?view=azure-dotnet)
- [AzurePowerShellCredential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.azurepowershellcredential?view=azure-dotnet)
- [InteractiveBrowserCredential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.interactivebrowsercredential?view=azure-dotnet)

## Caching configuration

Config providers in this module store the configuration in caches to minimize
the number of RPC requests to remote location. Every stored items has a property
that defines the time-to-live (TTL) value. When TTL expires, the configuration
becomes "softly expired" and the stored configuration will be refreshed by a
background thread. If configuration cannot be refreshed, it can still be used 
for another 30 minutes until it becomes "hardly expired". In other words, it takes
24 hours and 30 minutes for configuration before it becomes completely expired. 

The default value of TTL is 24 hours and it can be configured using the
"config_time_to_live" property in the unit of seconds.
An example of App Configuration in Azure with TTL of 60 seconds is listed below.

<table>
<thead><tr>
<th>Key</th>
<th>Value</th>
</tr></thead>
<tbody><tr>
<td>user</td>
<td>myUsername</td>
</tr><tr>
<td>password</td>
<td>myPassword</td>
</tr><tr>
<td>connect_descriptor</td>
<td>myHost:5521/myService</td>
</tr><tr>
<td>config_time_to_live</td>
<td>60</td>
</tr></tbody>
</table>

## Access Token Provider
The Access Token Provider provides Oracle JDBC with an access token that authorizes 
logins to an Autonomous Database. This is a Resource Provider 
identified by the name `ojdbc-provider-azure-token`.

This provider must be configured to 
<a href="#configuring-authentication-for-resource-providers">authenticate</a> as
an Active Directory application that has been mapped to a database user. 
Instructions  can be found in the
<a href="https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/manage-users-azure-ad.html#GUID-562655CA-4D8B-41D2-9165-6515BC824E07">
ADB product documentation.
</a>
#### Caching Mechanism
The `AccessTokenFactory` employs a caching mechanism to efficiently manage and reuse access tokens. By utilizing Oracle JDBC's cache for JWTs, 
access tokens are cached and updated one minute before they expire, ensuring no blocking of threads. This cache reduces latency when creating 
JDBC connections, as a thread opening a connection does not have to wait for a new token to be requested. You can check this in more detail 
at [Oracle's documentation](https://docs.oracle.com/en/database/oracle/oracle-database/19/jajdb/oracle/jdbc/AccessToken.html#createJsonWebTokenCache_java_util_function_Supplier_).

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
<td>scope</td>
<td>
Specifies the scope of databases that may be accessed with the token. See
<a href="#configuring-a-scope">Configuring a Scope</a> for details.
<td>
<i>A URI of the following form is accepted:</i><br>
<i>application-id-uri</i>/<i>scope-name</i>
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-token.properties](example-token.properties).

### Configuring a Scope
The "scope" parameter must be configured as the Application ID URI of a database
that has been registered with Active Directory. The path segment of the URI
may include the name of a scope that is recognized by the database application.
In the example below, a scope named "session:scope:connect" is appended to the
path of the Application ID URI
"https://example.com/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/":
<pre>
https://example.com/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/session:scope:connect
</pre>
To use the default scope, append ".default" to path instead:
<pre>
https://example.com/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/.default
</pre>

## Key Vault Username Provider
The Key Vault Username Provider provides Oracle JDBC with a database username
that is managed by the Key Vault service. This is a Resource Provider
identified by the name `ojdbc-provider-azure-key-vault-username`.

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
<td>vaultUrl</td>
<td>
The URI of a Key Vault.
<td>
<i>The URI will typically have the following form:</i>
<pre>
https://{vault-name}.vault.azure.net/
</pre>
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr><tr>
<td>secretName</td>
<td>
The name of a Key Vault Secret.
<td>
Any valid secret name is accepted.
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-key-vault.properties).

## Key Vault Password Provider
The Key Vault Password Provider provides Oracle JDBC with a database password
that is managed by the Key Vault service. This is a Resource Provider
identified by the name `ojdbc-provider-azure-key-vault-password`.

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
<td>vaultUrl</td>
<td>
The URI of a Key Vault.
<td>
<i>The URI will typically have the following form:</i>
<pre>
https://{vault-name}.vault.azure.net/
</pre>
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr><tr>
<td>secretName</td>
<td>
The name of a Key Vault Secret.
<td>
Any valid secret name is accepted.
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-key-vault.properties).

## Key Vault TCPS Wallet Provider

The TCPS Wallet Provider provides Oracle JDBC with keys and certificates managed by the Azure Key Vault service
to establish secure TLS connections with an Autonomous Database. This is a Resource Provider identified by the name
`ojdbc-provider-azure-key-vault-tl`.

For example, when connecting to Autonomous Database Serverless with mutual TLS (mTLS), you need to configure the JDBC-thin
driver with its client certificate. If this certificate is stored in a wallet file (e.g., `cwallet.sso`, `ewallet.p12`, `ewallet.pem`),
ou may store it in an Azure Key Vault secret for additional security.
You can then use this provider that will retrieve the wallet content from Azure Key Vault using the Azure SDK
and pass it to the JDBC thin driver.

- The type parameter must be specified to indicate the wallet format: SSO, PKCS12, or PEM.
- The walletpassword must be provided for wallets that require a password (e.g., PKCS12 or password-protected PEM files).

In addition to the set of [common parameters](#common-parameters-for-resource-providers), this provider also supports the parameters listed below.

<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead>
<tbody>
<tr>
<td>vaultUrl</td>
<td>The URL of the Azure Key Vault containing the TCPS file.</td>
<td> The <a href="https://docs.microsoft.com/en-us/azure/key-vault/general/overview">Azure Key Vault URL</a>, typically in the form: 
<pre>https://{vault-name}.vault.azure.net/</pre> 
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td>secretName</td>
<td>The name of the secret containing the TCPS wallet file in Azure Key Vault.</td>
<td>Any valid secret name</td>
<td> 
<i>No default value. A value must be configured for this parameter.</i> 
</td>
</tr>
<tr>
<td>walletPassword</td>
<td>
Optional password for PKCS12 or protected PEM files. If omitted, the file is assumed to be SSO or an non-protected PEM file.
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

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-key-vault-wallet.properties](example-key-vault-wallet.properties).

## Key Vault SEPS Wallet Provider

The SEPS Wallet Provider provides Oracle JDBC with a username and password managed by the Azure Key Vault service,
stored in a Secure External Password Store (SEPS) wallet. This is a Resource Provider identified by the name
`ojdbc-provider-azure-key-vault-seps`.

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
</tr></thead>
<tbody>
<tr>
<td>vaultUrl</td>
<td>The URL of the Azure Key Vault containing the SEPS wallet.</td>
<td> The <a href="https://docs.microsoft.com/en-us/azure/key-vault/general/overview">Azure Key Vault URL</a>, typically in the form: 
<pre>https://{vault-name}.vault.azure.net/</pre> 
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr>
<tr>
<td>secretName</td>
<td>The name of the secret containing the SEPS wallet file in Azure Key Vault.</td>
<td>Any valid secret name</td>
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
Optional parameter to specify the index of the connection string to use when retrieving credentials from the wallet
</td>
<td>A positive integer representing the index of the desired credential set (e.g., 1, 2, 3, etc.). </td>
<td>
<i>No default value. If not specified, the provider follows the default behavior as described above</i>
</td>
</tr>
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-key-vault-wallet.properties](example-key-vault-wallet.properties).

## Key Vault Connection String Provider

The Key Vault Connection String Provider provides Oracle JDBC with a connection string managed by the Azure Key Vault service.
This is a Resource Provider identified by the name `ojdbc-provider-azure-key-vault-tnsnames`.

This provider retrieves and decodes a `tnsnames.ora` file stored as a base64-encoded secret. in Azure Key Vault, allowing selection of
connection strings based on predefined consumer groups such as `HIGH`, `MEDIUM`, `LOW`, `TRANSACTION_PROCESSING`,
and `TRANSACTION_PROCESSING_URGENT`.For more details, refer to the official Oracle documentation on [Consumer Groups and Database Service Levels](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/manage-priorities.html#GUID-6CEFC4B7-6EF8-4237-A004-C88F570A480B).

This enables flexible configuration for secure database connections based on workload and concurrency requirements.

In addition to the set of [common parameters](#common-parameters-for-resource-providers), this provider also supports the parameters listed below.

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
  <tr> <td>vaultUrl</td> 
    <td>The URL of the Azure Key Vault containing the tnsnames.ora file.</td> 
    <td> The <a href="https://docs.microsoft.com/en-us/azure/key-vault/general/overview">Azure Key Vault URL</a>, typically in the form: <pre>https://{vault-name}.vault.azure.net/</pre> </td> 
    <td> <i>No default value. A value must be configured for this parameter.</i> </td> 
  </tr> 
  <tr>
    <td>secretName</td> 
    <td>The name of the secret containing the tnsnames.ora file in Azure Key Vault.</td> 
    <td>Any valid secret name</td> 
    <td> <i>No default value. A value must be configured for this parameter.</i> </td>
  </tr>
  <tr>
    <td>consumerGroup</td>
    <td>Specifies the consumer group to retrieve the appropriate connection string from the tnsnames.ora file.</td> 
    <td>HIGH, MEDIUM, LOW, TRANSACTION_PROCESSING, TRANSACTION_PROCESSING_URGENT</td>
    <td>MEDIUM</td> 
  </tr> 
</tbody>
</table>

An example of a [connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE) that configures this provider can be found in [example-key-vault.properties](example-key-vault.properties).

## Common Parameters for Resource Providers
Providers classified as Resource Providers in this module all support a
common set of parameters.
<table>
  <thead><tr>
    <th>Parameter Name</th>
    <th>Description</th>
    <th>Accepted Values</th>
    <th>Default Value</th>
  </tr></thead>
  <tbody>
    <tr>
      <td>authenticationMethod</td>
      <td>
      Configures a method of <a href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdk_authentication_methods.htm">
      authentication with Azure
      </a>
      </td>
      <td>
      Accepted values are defined in: <a href="#configuring-authentication-for-resource-providers">
      Configuring Authentication
      </a>
      <td>
      auto-detect
      </td>
    </tr>
    <tr>
      <td>tenantId</td>
      <td>
      Configures the ID of the application's Azure Active Directory tenant.
      </td>
      <td>
      An 
      <a href="https://learn.microsoft.com/en-us/azure/active-directory/fundamentals/active-directory-how-to-find-tenant">
      Active Directory tenant ID
      </a>
      <td><i>
        No default value. If <code>TENANT_ID</code> is configured as an 
        <a href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#environment-variables">
        Azure SDK environment variable
        </a>, it will be used.
      </i></td>
    </tr>
    <tr>
      <td>clientId</td>
      <td>
      Configures the ID of an Azure Active Directory application.
      </td>
      <td>
      An 
      <a href="https://learn.microsoft.com/en-us/azure/active-directory/develop/application-model">
      Active Directory application ID
      </a>
      <td><i>
        No default value. If <code>CLIENT_ID</code> is configured as an 
        <a href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#environment-variables">
        Azure SDK environment variable
        </a>, it will be used.
      </i></td>
    </tr>
    <tr>
      <td>clientCertificatePath</td>
      <td>
      Configures the file system path to a certificate of an Azure Active
      Directory application.
      </td>
      <td>
      An 
      <a href="https://learn.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal#option-1-recommended-create-and-upload-a-self-signed-certificate">
      Active Directory application certificate
      </a>. The file may be use PEM or PFX encoding.
      <td><i>
        No default value. If <code>CLIENT_CERTIFICATE_PATH</code> is configured as an 
        <a href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#service-principal-with-certificate">
        Azure SDK environment variable
        </a>, it will be used.
      </i></td>
    </tr>
    <tr>
      <td>clientCertificatePassword</td>
      <td>
      Configures the password for a PFX certificate of an Azure Active
      Directory application.
      </td>
      <td>
      An PFX certificate password.
      <td><i>
        No default value. If <code>CLIENT_CERTIFICATE_PASSWORD</code> is configured as an 
        <a href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#service-principal-with-certificate">
        Azure SDK environment variable
        </a>, it will be used.
      </i></td>
    </tr>
    <tr>
      <td>clientSecret</td>
      <td>
      Configures a secret of an Azure Active Directory application.
      </td>
      <td>
      An 
      <a href="https://learn.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal#option-2-create-a-new-application-secret">
      Active Directory application secret
      </a>.
      <td><i>
        No default value. If <code>CLIENT_SECRET</code> is configured as an 
        <a href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#service-principal-with-secret">
        Azure SDK environment variable
        </a>, it will be used.
      </i></td>
    </tr>
    <tr>
      <td>username</td>
      <td>
      Configures the username of an Azure account.
      </td>
      <td>
      A username, which is typically an email address.
      </td>
      <td><i>
        No default value. If <code>AZURE_USERNAME</code> is configured as an 
        <a href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#username-and-password">
        Azure SDK environment variable
        </a>, it will be used.
      </i></td>
    </tr>
    <tr>
      <td>password</td>
      <td>
      Configures the password of an Azure account.
      </td>
      <td>
      The password of an Azure account.
      </td>
      <td><i>
        No default value. If <code>AZURE_PASSWORD</code> is configured as an 
        <a href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#username-and-password">
        Azure SDK environment variable
        </a>, it will be used.
      </i></td>
    </tr>
    <tr>
      <td>redirectUrl</td>
      <td>
      <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/reply-url">
      Redirect URL
      </a>
      for <code>authentication-method=interactive</code>
      </td>
      <td>
      A URL of the form <code>http://localhost[:port-number]</code> is accepted.
      <td>
      <i>
        <code>http://localhost</code>
        (redirects to any available port in the ephemeral range)
      </i>
      </td>
    </tr>
  </tbody>
</table>

These parameters may be configured as a connection properties recognized by the
Oracle JDBC Driver. Parameter names are recognized when appended to the name of
a connection property that identifies a provider.
For example, when the connection property `oracle.jdbc.provider.password`
identifies a  provider, any of the parameter names listed above may be appended to it:
```properties
oracle.jdbc.provider.password=ojdbc-provider-azure-key-vault-password
oracle.jdbc.provider.password.authenticationMethod=service-principal
oracle.jdbc.provider.password.tenantId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
oracle.jdbc.provider.password.clientId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
oracle.jdbc.provider.password.clientCertificatePath=/users/app/certificate.pem
```
In the example above, the parameter names `authenticationMethod`, `tenantId`,
`clientId`, and `clientCertificatePath` are appended to the name of the
connection property `oracle.jdbc.provider.password`.
This has the effect of configuring the Key Vault Password Provider, which
is identified by that property.

These same parameter names can be appended to the name of any other property
that identifies a provider. For instance, a provider identified by
the connection property `oracle.jdbc.provider.accessToken` can be configured with
the same parameters seen in the previous example:
```properties
oracle.jdbc.provider.accessToken=ojdbc-provider-azure-token
oracle.jdbc.provider.accessToken.authenticationMethod=service-principal
oracle.jdbc.provider.accessToken.tenantId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
oracle.jdbc.provider.accessToken.clientId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
oracle.jdbc.provider.accessToken.clientCertificatePath=/users/app/certificate.pem
```
Connection properties which identify and configure a provider may appear in a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
or be configured programmatically. Configuration with JVM system properties is
not supported.

### Configuring Authentication for Resource Providers
Resource Providers in this module must authenticate with Azure. By default, a provider will
automatically detect any available credentials. A specific credential
may be configured using the "authenticationMethod" parameter. The parameter may
be set to any of the following values:
<dl>
<dt>service-principal</dt>
<dd>
Authenticate as an <a href="https://learn.microsoft.com/en-us/azure/active-directory/develop/app-objects-and-service-principals#service-principal-object">
application service principal
</a>. A tenant ID and client ID must be configured, along with a certificate or
a secret.
</dd>
<dt>managed-identity</dt>
<dd>
Authenticate as a <a href="https://learn.microsoft.com/en-us/azure/active-directory/managed-identities-azure-resources/overview">
managed identity
</a>. A client ID may be configured to authenticate as a user assigned managed 
identity.
</dd>
<dt>password</dt>
<dd>
Authenticate as an Azure user account. A client ID must be configured, along 
with a username and password.
</dd>
<dt>device-code</dt>
<dd>
Authenticate interactively by logging in to an Azure account in a web browser.
A browser link is output to the standard output stream.
</dd>
<dt>interactive</dt>
<dd>
Authenticate interactively by logging in to a cloud account with your
default web browser. The browser window is opened automatically.
</dd>
<dt>auto-detect</dt>
<dd>
This is the default authentication method. The provider will attempt the 
following authentication methods, in the order listed, until one succeeds:
"service-principal", "password", and "managed-identity".
</dd>
<dt></dt>
</dl>
