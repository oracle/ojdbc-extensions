# Oracle JDBC Providers for OCI

This module contains providers for integration between Oracle JDBC and
Oracle Cloud Infrastructure (OCI).

## Centralized Config Providers

<dl>
<dt><a href="#oci-database-tools-connections-config-provider">OCI Database 
Tools Connections Config Provider</a></dt>
<dd>Provides connection properties managed by the Database Tools Connection 
service</dd>
<dt><a href="#oci-object-storage-config-provider">OCI Object Storage Config 
Provider</a></dt>
<dd>Provides connection properties managed by the Object Storage service</dd>
</dl>
<dl>

## Resource Providers

<dt><a href="#database-connection-string-provider">Database Connection String Provider</a></dt>
<dd>Provides connection strings for an Autonomous Database</dd>
<dt><a href="#database-tls-provider">Database TLS Provider</a></dt>
<dd>Provides TLS keys and certificates for an Autonomous Database</dd>
<dt><a href="#vault-password-provider">Vault Password Provider</a></dt>
<dd>Provides passwords managed by the Vault service</dd>
<dt><a href="#vault-username-provider">Vault Username Provider</a></dt>
<dd>Provides usernames managed by the Vault service</dd>
<dt><a href="#access-token-provider">Access Token Provider</a></dt>
<dd>Provides access tokens issued by the Dataplane service</dd>
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
  <artifactId>ojdbc-provider-oci</artifactId>
  <version>1.0.0</version>
</dependency>
```

## OCI Database Tools Connections Config Provider

The OCI Database Tools Connections is a managed service that can be used to configure connections to a database. 
The created resource stores connection properties, including user, password and wallets (these last two optionally as references to a secret in OCI Vault).
Each configuration has an identifier (OCID) that is used to identify which connection is requested by the driver.

JDBC URL Sample that uses the OCI DBTools provider:

<pre>
jdbc:oracle:thin:@config-ocidbtools:ocid1.databasetoolsconnection.oc1.phx.ama ...
</pre>

## OCI Object Storage Config Provider
The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-ociobject:` to be able to identify that the configuration parameters should be loaded using OCI Object Storage. Users only need to indicate the URL Path of the Object containing the JSON payload, with the following syntax:

<pre>
jdbc:oracle:thin:@config-ociobject:{url_path}[?option1=value1&option2=value2...]
</pre>

The insturctions of obtaining a URL Path can be found in [Get the URI or Pre-Authenticated Request URL to Access the Object Store](https://docs.oracle.com/en/cloud/paas/autonomous-database/csgru/get-uri-access-object-store.html).

### JSON Payload format

There are 3 fixed values that are looked at the root level.

- connect_descriptor (required)
- user (optional)
- password (optional)

The rest are dependent on the driver, in our case `/jdbc`. The key-value pairs that are with sub-prefix `/jdbc` will be applied to a DataSource. The key values are constant keys which are equivalent to the properties defined in the [OracleConnection](https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleConnection.html) interface.

For example, let's suppose an url like:

<pre>
jdbc:oracle:thin:@config-ociobject:https://objectstorage.us-phoenix-1.oraclecloud.com/n/mytenancy/b/bucket1/o/payload_ojdbc_objectstorage.json
</pre>

And the JSON Payload for the file **payload_ojdbc_objectstorage.json** in the **bucket1** which namespace is **mytenancy** is as following:

```json
{
  "connect_descriptor": "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.us-phoenix-1.oraclecloud.com))(connect_data=(service_name=xsxsxs_dbtest_medium.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))",
  "user": "scott",
  "password": { 
    "type": "vault-oci",
    "value": "ocid1.vaultsecret.oc1.phx.amaaaaaxxxx"
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
    ds.setURL("jdbc:oracle:thin:@config-ociobject:https://objectstorage.us-phoenix-1.oraclecloud.com/n/mytenancy/b/bucket1/o/payload_ojdbc_objectstorage.json");
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("select sysdate from dual");
    if (rs.next())
      System.out.println("select sysdate from dual: " + rs.getString(1));
```

### Password JSON Object

For the JSON type of provider (OCI Object Storage, HTTP/HTTPS, File) the password is an object itself with the following spec:

- type
  - Mandatory
  - Possible values
    - vault-oci
    - vault-azure
    - base64
- value
  - Mandatory
  - Possible values
    - OCID of the secret (if vault-oci)
    - Azure Key Vault URI (if vault-azure)
    - Base64 Encoded password (if base64)
    - Text
- authentication
  - Optional (it will apply defaults in the same way as described in [Configuring Authentication](#configuring-authentication)).
  - Possible Values
    - method
    - optional parameters (depends on the cloud provider, applies the same logic as [Config Provider for Azure](../ojdbc-provider-azure/README.md#config-provider-for-azure)).

## Common Parameters for Centralized Config Providers
OCI Database Tools Connections Config Provider and OCI Object Storage Config Provider
share the same sets of parameters for authentication configuration.

### Configuring Authentication

The Centralized Config Providers in this module use the 
[OCI SDK Authentication Methods](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdk_authentication_methods.htm) to provide authorization and authentication to the Object Storage, Database Tools Connection and Vault services.
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
  <td rowspan="7"><b>OCI_DEFAULT</b> or &lt;Empty&gt;</td>
  <td rowspan="7">API Key-Based Authentication</td>
  <td rowspan="7">(~/.oci/config) or<br>
(~/.oraclebmc/config) or<br>
environment variable OCI_CONFIG_FILE <br>
(this is the same approach that oci-java-sdk has)
Either above or all the optional values
in Optional Parameters</td>
  <td>OCI_PROFILE</td></tr>
  <td>OCI_TENANCY</td></tr>
  <td>OCI_USER</td></tr>
  <td>OCI_FINGERPRINT</td></tr>
  <td>OCI_KEY_FILE</td></tr>
  <td>OCI_PASS_PHRASE</td></tr>
  <tr><td>OCI_REGION (*)</td>
</tr>
<tr>
  <td><b>OCI_INSTANCE_PRINCIPAL</b></td>
  <td>Instance Principal Authentication</td>
  <td>&nbsp;</td>
  <td>&nbsp;</td>
</tr>
<tr>
  <td><b>OCI_RESOURCE_PRINCIPAL</b></td>
  <td>Resource Principal Authentication</td>
  <td>&nbsp;</td>
  <td>&nbsp;</td>
</tr>
<tr>
  <td><b>OCI_INTERACTIVE</b></td>
  <td>Session Token-Based Authentication</td>
  <td>Same as OCI_DEFAULT</td>
  <td>Same as OCI_DEFAULT</td>
</tr>
</tbody>
</table>

<i>*Note: this parameter is introduced to align with entries of the config file. The region that is used for calling Object Storage, Database Tools Connection, and Secret services will be extracted from the Object Storage URL, Database Tools Connection OCID or Secret OCID</i>

## Database Connection String Provider
The Database Connection String Provider provides Oracle JDBC with the connection string of an
Autonomous Database. This is a Resource Provider identified by the name
`ojdbc-provider-oci-database-connection-string`.

For databases that require mutual TLS (mTLS) authentication, it is recommended 
to use the <a href="#database-tls-provider">
Database TLS Provider</a>
in conjunction with this provider.

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
<td>ocid</td>
<td>Identifies the database that a connection string is provided for.</td>
<td>
The <a href="https://docs.oracle.com/en-us/iaas/Content/General/Concepts/identifiers.htm">
OCID
</a>
of an Autonomous Database
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td><tr>
<td>consumerGroup</td>
<td>
Configures a share of CPU and IO resources allocated for the database session.
Predefined groups are specified in the <a href="https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/manage-cpu-shares.html#GUID-8FEE49FF-CDEE-4433-B812-0AAABA8DEC7F">
ADB product documentation
</a>
</td>
<td>
"HIGH", "MEDIUM", "LOW", "TP", or "TPURGENT".
<br>
<i>A database provisioned with the Data Warehouse workload type will not recognize
TP or TPURGENT</i>
</td>
<td>
MEDIUM
</td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-adb.properties](example-adb.properties).


## Database TLS Provider
The Database TLS Provider provides Oracle JDBC with keys and certificates for 
[mutual TLS authentication](https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/connect-introduction.html#GUID-9A472E49-3B2B-4D9F-9DC2-D3E6E4454285)
(mTLS)
with an Autonomous Database. This is a Resource Provider identified by the name
`ojdbc-provider-oci-database-tls`.

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
<td>ocid</td>
<td>Identifies the database that TLS keys and certificates are provided for.</td>
<td>
The <a href="https://docs.oracle.com/en-us/iaas/Content/General/Concepts/identifiers.htm">
OCID
</a>
of an Autonomous Database 
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-adb.properties](example-adb.properties).

## Vault Password Provider
The Vault Password Provider provides Oracle JDBC with a password that is managed
by the OCI Vault service. This is a Resource Provider identified by the
name `ojdbc-provider-oci-vault-password`.

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
<td>ocid</td>
<td>Identifies the secret that is provided.</td>
<td>
The <a href="https://docs.oracle.com/en-us/iaas/Content/General/Concepts/identifiers.htm">
OCID
</a>
of an OCI Vault secret
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-vault.properties).

## Vault Username Provider
The Vault Username Provider provides Oracle JDBC with a username that is managed by the
OCI Vault service. This is a Resource Provider identified by the name
`ojdbc-provider-oci-vault-username`.

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
<td>ocid</td>
<td>Identifies the secret that is provided.</td>
<td>
The <a href="https://docs.oracle.com/en-us/iaas/Content/General/Concepts/identifiers.htm">
OCID
</a>
of an OCI Vault secret
</td>
<td>
<i>No default value. A value must be configured for this parameter.</i>
</td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-vault.properties](example-vault.properties).

## Access Token Provider
The Access Token Provider provides Oracle JDBC with an access token that authorizes logins to an Autonomous Database. This is a Resource Provider identified by 
the name `ojdbc-provider-oci-token`.

This provider must be configured to <a href="#configuring-authentication">authenticate</a> as
an IAM user that has been mapped to a database user. The IAM user must also be included in a policy that grants access to the Autonomous Database. Instructions
can be found in the <a href="https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/manage-users-iam.html#GUID-4E206209-4E3B-4387-9364-BDCFB4E16E2E">
ADB product documentation.
</a>

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
<td>scope</td>
<td>
Specifies the scope of databases that may be accessed with the token. See
<a href="#configuring-a-scope">Configuring a Scope</a> for details.
<td>
<i>A URN in any of the following forms is accepted:</i><pre>
urn:oracle:db::id::<i>compartment-ocid</i>::<i>database-ocid</i>
urn:oracle:db::id::<i>compartment-ocid</i>
urn:oracle:db::id::*
</pre>
</td>
<td><pre>
urn:oracle:db::id::*
</pre></td>
</tr></tbody>
</table>

An example of a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
that configures this provider can be found in
[example-token.properties](example-token.properties).

### Configuring a Scope

The "scope" parameter may be configured as a URN that identifies a database
and/or compartment.

The least privileged scope is one that authorizes logins to a single
database. This scope is expressed
as a URN in the form shown below, where <i>database-ocid</i> is
the OCID of a database, and <i>compartment-ocid</i> is the OCID of the
compartment that contains the database.
<pre>
urn:oracle:db::id::<i>comparment-ocid</i>::<i>database-ocid</i>
</pre>
A more privileged scope is one that authorizes logins to all databases within a
compartment. This scope is expressed as a URN in the form shown below, where 
<i>compartment-ocid</i> is the OCID of the compartment that contains the
databases.
<pre>
urn:oracle:db::id::<i>comparment-ocid</i>
</pre>
The most privileged scope is one that authorizes logins to all databases within
a tenancy. This scope is expressed as the URN shown below, which contains the 
`*` symbol.
<pre>
urn:oracle:db::id::*
</pre>

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
      authentication with OCI
      </a>
      </td>
      <td>
      Accepted values are defined in: <a href="#configuring-authentication">
      Configuring Authentication
      </a>
      <td>
      auto-detect
      </td>
    </tr>
    <tr>
      <td>configFile</td>
      <td>
      Configures the path to an <a href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm">
      OCI configuration file
      </a>
      </td>
      <td>
      A file system path
      <td>
      ~/.oci/config
      </td>
    </tr>
    <tr>
      <td>profile</td>
      <td>
      Configures the name of a profile in an OCI configuration file
      </td>
      <td>
      A profile name
      <td>
      DEFAULT
      </td>
    </tr>
  </tbody>
</table>

These parameters may be configured as a connection properties recognized by the
Oracle JDBC Driver. Parameter names are recognized when appended to the name of
a connection property that identifies a provider.
For example, when the connection property `oracle.jdbc.provider.database`
identifies a  provider, any of the parameter names listed above may be appended to it:
```properties
oracle.jdbc.provider.database=ojdbc-provider-oci-database-connection-string
oracle.jdbc.provider.database.authenticationMethod=config-file
oracle.jdbc.provider.database.configFile=/home/app/resources/oci-config
oracle.jdbc.provider.database.profile=APP_PROFILE
```
In the example above, the parameter names `authenticationMethod`, `configFile`,
and `profile` are appended to the name of the connection property
`oracle.jdbc.provider.database`.
This has the effect of configuring the Database Connection String Provider, which
is identified by that property.

These same parameter names can be appended to the name of any other property
that identifies a provider. For instance, a provider identified by
the connection property `oracle.jdbc.provider.accessToken` can be configured with
the same parameters seen in the previous example:
```properties
oracle.jdbc.provider.accessToken=ojdbc-provider-oci-token
oracle.jdbc.provider.accessToken.authenticationMethod=config-file
oracle.jdbc.provider.accessToken.configFile=/home/app/resources/oci-config
oracle.jdbc.provider.accessToken.profile=APP_PROFILE
```
Connection properties which identify and configure a provider may appear in a
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE)
or be configured programmatically. Configuration with JVM system properties is
not supported.

### Configuring Authentication

Providers in this module must authenticate with OCI. By default, a provider will
automatically detect any available credentials.  A specific credential
may be configured using the "authenticationMethod" parameter. The parameter may
be set to any of the following values:
<dl>
<dt>config-file</dt>
<dd>
Authenticate with credentials configured by an <a href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm">
OCI configuration file
</a>.
</dd>
<dt>instance-principal</dt>
<dd>
Authenticate as an <a href="https://docs.oracle.com/en-us/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm">
instance principal
</a>.
</dd>
<dt>resource-principal</dt>
<dd>
Authenticate as a <a href="https://docs.oracle.com/en-us/iaas/Content/Functions/Tasks/functionsaccessingociresources.htm">
resource principal
</a>.
</dd>
<dt>cloud-shell</dt>
<dd>
Authenticate with the session token of a <a href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/cloudshellintro.htm">
Cloud Shell
</a>.
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
"config-file", "cloud-shell", "resource-principal", and "instance-principal".
</dd>
<dt></dt>
</dl>
