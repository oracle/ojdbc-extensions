# Oracle JDBC Providers for GCP

This module contains providers for integration between Oracle JDBC and
Google Cloud Platform (GCP).

## Centralized Config Providers

<dl>
<dt><a href="#gcp-object-storage-config-provider">GCP Object Storage Config 
Provider</a></dt>
<dd>Provides connection properties managed by the Object Storage service</dd>
<dt><a href="#gcp-vault-secret-config-provider">GCP Vault Secret Config Provider</a></dt>
<dd>Provides connection properties managed by the Secret Manager service</dd>
<dt><a href="#caching-configuration">Caching configuration</a></dt>
<dd>Caching mechanism adopted by Centralized Config Providers</dd>
</dl>

## Resource Providers

<dl>
<dt><a href="#vault-password-provider">Vault Password Provider</a></dt>
<dd>Provides passwords managed by the Vault service</dd>
<dt><a href="#vault-username-provider">Vault Username Provider</a></dt>
<dd>Provides usernames managed by the Vault service</dd>
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

ADC searches for credentials in the following locations:

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

## GCP Object Storage Config Provider
The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-gcp-object:` to be able to identify that the configuration parameters should be loaded using GCP Object Storage. Users only need to indicate the project, bucket and object containing the JSON payload, with the following syntax:

<pre>
jdbc:oracle:thin:@config-gcp-object://project={project};bucket={bucket};object={object}]
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
    "type": "gcpsecret",
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
    ds.setURL("jdbc:oracle:thin:@config-gcpobject://project=myproject;bucket=mybucket;object=payload_ojdbc_objectstorage.json");
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
    - gcpsecret
- value
  - Mandatory
  - Possible values
    - OCID of the secret (if ocivault)
    - Azure Key Vault URI (if azurevault)
    - Base64 Encoded password (if base64)
    - GCP resource name (if gcpsecret)
    - Text
- authentication
  - Optional
  - Possible Values
    - method
    - optional parameters (depends on the cloud provider).

## GCP Vault Secret Config Provider
Apart from GCP Object Storage, users can also store JSON Payload in the content of GCP Secret Manager secret. Users need to indicate the resource name:

<pre>
jdbc:oracle:thin:@config-gcpvault:{resource-name}
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


